import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;

public class SyncService {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "simplenote-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean sincronizando = new AtomicBoolean(false);
    private static ScheduledFuture<?> edicaoPendente;
    private static boolean iniciado;
    private static Runnable aoAtualizar;

    public static synchronized void iniciar(Runnable atualizar) {
        aoAtualizar = atualizar;
        if (!iniciado) {
            iniciado = true;
            executor.scheduleWithFixedDelay(() -> sincronizar(null), 1, 90, TimeUnit.SECONDS);
        }
    }

    public static synchronized void agendar() {
        if (edicaoPendente != null) edicaoPendente.cancel(false);
        edicaoPendente = executor.schedule(() -> sincronizar(null), 12, TimeUnit.SECONDS);
    }

    public static void manual(java.util.function.Consumer<String> resultado) {
        executor.execute(() -> sincronizar(resultado));
    }

    private static void sincronizar(java.util.function.Consumer<String> resultado) {
        if (!sincronizando.compareAndSet(false, true)) {
            if(resultado != null) Platform.runLater(() -> resultado.accept("Sincronização em andamento."));
            return;
        }
        try {
            executarSync();
            if (aoAtualizar != null) Platform.runLater(aoAtualizar);
            if (resultado != null) Platform.runLater(() -> resultado.accept("Sincronizado."));
        } catch (Exception erro) {
            // Trabalho offline continua salvo em SQLite e aguarda próxima tentativa.
            if (resultado != null) Platform.runLater(() -> resultado.accept("Sem conexão com o MariaDB."));
            System.err.println("Sincronização pendente: " + erro.getMessage());
        } finally {
            sincronizando.set(false);
        }
    }

    private static void executarSync() throws SQLException {
        List<Note> pendentes = CacheStore.pendentes();
        List<NotaConfirmada> confirmacoes = new ArrayList<>();
        List<NotaRemota> recebidas = new ArrayList<>();
        List<Integer> remotasExistentes = new ArrayList<>();
        List<CorConfirmada> coresConfirmadas = new ArrayList<>();
        String[] nomesRemotos = new String[7];
        boolean padraoPendente = CacheStore.corPadraoPendente();
        int padrao = CacheStore.corPadrao();
        int padraoServidor = padrao;

        try (Connection conexao = Db.conectar()) {
            conexao.setAutoCommit(false);
            try {
                for (Note nota : pendentes) {
                    Integer idServidor = nota.getRemoteId();
                    if (nota.isExcluida()) {
                        if (idServidor != null) {
                            try(PreparedStatement sql=conexao.prepareStatement("DELETE FROM notes WHERE id=?")) {
                                sql.setInt(1,idServidor);sql.executeUpdate();
                            }
                        }
                    } else if (idServidor != null) {
                        try(PreparedStatement sql=conexao.prepareStatement("UPDATE notes SET title=?,content=?,custom_title=?,color_id=? WHERE id=?")) {
                            preparar(sql,nota);sql.setInt(5,idServidor);
                            if(sql.executeUpdate()==0) idServidor = inserir(conexao,nota);
                        }
                    } else {
                        idServidor = inserir(conexao,nota);
                    }
                    confirmacoes.add(new NotaConfirmada(nota.getId(),nota.getRevisao(),idServidor,nota.isExcluida()));
                }

                for (int id : CacheStore.coresPendentes()) {
                    String nome = CacheStore.nomeCor(id);
                    try(PreparedStatement sql=conexao.prepareStatement("INSERT INTO note_colors(id,name) VALUES(?,?) ON DUPLICATE KEY UPDATE name=VALUES(name)")) {
                        sql.setInt(1,id);sql.setString(2,nome);sql.executeUpdate();
                    }
                    coresConfirmadas.add(new CorConfirmada(id,nome));
                }
                if (padraoPendente) {
                    try(PreparedStatement sql=conexao.prepareStatement("INSERT INTO app_settings(config_key,config_value) VALUES('default_color',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")) {
                        sql.setString(1,Integer.toString(padrao));sql.executeUpdate();
                    }
                }

                try (PreparedStatement sql=conexao.prepareStatement("SELECT id,content,custom_title,color_id,updated_at FROM notes ORDER BY id"); ResultSet r=sql.executeQuery()) {
                    while(r.next()) {
                        Timestamp instante=r.getTimestamp("updated_at");
                        long tempo=instante == null ? System.currentTimeMillis() : instante.toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        int id=r.getInt("id");
                        remotasExistentes.add(id);
                        recebidas.add(new NotaRemota(id,r.getString("content"),r.getString("custom_title"),r.getInt("color_id"),tempo));
                    }
                }
                try(Statement sql=conexao.createStatement();ResultSet r=sql.executeQuery("SELECT id,name FROM note_colors")) {
                    while(r.next()) {int id=r.getInt(1);if(id>=1&&id<=6)nomesRemotos[id]=r.getString(2);}
                }
                try(Statement sql=conexao.createStatement();ResultSet r=sql.executeQuery("SELECT config_value FROM app_settings WHERE config_key='default_color'")) {
                    if(r.next())padraoServidor=Integer.parseInt(r.getString(1));
                }
                conexao.commit();
            } catch (Exception erro) {
                conexao.rollback();
                throw erro instanceof SQLException ? (SQLException) erro : new SQLException("Erro ao sincronizar",erro);
            }
        }

        for (NotaConfirmada n : confirmacoes) CacheStore.confirmarSincronizacao(n.localId,n.revisao,n.remoto,n.apagada);
        for (NotaRemota n : recebidas) CacheStore.receberNota(n.id,n.conteudo,n.titulo,n.cor,n.modificado);
        CacheStore.limparRemotosAusentes(remotasExistentes);
        for (CorConfirmada c : coresConfirmadas) CacheStore.confirmarCor(c.id,c.nome);
        if (padraoPendente) CacheStore.confirmarCorPadrao(padrao);
        for(int i=1;i<=6;i++) if(nomesRemotos[i]!=null)CacheStore.receberCor(i,nomesRemotos[i]);
        CacheStore.receberCorPadrao(padraoServidor);
    }

    private static int inserir(Connection db, Note nota) throws SQLException {
        try(PreparedStatement st=db.prepareStatement("INSERT INTO notes(title,content,custom_title,color_id) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            preparar(st,nota); st.executeUpdate();
            try(ResultSet r=st.getGeneratedKeys()) {if(r.next())return r.getInt(1);}
        }
        throw new SQLException("Não foi possível obter o ID da nota no MariaDB.");
    }
    private static void preparar(PreparedStatement st,Note n) throws SQLException {
        st.setString(1,Note.gerarResumo(n.getConteudo()));
        st.setString(2,n.getConteudo());
        st.setString(3,n.getTituloPersonalizado());
        st.setInt(4,n.getCor());
    }

    public static void parar() {executor.shutdownNow();}

    private record NotaConfirmada(int localId,long revisao,Integer remoto,boolean apagada) {}
    private record NotaRemota(int id,String conteudo,String titulo,int cor,long modificado) {}
    private record CorConfirmada(int id,String nome) {}
}
