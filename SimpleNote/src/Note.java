import java.time.LocalDateTime;

public class Note {
    private int id; // ID local do cache
    private Integer remoteId; // ID da nota no MariaDB; null enquanto não sincronizada
    private String conteudo;
    private String tituloPersonalizado;
    private int cor;
    private LocalDateTime alteradoEm;
    private long revisao;
    private boolean pendente;
    private boolean excluida;

    public Note(int id, Integer remoteId, String conteudo, String tituloPersonalizado,
                int cor, LocalDateTime alteradoEm, long revisao, boolean pendente, boolean excluida) {
        this.id = id;
        this.remoteId = remoteId;
        this.conteudo = conteudo;
        this.tituloPersonalizado = tituloPersonalizado;
        this.cor = cor;
        this.alteradoEm = alteradoEm;
        this.revisao = revisao;
        this.pendente = pendente;
        this.excluida = excluida;
    }

    public int getId() { return id; }
    public Integer getRemoteId() { return remoteId; }
    public String getConteudo() { return conteudo; }
    public String getTituloPersonalizado() { return tituloPersonalizado; }
    public int getCor() { return cor; }
    public LocalDateTime getAlteradoEm() { return alteradoEm; }
    public long getRevisao() { return revisao; }
    public boolean isPendente() { return pendente; }
    public boolean isExcluida() { return excluida; }

    public static String gerarResumo(String texto) {
        if (texto == null) return "";
        String linha = texto.stripLeading().lines().findFirst().orElse("").strip();
        if (linha.isBlank()) linha = texto.strip().replaceAll("\\s+", " ");
        return linha.length() > 40 ? linha.substring(0, 40) : linha;
    }

    public String getResumo() { return gerarResumo(conteudo); }
    public String getTituloExibido() {
        if (tituloPersonalizado != null && !tituloPersonalizado.isBlank()) {
            return tituloPersonalizado.length() > 40 ? tituloPersonalizado.substring(0,40) : tituloPersonalizado;
        }
        String resumo = getResumo();
        return resumo.isBlank() ? "SimpleNote" : resumo;
    }
}
