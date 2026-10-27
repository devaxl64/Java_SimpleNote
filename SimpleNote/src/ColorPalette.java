import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ColorPalette {
    // Apenas seleciona a cor da nota; os nomes são alterados na tela inicial.
    public static void editor(Button origem, Consumer<Integer> selecionar) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("menu-paleta");
        GridPane grade = new GridPane();
        grade.setHgap(8);
        grade.setVgap(8);
        grade.setPadding(new Insets(8));
        for (int id=1;id<=6;id++) {
            final int cor = id;
            Button botao = criarBotao(cor,88,82);
            botao.setTooltip(new Tooltip("Selecionar esta cor"));
            botao.setOnAction(e -> {selecionar.accept(cor);menu.hide();});
            grade.add(botao,(id-1)%3,(id-1)/3);
        }
        CustomMenuItem item = new CustomMenuItem(grade,false);
        item.setHideOnClick(false);
        menu.getItems().add(item);
        menu.show(origem,javafx.geometry.Side.BOTTOM,0,7);
        Platform.runLater(() -> grade.getChildren().get(0).requestFocus());
    }

    // Clique numa cor: ela vira a padrão e o nome é editado dentro do próprio bloco.
    public static void configuracoes(Stage proprietario,Runnable atualizar) {
        Stage janela = new Stage();
        janela.initOwner(proprietario);
        janela.initModality(Modality.WINDOW_MODAL);
        janela.setTitle("Cores");
        janela.setResizable(false);
        GridPane grade = new GridPane();
        grade.setHgap(10);
        grade.setVgap(10);
        grade.setAlignment(Pos.CENTER);
        final List<Button> botoes = new ArrayList<>();

        for (int id=1;id<=6;id++) {
            final int cor=id;
            Button botao=criarBotao(cor,118,112);
            botoes.add(botao);
            if(cor==CacheStore.corPadrao()) botao.getStyleClass().add("cor-selecionada");
            TextArea editar=new TextArea();
            editar.setText(CacheStore.nomeCor(cor));
            editar.setWrapText(true);
            editar.getStyleClass().add("cor-campo");
            editar.setStyle("-fx-control-inner-background:"+NoteColors.fundo(cor)+";");
            editar.setPrefSize(118,112);
            editar.setMaxSize(118,112);
            editar.setVisible(false);
            editar.setManaged(false);
            StackPane bloco=new StackPane(botao,editar);
            bloco.setAlignment(Pos.CENTER);

            Runnable confirmar=()->{
                if(!editar.isVisible())return;
                String novo=editar.getText().strip();
                try {
                    if(!novo.equals(CacheStore.nomeCor(cor))) {
                        CacheStore.renomearCor(cor,novo);
                        SyncService.agendar();
                    }
                    Label texto=(Label)botao.getGraphic();
                    texto.setText(novo);
                } catch(SQLException erro) {erro.printStackTrace();}
                editar.setVisible(false);
                editar.setManaged(false);
                botao.setVisible(true);
                botao.setManaged(true);
            };
            editar.textProperty().addListener((o,a,n)->{
                String normal=n.replace("\n","").replace("\r","");
                if(normal.length()>20)normal=normal.substring(0,20);
                if(!normal.equals(n))editar.setText(normal);
            });
            editar.focusedProperty().addListener((o,a,temFoco)->{
                if(!temFoco)confirmar.run();
            });
            editar.setOnKeyPressed(e->{
                if(e.getCode()==KeyCode.ENTER || e.getCode()==KeyCode.TAB) {
                    confirmar.run();
                    botao.requestFocus();
                    e.consume();
                } else if(e.getCode()==KeyCode.ESCAPE) {
                    editar.setText(CacheStore.nomeCor(cor));
                    confirmar.run();
                    botao.requestFocus();
                    e.consume();
                }
            });
            botao.setOnAction(e->{
                try {
                    CacheStore.definirCorPadrao(cor);
                    SyncService.agendar();
                    atualizar.run();
                    for(Button outro:botoes)outro.getStyleClass().remove("cor-selecionada");
                    botao.getStyleClass().add("cor-selecionada");
                } catch(SQLException erro) {erro.printStackTrace();}
                editar.setText(CacheStore.nomeCor(cor));
                botao.setVisible(false);
                botao.setManaged(false);
                editar.setManaged(true);
                editar.setVisible(true);
                Platform.runLater(editar::requestFocus);
            });
            grade.add(bloco,(id-1)%3,(id-1)/3);
        }
        VBox corpo=new VBox(grade);
        corpo.setAlignment(Pos.CENTER);
        corpo.getStyleClass().add("painel-cores");
        Scene cena=new Scene(corpo,448,308);
        cena.getStylesheets().add(ColorPalette.class.getResource("styles.css").toExternalForm());
        if("escuro".equals(CacheStore.configuracao("theme","escuro")))
            cena.getRoot().getStyleClass().add("dark-mode");
        cena.setOnKeyPressed(e->{if(e.getCode()==KeyCode.ESCAPE)janela.close();});
        janela.setScene(cena);
        janela.showAndWait();
    }

    private static Button criarBotao(int cor,double largura,double altura) {
        Label texto=new Label(CacheStore.nomeCor(cor));
        texto.setWrapText(true);
        texto.setMaxWidth(largura-18);
        texto.setAlignment(Pos.CENTER);
        texto.getStyleClass().add("cor-texto");
        Button botao=new Button();
        botao.setGraphic(texto);
        botao.setPrefSize(largura,altura);
        botao.setMinSize(largura,altura);
        botao.setMaxSize(largura,altura);
        botao.getStyleClass().add("cor-bloco");
        botao.setStyle("-fx-background-color:"+NoteColors.fundo(cor)+
            ";-fx-border-color:"+NoteColors.borda(cor)+";");
        return botao;
    }
}
