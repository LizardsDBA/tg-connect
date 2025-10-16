package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.service.DashboardAlunoService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DashboardAlunoController {

    @FXML private Button btnDashboard;
    @FXML private Label lblConclusao, lblPendencias, lblUltimaVersao;

    private final DashboardAlunoService service = new DashboardAlunoService();

    @FXML
    private void initialize() {
        if (btnDashboard != null) btnDashboard.getStyleClass().add("active");

        // Garante que veio do login
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        // Carrega KPIs do aluno logado
        service.carregarKpis(u.getId()).ifPresentOrElse(k -> {
            // percentual é double no service; arredondo para int para manter seu setConclusao(int)
            int pct = (int) Math.round(k.percentual());
            setConclusao(pct);
            setPendencias(k.pendencias());
            setUltimaVersao(k.ultimaVersao());
        }, () -> {
            // fallback seguro
            setConclusao(0);
            setPendencias(0);
            setUltimaVersao("—");
        });
    }

    private void setConclusao(int pct) { lblConclusao.setText(pct + "%"); }
    private void setPendencias(int n) { lblPendencias.setText(Integer.toString(n)); }
    private void setUltimaVersao(String v) { lblUltimaVersao.setText(v); }

    // Navegação (mantida)
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){ SceneManager.go("aluno/Inbox.fxml"); }
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }
}
