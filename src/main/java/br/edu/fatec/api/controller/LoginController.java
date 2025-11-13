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
import br.edu.fatec.api.dao.JdbcSolicitacaoOrientacaoDao;
import javafx.concurrent.Task;

public class LoginController {

    private final LoginService loginService = new LoginService(new JdbcLoginDao());

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
                evt.consume();
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
                    verificarOrientadorAluno(u.getId());
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

    private void verificarOrientadorAluno(Long alunoId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                JdbcSolicitacaoOrientacaoDao dao = new JdbcSolicitacaoOrientacaoDao();
                return dao.alunoTemOrientador(alunoId);
            }
        };

        task.setOnSucceeded(e -> {
            boolean temOrientador = task.getValue();

            if (temOrientador) {
                SceneManager.go("aluno/Dashboard.fxml");
            } else {
                SceneManager.go("aluno/SolicitarOrientacao.fxml");
            }
        });

        task.setOnFailed(e -> {
            // CORRIGIDO: pegar exceção da task, não do evento
            task.getException().printStackTrace();
            SceneManager.go("aluno/Dashboard.fxml");
        });

        new Thread(task).start();
    }
}