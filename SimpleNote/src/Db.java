import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Db {

    public static Connection conectar() throws SQLException {
        Path arquivo = Path.of(
            System.getProperty("user.home"),
            ".config", "simplenote", "database.properties"
        );

        Properties configuracao = new Properties();

        try (InputStream entrada = Files.newInputStream(arquivo)) {
            configuracao.load(entrada);
        } catch (IOException erro) {
            throw new SQLException("Configuração do banco não encontrada: " + arquivo, erro);
        }

        String url = configuracao.getProperty("db.url");
        String usuario = configuracao.getProperty("db.user");
        String senha = configuracao.getProperty("db.password");

        if (url == null || usuario == null || senha == null) {
            throw new SQLException("Configuração do banco está incompleta.");
        }

        return DriverManager.getConnection(url, usuario, senha);
    }
}
