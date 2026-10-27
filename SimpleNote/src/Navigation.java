import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navigation {
    private static String avisoPendente;

    public static void avisarNaLista(String aviso) { avisoPendente = aviso; }
    public static String consumirAviso() {
        String resultado = avisoPendente;
        avisoPendente = null;
        return resultado;
    }

    public static void abrirLista(Node origem) throws IOException {
        FXMLLoader arquivo = new FXMLLoader(Navigation.class.getResource("notes.fxml"));
        Parent tela = arquivo.load();
        trocarTela(origem, tela);
    }

    public static void abrirEditor(Node origem, Note nota) throws IOException {
        FXMLLoader arquivo = new FXMLLoader(Navigation.class.getResource("editor.fxml"));
        Parent tela = arquivo.load();
        NoteEditorController controle = arquivo.getController();
        controle.configurar(nota);
        trocarTela(origem, tela);
        controle.ativarFechamento();
    }

    private static void trocarTela(Node origem, Parent tela) {
        Stage janela = (Stage) origem.getScene().getWindow();
        double largura = janela.getScene().getWidth();
        double altura = janela.getScene().getHeight();
        janela.setOnCloseRequest(null);
        janela.setScene(new Scene(tela, largura, altura));
        janela.setTitle("SimpleNote");
    }
}
