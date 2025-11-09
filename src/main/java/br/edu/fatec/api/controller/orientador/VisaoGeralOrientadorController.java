package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.service.DashboardOrientadorService; // NOVO IMPORT

public class VisaoGeralOrientadorController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnVisaoGeral;
    @FXML private Button btnSouCoordenador;

    // KPIs (IDs mantidos do FXML)
    @FXML private Label lblPendentes;
    @FXML private Label lblHoje;
    @FXML private Label lblAtrasados;
    @FXML private Label lblPrazo;

    // NOVO SERVICE
    private final DashboardOrientadorService service = new DashboardOrientadorService();

    @FXML
    private void initialize() {
        // Destaca rota ativa
        if (btnVisaoGeral != null && !btnVisaoGeral.getStyleClass().contains("active")) {
            btnVisaoGeral.getStyleClass().add("active");
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        boolean isCoord = (u.getRole() == Role.COORDENADOR);
        if (btnSouCoordenador != null) {
            btnSouCoordenador.setVisible(isCoord);
            btnSouCoordenador.setManaged(isCoord);
        }

        // --- LÓGICA ATUALIZADA ---
        // Carrega os 4 KPIs do service
        service.carregarKpis(u.getId()).ifPresent(kpis -> {
            setPendentes(kpis.pendencias());
            setOrientandos(kpis.orientandos());
            setComReprovacoes(kpis.reprovacoes());
            setConcluidos(kpis.concluidos());
        });
    }

    // Métodos de Set atualizados para os novos KPIs
    private void setPendentes(int v) { if (lblPendentes != null) lblPendentes.setText(Integer.toString(v)); }
    private void setOrientandos(int v) { if (lblHoje != null) lblHoje.setText(Integer.toString(v)); }
    private void setComReprovacoes(int v) { if (lblAtrasados != null) lblAtrasados.setText(Integer.toString(v)); }
    private void setConcluidos(int v) { if (lblPrazo != null) lblPrazo.setText(Integer.toString(v)); }

    // ===== Navegação (Sem alterações) =====
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }
    public void goHomeCoord(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
}