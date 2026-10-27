import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SupportView {
    // Mesmos payloads Pix da versão anterior. Nunca enviar credenciais do banco aqui.
    private static final String PIX2="00020126580014BR.GOV.BCB.PIX01364bb923ab-2b18-4757-b8c5-866c999eee2852040000530398654042.005802BR5925Marcell Axel Elidio de Ca6009SAO PAULO62140510PSX3LCH8Qj6304B14B";
    private static final String PIX5="00020126580014BR.GOV.BCB.PIX01364bb923ab-2b18-4757-b8c5-866c999eee2852040000530398654045.005802BR5925Marcell Axel Elidio de Ca6009SAO PAULO62140510X8HatTAHXs6304A423";
    private static final String PIX10="00020126580014BR.GOV.BCB.PIX01364bb923ab-2b18-4757-b8c5-866c999eee28520400005303986540510.005802BR5925Marcell Axel Elidio de Ca6009SAO PAULO62140510vAcZgM4CGO630485FC";
    private static final String PIXLIVRE="00020126580014BR.GOV.BCB.PIX01364bb923ab-2b18-4757-b8c5-866c999eee285204000053039865802BR5925Marcell Axel Elidio de Ca6009SAO PAULO62140510p4mVw187hp630418A5";

    public static void abrir(Stage dono) {
        Stage janela = prepararJanela(dono,"Sobre o App");
        VBox painel = new VBox(13);
        painel.getStyleClass().add("painel-sobre");
        painel.setAlignment(Pos.TOP_CENTER);
        Label titulo = new Label("SimpleNote");
        titulo.getStyleClass().add("titulo-login");
        Label autor = new Label("Axel Castro · devaxl64");
        autor.getStyleClass().add("texto-secundario");
        Hyperlink github = new Hyperlink("github.com/devaxl64");
        github.setOnAction(e -> App.abrirLink("https://github.com/devaxl64"));
        Hyperlink email = new Hyperlink("axl3764@gmail.com");
        email.setOnAction(e -> App.abrirLink("mailto:axl3764@gmail.com"));
        Label apoio = new Label("Apoie o desenvolvedor");
        apoio.getStyleClass().add("titulo-dialogo");
        GridPane opcoes = new GridPane();
        opcoes.setHgap(9);
        opcoes.setVgap(9);
        opcoes.setAlignment(Pos.CENTER);
        opcoes.add(botaoPix("R$ 2", PIX2, janela),0,0);
        opcoes.add(botaoPix("R$ 5", PIX5, janela),1,0);
        opcoes.add(botaoPix("R$ 10", PIX10, janela),0,1);
        opcoes.add(botaoPix("Outro valor", PIXLIVRE, janela),1,1);
        painel.getChildren().addAll(titulo,autor,github,email,apoio,opcoes);
        Scene cena = new Scene(painel,365,345);
        aplicarEstilo(cena);
        janela.setScene(cena);
        janela.showAndWait();
    }

    private static Button botaoPix(String valor,String codigo,Stage dona) {
        Button botao = new Button(valor);
        botao.getStyleClass().add("botao-apoio");
        botao.setOnAction(e -> abrirQr(dona,valor,codigo));
        return botao;
    }

    private static void abrirQr(Stage dona,String valor,String payload) {
        Stage janela = prepararJanela(dona,"Pix — " + valor);
        VBox painel = new VBox(12);
        painel.setPadding(new Insets(22));
        painel.setAlignment(Pos.CENTER);
        painel.getStyleClass().add("painel-sobre");
        Label legenda = new Label("Escaneie com o aplicativo do banco");
        legenda.getStyleClass().add("titulo-dialogo");
        ImageView imagem = new ImageView();
        try {
            // A câmera lê o payload Pix (o app bancário interpreta os dados do pagamento).
            BitMatrix bits = new QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,250,250,
                Map.of(EncodeHintType.MARGIN, 2));
            WritableImage qr = new WritableImage(bits.getWidth(),bits.getHeight());
            for(int y=0;y<bits.getHeight();y++) for(int x=0;x<bits.getWidth();x++)
                qr.getPixelWriter().setColor(x,y,bits.get(x,y)?Color.BLACK:Color.WHITE);
            imagem.setImage(qr);
        } catch(WriterException erro) {
            legenda.setText("Não foi possível gerar o QR Code.");
        }
        Button copiar = new Button("Copiar código Pix");
        copiar.getStyleClass().add("botao-apoio");
        copiar.setOnAction(e -> {
            ClipboardContent texto = new ClipboardContent();
            texto.putString(payload);
            Clipboard.getSystemClipboard().setContent(texto);
            copiar.setText("Copiado");
        });
        Button fechar = new Button("Fechar");
        fechar.setOnAction(e -> janela.close());
        HBox botoes = new HBox(8,copiar,fechar);
        botoes.setAlignment(Pos.CENTER);
        painel.getChildren().addAll(legenda,imagem,botoes);
        Scene cena = new Scene(painel,345,375);
        aplicarEstilo(cena);
        janela.setScene(cena);
        janela.showAndWait();
    }

    private static Stage prepararJanela(Stage dono,String titulo) {
        Stage janela = new Stage();
        janela.initOwner(dono);
        janela.initModality(Modality.WINDOW_MODAL);
        janela.setTitle(titulo);
        janela.setResizable(false);
        janela.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if(e.getCode()==KeyCode.ESCAPE) {janela.close();e.consume();}
        });
        return janela;
    }
    private static void aplicarEstilo(Scene cena) {
        cena.getStylesheets().add(SupportView.class.getResource("styles.css").toExternalForm());
        if("escuro".equals(CacheStore.configuracao("theme","escuro")))
            cena.getRoot().getStyleClass().add("dark-mode");
    }
}
