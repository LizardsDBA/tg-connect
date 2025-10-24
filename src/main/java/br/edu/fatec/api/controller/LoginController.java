package br.edu.fatec.api.controller;

import br.edu.fatec.api.controller.aluno.EditorAlunoController;
import br.edu.fatec.api.dao.JdbcLoginDao;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.service.LoginService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class LoginController {


    private final LoginService loginService = new LoginService(new JdbcLoginDao());

    // ajuste os fx:id conforme seu FXML
    @FXML private VBox allView;
    @FXML private TextField txtEmail;
    @FXML private PasswordField pfSenha;
    @FXML private Label lblErro;
    @FXML private Button btnEnter;

    @FXML
    public void initialize() {
        allView.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                btnEnter.fire();
                evt.consume(); // evita propagação se quiser
            }
        });
    }

    @FXML
    private void onEntrar() {
        String email = txtEmail.getText();
        String senha = pfSenha.getText();

        loginService.login(email, senha).ifPresentOrElse((User u) -> {
            lblErro.setText("");
            Session.setUser(u);
            switch (u.getRole()) {
                case ALUNO:
                    SceneManager.go("aluno/Dashboard.fxml");
                    break;
                case ORIENTADOR:
                    SceneManager.go("orientador/VisaoGeral.fxml");
                    break;
                case COORDENADOR:
                    SceneManager.go("coordenacao/VisaoGeral.fxml");
                    break;
                default:
                    lblErro.setText("Perfil desconhecido.");
            }
        }, () -> lblErro.setText("E-mail ou senha inválidos"));
    }
}
