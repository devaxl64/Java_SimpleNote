import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private static App instancia;

    public static void main(String[] args) { launch(args); }

    public static void abrirLink(String url) {
        if (instancia != null) instancia.getHostServices().showDocument(url);
    }

    @Override
    public void start(Stage janela) throws Exception {
        instancia = this;
        CacheStore.iniciar();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("layout.fxml"));
        Scene tela = new Scene(loader.load(), 390, 490);
        janela.setTitle("SimpleNote");
        janela.setMinWidth(340);
        janela.setMinHeight(440);
        janela.setScene(tela);
        janela.show();
    }

    @Override
    public void stop() {
        SyncService.parar();
    }
}
