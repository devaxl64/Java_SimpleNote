import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private Label lblMensagem;

    @FXML
    private void fazerLogin(ActionEvent event) {
        String usuario = txtUsuario.getText().trim();
        String senha = txtSenha.getText();

        // Login original do projeto. Autenticação real fica para etapa futura.
        if (usuario.equals("admin") && senha.equals("1234")) {
            try {
                Navigation.abrirLista(txtUsuario);
            } catch (IOException erro) {
                lblMensagem.setText("Não foi possível abrir as notas.");
                erro.printStackTrace();
            }
        } else {
            lblMensagem.setText("Usuário ou senha incorretos.");
        }
    }
}
