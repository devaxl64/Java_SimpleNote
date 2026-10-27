import java.sql.SQLException;
import java.util.List;

// Compatibilidade com a estrutura inicial. A persistência agora passa pelo cache local.
public class NoteRepository {
    public List<Note> listar() throws SQLException { return CacheStore.listar(); }
    public int criar(String conteudo) throws SQLException {
        return CacheStore.salvar(0, conteudo, null, CacheStore.corPadrao());
    }
    public void atualizar(int id, String conteudo) throws SQLException {
        Note anterior = CacheStore.listar().stream().filter(n -> n.getId()==id).findFirst().orElse(null);
        if(anterior!=null)CacheStore.salvar(id,conteudo,anterior.getTituloPersonalizado(),anterior.getCor());
    }
    public void excluir(int id) throws SQLException { CacheStore.salvar(id,"",null,1); }
}
