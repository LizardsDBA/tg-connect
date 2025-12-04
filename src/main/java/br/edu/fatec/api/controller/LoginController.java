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
import java.sql.SQLException;

public class LoginController {

    private final LoginService loginService = new LoginService(new JdbcLoginDao());

    @FXML
    private VBox allView;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField pfSenha;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnEnter;

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
                    // LÓGICA CORRIGIDA: Verificar status do aluno
                    JdbcSolicitacaoOrientacaoDao solicitacaoDao = new JdbcSolicitacaoOrientacaoDao();
                    try {
                        boolean temOrientacao = solicitacaoDao.alunoTemOrientacaoAtiva(u.getId());
                        boolean temSolicitacaoPendente = solicitacaoDao.alunoTemSolicitacaoPendente(u.getId());

                        // FLUXO CORRIGIDO:
                        // 1. Se tem solicitação pendente → tela de solicitação (mostra status pendente)
                        if (temSolicitacaoPendente) {
                            SceneManager.go("aluno/SolicitacaoOrientacaoAluno.fxml");
                            return;
                        }

                        // 2. Se não tem orientação e não tem solicitação pendente → tela de solicitação (nova)
                        if (!temOrientacao) {
                            SceneManager.go("aluno/SolicitacaoOrientacaoAluno.fxml");
                            return;
                        }

                        // 3. Se tem orientação ativa → dashboard normal
                        SceneManager.go("aluno/Dashboard.fxml");

                    } catch (SQLException e) {
                        e.printStackTrace();
                        lblErro.setText("Erro ao verificar status do aluno");
                    }
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