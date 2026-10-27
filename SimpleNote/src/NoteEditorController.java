import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NoteEditorController {
    @FXML private TextField txtTitulo;
    @FXML private TextArea txtConteudo;
    @FXML private Button btnVoltar;
    @FXML private Button btnCor;
    @FXML private Label lblEstado;

    private final PauseTransition autoSalvar = new PauseTransition(Duration.millis(1100));
    private int id;
    private int cor = 1;
    private String tituloPersonalizado;
    private boolean carregando;
    private String salvoTexto = "";
    private String salvoTitulo;
    private int salvaCor = 1;
    private String textoInicial = "";
    private String tituloInicial;
    private int corInicial = 1;

    @FXML private void initialize() {
        autoSalvar.setOnFinished(e -> salvar());
        txtTitulo.textProperty().addListener((o,antes,novo) -> {
            if(novo.length()>40) txtTitulo.setText(novo.substring(0,40));
        });
        txtConteudo.textProperty().addListener((o,antigo,novo) -> {
            if(!carregando) {
                atualizarTitulo();
                autoSalvar.playFromStart();
            }
        });
        txtTitulo.focusedProperty().addListener((o,antigo,atual) -> {
            if(atual) Platform.runLater(txtTitulo::selectAll);
            else if(!carregando) {
                confirmarTitulo();
                salvar();
            }
        });
        txtTitulo.setOnAction(e -> {
            confirmarTitulo();
            txtConteudo.requestFocus();
        });
        // Tab navega pelos botões; dentro do texto as setas continuam movendo o cursor.
        txtConteudo.addEventFilter(KeyEvent.KEY_PRESSED,e -> {
            if(e.getCode() == KeyCode.TAB) {
                if(e.isShiftDown()) btnCor.requestFocus();
                else btnVoltar.requestFocus();
                e.consume();
            } else if(e.getCode() == KeyCode.DOWN && txtConteudo.getCaretPosition() == txtConteudo.getLength()) {
                // No fim da nota, desce para uma nova linha sem deslocar o texto existente.
                txtConteudo.appendText("\n");
                e.consume();
            }
        });
    }

    public void configurar(Note nota) {
        carregando = true;
        if(nota != null) {
            id = nota.getId();
            cor = nota.getCor();
            tituloPersonalizado = nota.getTituloPersonalizado();
            txtConteudo.setText(nota.getConteudo());
        } else {
            cor = CacheStore.corPadrao();
        }
        salvoTexto = txtConteudo.getText();
        salvoTitulo = tituloPersonalizado;
        salvaCor = cor;
        textoInicial = salvoTexto;
        tituloInicial = salvoTitulo;
        corInicial = cor;
        atualizarTitulo();
        aplicarCor();
        carregando = false;
        Platform.runLater(txtConteudo::requestFocus);
    }

    public void ativarFechamento() {
        Stage janela = (Stage) txtConteudo.getScene().getWindow();
        janela.setOnCloseRequest(e -> {if(!salvar())e.consume();});
    }

    private String tituloAutomatico() {
        String resumo = Note.gerarResumo(txtConteudo.getText());
        return resumo.isBlank() ? "SimpleNote" : resumo;
    }
    private void atualizarTitulo() {
        if(!txtTitulo.isFocused())
            txtTitulo.setText(tituloPersonalizado == null ? tituloAutomatico() : tituloPersonalizado);
    }
    private void confirmarTitulo() {
        String digitado = txtTitulo.getText().strip();
        String automatico = tituloAutomatico();
        tituloPersonalizado = digitado.isBlank() || digitado.equals(automatico)
            ? null : (digitado.length()>40 ? digitado.substring(0,40) : digitado);
        atualizarTitulo();
    }
    private void aplicarCor() {
        String fundo = NoteColors.fundo(cor);
        txtConteudo.setStyle("-fx-control-inner-background:"+fundo+";-fx-background-color:"+fundo+";");
        btnCor.setStyle("-fx-background-color:"+fundo+";-fx-border-color:"+NoteColors.borda(cor)+";");
    }
    @FXML private void abrirCores() {
        ColorPalette.editor(btnCor,escolha -> {
            cor = escolha;
            aplicarCor();
            salvar();
        });
    }

    private boolean salvar() {
        try {
            if(txtTitulo.isFocused()) confirmarTitulo();
            String texto = txtConteudo.getText();
            if(texto.equals(salvoTexto) && Objects.equals(tituloPersonalizado,salvoTitulo) && cor==salvaCor)
                return true;
            int anterior = id;
            id = CacheStore.salvar(id,texto,tituloPersonalizado,cor);
            salvoTexto = texto;
            salvoTitulo = tituloPersonalizado;
            salvaCor = cor;
            if(!texto.isBlank() || anterior > 0) SyncService.agendar();
            lblEstado.setText("");
            return true;
        } catch(SQLException erro) {
            lblEstado.setText("Não foi possível salvar localmente.");
            erro.printStackTrace();
            return false;
        }
    }

    @FXML private void voltar() {
        autoSalvar.stop();
        if(txtTitulo.isFocused()) confirmarTitulo();
        if(!salvar())return;
        boolean alterado = !txtConteudo.getText().equals(textoInicial)
            || !Objects.equals(tituloInicial,tituloPersonalizado) || corInicial!=cor;
        if(alterado && !txtConteudo.getText().isBlank()) Navigation.avisarNaLista("Salvo.");
        try {Navigation.abrirLista(txtConteudo);}
        catch(IOException erro) {lblEstado.setText("Não foi possível voltar.");erro.printStackTrace();}
    }

    @FXML private void teclaEditor(KeyEvent e) {
        if(e.getCode()==KeyCode.ESCAPE) {voltar();e.consume();}
    }
}
