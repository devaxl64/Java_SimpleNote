import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private Button btnEntrar;

    @FXML
    private Label lblSenha;

    @FXML
    private Label lblTelaLogin;

    @FXML
    private Label lblUsuario;

    @FXML
    private TextField txtSenha;

    @FXML
    private TextField txtUsuario;

    @FXML
    void fazerLogin(ActionEvent event) {
        String usuario = txtUsuario.getText();
        String senha = txtSenha.getText();

        if (usuario.equals("admin") && senha.equals("1234")) {
            System.out.println("Login efetuado!");
            
        } else {
            System.out.println("Usuário ou senha incorreta!");
        }

    }

}
