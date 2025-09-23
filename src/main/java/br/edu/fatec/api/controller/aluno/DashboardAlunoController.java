package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DashboardAlunoController {

    @FXML private Button btnDashboard;
    @FXML private Label lblConclusao, lblPendencias, lblUltimaVersao;

    @FXML
    private void initialize() {
        if (btnDashboard != null) btnDashboard.getStyleClass().add("active");

        // TODO: puxar do service (SQLite) — placeholders de exemplo:
        setConclusao(48);          // ex.: 48%
        setPendencias(3);          // ex.: 3
        setUltimaVersao("v1.4");   // ex.: v1.4
    }

    private void setConclusao(int pct) { lblConclusao.setText(pct + "%"); }
    private void setPendencias(int n) { lblPendencias.setText(Integer.toString(n)); }
    private void setUltimaVersao(String v) { lblUltimaVersao.setText(v); }

    // Navegação
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){ SceneManager.go("aluno/Inbox.fxml"); }
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }
}
