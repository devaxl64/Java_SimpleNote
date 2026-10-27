import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CacheStore {
    private static final String URL = criarUrl();

    private static String criarUrl() {
        String so = System.getProperty("os.name", "").toLowerCase();
        String base = so.contains("win") ? System.getenv("APPDATA") : null;
        if (base == null || base.isBlank()) base = Path.of(System.getProperty("user.home"), ".local", "share").toString();
        try {
            Path pasta = Path.of(base, "simplenote");
            Files.createDirectories(pasta);
            return "jdbc:sqlite:" + pasta.resolve("cache.db");
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível preparar o armazenamento local.", e);
        }
    }

    private static Connection conectar() throws SQLException { return DriverManager.getConnection(URL); }

    public static synchronized void iniciar() throws SQLException {
        try (Connection db = conectar(); Statement sql = db.createStatement()) {
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS local_notes (id INTEGER PRIMARY KEY AUTOINCREMENT, remote_id INTEGER UNIQUE, content TEXT NOT NULL, custom_title TEXT, color_id INTEGER NOT NULL DEFAULT 1, modified INTEGER NOT NULL, dirty INTEGER NOT NULL DEFAULT 1, deleted INTEGER NOT NULL DEFAULT 0)");
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS local_colors (id INTEGER PRIMARY KEY, name TEXT NOT NULL DEFAULT '', dirty INTEGER NOT NULL DEFAULT 0)");
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS local_settings (key TEXT PRIMARY KEY, value TEXT NOT NULL, dirty INTEGER NOT NULL DEFAULT 0)");
            for (int i = 1; i <= 6; i++) {
                sql.executeUpdate("INSERT OR IGNORE INTO local_colors(id,name,dirty) VALUES(" + i + ", '', 0)");
            }
            sql.executeUpdate("INSERT OR IGNORE INTO local_settings(key,value,dirty) VALUES('default_color','1',0)");
            sql.executeUpdate("INSERT OR IGNORE INTO local_settings(key,value,dirty) VALUES('layout_normal','list',0)");
            sql.executeUpdate("INSERT OR IGNORE INTO local_settings(key,value,dirty) VALUES('layout_maximized','grid',0)");
            sql.executeUpdate("INSERT OR IGNORE INTO local_settings(key,value,dirty) VALUES('theme','escuro',0)");
            sql.executeUpdate("INSERT OR IGNORE INTO local_settings(key,value,dirty) VALUES('sort','recentes',0)");
        }
    }

    private static Note ler(ResultSet r) throws SQLException {
        Object remoto = r.getObject("remote_id");
        Integer remoteId = remoto == null ? null : ((Number) remoto).intValue();
        long tempo = r.getLong("modified");
        LocalDateTime data = LocalDateTime.ofInstant(Instant.ofEpochMilli(tempo), ZoneId.systemDefault());
        return new Note(r.getInt("id"), remoteId, r.getString("content"), r.getString("custom_title"),
            r.getInt("color_id"), data, tempo, r.getInt("dirty") == 1, r.getInt("deleted") == 1);
    }

    public static synchronized List<Note> listar() throws SQLException {
        List<Note> notas = new ArrayList<>();
        try (Connection db = conectar(); PreparedStatement st = db.prepareStatement("SELECT * FROM local_notes WHERE deleted=0 ORDER BY modified DESC,id DESC"); ResultSet r = st.executeQuery()) {
            while (r.next()) notas.add(ler(r));
        }
        return notas;
    }

    public static synchronized List<Note> pendentes() throws SQLException {
        List<Note> notas = new ArrayList<>();
        try (Connection db = conectar(); PreparedStatement st = db.prepareStatement("SELECT * FROM local_notes WHERE dirty=1 ORDER BY id"); ResultSet r = st.executeQuery()) {
            while (r.next()) notas.add(ler(r));
        }
        return notas;
    }

    // Nota vazia não é criada; nota existente esvaziada é removida.
    public static synchronized int salvar(int id, String texto, String titulo, int cor) throws SQLException {
        String conteudo = texto == null ? "" : texto;
        long momento = System.currentTimeMillis();
        try (Connection db = conectar()) {
            if (conteudo.isBlank()) {
                if (id > 0) {
                    try (PreparedStatement st = db.prepareStatement("UPDATE local_notes SET deleted=1,dirty=1,modified=? WHERE id=?")) {
                        st.setLong(1, momento); st.setInt(2,id); st.executeUpdate();
                    }
                }
                return 0;
            }
            if (id > 0) {
                try (PreparedStatement st = db.prepareStatement("UPDATE local_notes SET content=?,custom_title=?,color_id=?,dirty=1,deleted=0,modified=? WHERE id=?")) {
                    st.setString(1,conteudo); st.setString(2,titulo); st.setInt(3,cor); st.setLong(4,momento); st.setInt(5,id); st.executeUpdate();
                }
                return id;
            }
            try (PreparedStatement st = db.prepareStatement("INSERT INTO local_notes(content,custom_title,color_id,modified,dirty,deleted) VALUES(?,?,?,?,1,0)", Statement.RETURN_GENERATED_KEYS)) {
                st.setString(1,conteudo); st.setString(2,titulo); st.setInt(3,cor); st.setLong(4,momento); st.executeUpdate();
                try (ResultSet r = st.getGeneratedKeys()) { if (r.next()) return r.getInt(1); }
            }
        }
        throw new SQLException("Não foi possível salvar a nota no cache.");
    }

    public static synchronized void confirmarSincronizacao(int localId, long revisao, Integer remoteId, boolean removida) throws SQLException {
        try (Connection db = conectar()) {
            if (removida) {
                try (PreparedStatement st = db.prepareStatement("DELETE FROM local_notes WHERE id=? AND modified=?")) {
                    st.setInt(1,localId); st.setLong(2,revisao); st.executeUpdate();
                }
            } else {
                // Se a nota mudou enquanto sincronizava, conservamos dirty=1 para reenviá-la.
                try (PreparedStatement st = db.prepareStatement("UPDATE local_notes SET remote_id=?,dirty=CASE WHEN modified=? THEN 0 ELSE 1 END WHERE id=?")) {
                    st.setObject(1,remoteId); st.setLong(2,revisao); st.setInt(3,localId); st.executeUpdate();
                }
            }
        }
    }

    public static synchronized void receberNota(int remoteId, String conteudo, String titulo, int cor, long modificado) throws SQLException {
        try (Connection db = conectar()) {
            try (PreparedStatement st = db.prepareStatement("INSERT INTO local_notes(remote_id,content,custom_title,color_id,modified,dirty,deleted) VALUES(?,?,?,?,?,0,0) ON CONFLICT(remote_id) DO UPDATE SET content=excluded.content,custom_title=excluded.custom_title,color_id=excluded.color_id,modified=excluded.modified WHERE dirty=0")) {
                st.setInt(1,remoteId); st.setString(2,conteudo); st.setString(3,titulo); st.setInt(4,cor); st.setLong(5,modificado); st.executeUpdate();
            }
        }
    }

    // Se o servidor perder uma nota, preservamos a cópia local e a reenviamos.
    public static synchronized void limparRemotosAusentes(List<Integer> presentes) throws SQLException {
        try (Connection db = conectar(); PreparedStatement st = db.prepareStatement("SELECT id,remote_id FROM local_notes WHERE dirty=0 AND remote_id IS NOT NULL" ); ResultSet r = st.executeQuery()) {
            List<Integer> ausentes = new ArrayList<>();
            while (r.next()) if (!presentes.contains(r.getInt("remote_id"))) ausentes.add(r.getInt("id"));
            try (PreparedStatement marcar = db.prepareStatement("UPDATE local_notes SET remote_id=NULL,dirty=1 WHERE id=? AND dirty=0")) {
                for (Integer id : ausentes) {marcar.setInt(1,id); marcar.executeUpdate();}
            }
        }
    }

    public static synchronized String nomeCor(int id) {
        try (Connection db=conectar(); PreparedStatement st=db.prepareStatement("SELECT name FROM local_colors WHERE id=?")) {
            st.setInt(1,id);
            try(ResultSet r=st.executeQuery()){if(r.next()) return r.getString(1);}
        } catch(SQLException ignored) { }
        return "";
    }

    public static synchronized void renomearCor(int id, String nome) throws SQLException {
        String valor = nome == null ? "" : nome.strip();
        if(valor.length()>20) valor=valor.substring(0,20);
        try(Connection db=conectar(); PreparedStatement st=db.prepareStatement("UPDATE local_colors SET name=?,dirty=1 WHERE id=?")) {
            st.setString(1,valor);st.setInt(2,id);st.executeUpdate();
        }
    }

    public static synchronized int corPadrao() {
        try (Connection db=conectar(); PreparedStatement st=db.prepareStatement("SELECT value FROM local_settings WHERE key='default_color'"); ResultSet r=st.executeQuery()) {
            if(r.next())return Math.max(1,Math.min(6,Integer.parseInt(r.getString(1))));
        } catch(Exception ignored) { }
        return 1;
    }

    public static synchronized void definirCorPadrao(int id) throws SQLException {
        try(Connection db=conectar(); PreparedStatement st=db.prepareStatement("UPDATE local_settings SET value=?,dirty=1 WHERE key='default_color'")) {
            st.setString(1,Integer.toString(id));st.executeUpdate();
        }
    }

    public static synchronized List<Integer> coresPendentes() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement("SELECT id FROM local_colors WHERE dirty=1");ResultSet r=st.executeQuery()) {while(r.next())ids.add(r.getInt(1));}
        return ids;
    }
    public static synchronized void confirmarCor(int id, String nome) throws SQLException {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement("UPDATE local_colors SET dirty=0 WHERE id=? AND name=?")) {st.setInt(1,id);st.setString(2,nome);st.executeUpdate();}
    }
    public static synchronized void receberCor(int id,String nome) throws SQLException {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement("UPDATE local_colors SET name=? WHERE id=? AND dirty=0")) {st.setString(1,nome);st.setInt(2,id);st.executeUpdate();}
    }
    public static synchronized boolean corPadraoPendente() throws SQLException {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement("SELECT dirty FROM local_settings WHERE key='default_color'");ResultSet r=st.executeQuery()) {return r.next()&&r.getInt(1)==1;}
    }
    public static synchronized void confirmarCorPadrao(int id) throws SQLException {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement("UPDATE local_settings SET dirty=0 WHERE key='default_color' AND value=?")) {st.setString(1,Integer.toString(id));st.executeUpdate();}
    }
    public static synchronized void receberCorPadrao(int id) throws SQLException {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement("UPDATE local_settings SET value=? WHERE key='default_color' AND dirty=0")) {st.setString(1,Integer.toString(id));st.executeUpdate();}
    }
    // Preferências de visualização: janela normal e maximizada são independentes.
    public static synchronized boolean modoGrade(boolean maximizada) {
        String chave = maximizada ? "layout_maximized" : "layout_normal";
        try (Connection db = conectar(); PreparedStatement st = db.prepareStatement(
                "SELECT value FROM local_settings WHERE key=?")) {
            st.setString(1, chave);
            try (ResultSet r = st.executeQuery()) {
                if (r.next()) return "grid".equals(r.getString(1));
            }
        } catch (SQLException erro) { erro.printStackTrace(); }
        return maximizada;
    }

    public static synchronized void definirModoGrade(boolean maximizada, boolean grade) throws SQLException {
        String chave = maximizada ? "layout_maximized" : "layout_normal";
        try (Connection db = conectar(); PreparedStatement st = db.prepareStatement(
                "INSERT INTO local_settings(key,value,dirty) VALUES(?,?,0) " +
                "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            st.setString(1, chave);
            st.setString(2, grade ? "grid" : "list");
            st.executeUpdate();
        }
    }
    public static synchronized String configuracao(String chave,String padrao) {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement(
                "SELECT value FROM local_settings WHERE key=?")) {
            st.setString(1,chave);
            try(ResultSet r=st.executeQuery()) {if(r.next())return r.getString(1);}
        } catch(SQLException erro) {erro.printStackTrace();}
        return padrao;
    }

    public static synchronized void definirConfiguracao(String chave,String valor) throws SQLException {
        try(Connection db=conectar();PreparedStatement st=db.prepareStatement(
                "INSERT INTO local_settings(key,value,dirty) VALUES(?,?,0) " +
                "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            st.setString(1,chave);st.setString(2,valor);st.executeUpdate();
        }
    }

}
