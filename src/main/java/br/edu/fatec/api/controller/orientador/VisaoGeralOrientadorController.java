package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.service.DashboardOrientadorService;
import br.edu.fatec.api.service.DashboardOrientadorService.Kpis;
import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.controller.orientador.ChatOrientadorController;
// ----- IMPORT ADICIONADO AQUI -----
import br.edu.fatec.api.controller.orientador.HistoricoOrientadorController;

public class VisaoGeralOrientadorController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnVisaoGeral;
    @FXML private Button btnSouCoordenador;

    // KPIs
    @FXML private Label lblPendentes;
    @FXML private Label lblTotalOrientandos;
    @FXML private Label lblAlunosReprovados;
    @FXML private Label lblTGsConcluidos;

    private final DashboardOrientadorService service = new DashboardOrientadorService();
    private Kpis kpis;

    @FXML
    private void initialize() {
        if (btnVisaoGeral != null && !btnVisaoGeral.getStyleClass().contains("active")) {
            btnVisaoGeral.getStyleClass().add("active");
        }
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        User u = Session.getUser();
        System.out.println("### DEBUG [SESSÃO]: Usuário da Sessão: " + u);

        boolean isCoord = (u != null && u.getRole() == Role.COORDENADOR);
        if (btnSouCoordenador != null) {
            btnSouCoordenador.setVisible(isCoord);
            btnSouCoordenador.setManaged(isCoord);
        }

        if (u != null) {
            this.kpis = service.carregarKpis(u.getId())
                    .orElse(new Kpis(0, 0, 0, 0));
        } else {
            System.err.println("### DEBUG [SESSÃO]: Usuário NULO! Usando zeros.");
            this.kpis = new Kpis(0, 0, 0, 0);
        }

        System.out.println("### DEBUG [BANCO]: KPIs carregados: " + kpis);

        setPendentes(kpis.pendencias());
        setTotalOrientandos(kpis.orientandos());
        setAlunosReprovados(kpis.reprovacoes());
        setTGsConcluidos(kpis.concluidos());
    }

    private void setPendentes(int v)        { if (lblPendentes != null) lblPendentes.setText(Integer.toString(v)); }
    private void setTotalOrientandos(int v) { if (lblTotalOrientandos != null) lblTotalOrientandos.setText(Integer.toString(v)); }
    private void setAlunosReprovados(int v) { if (lblAlunosReprovados != null) lblAlunosReprovados.setText(Integer.toString(v)); }
    private void setTGsConcluidos(int v)    { if (lblTGsConcluidos != null) lblTGsConcluidos.setText(Integer.toString(v)); }

    // ===== Navegação =====
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }

    /**
     * ATUALIZADO: Agora chama onRefreshData() para recarregar o histórico.
     */
    public void goHistorico() {
        SceneManager.go("orientador/Historico.fxml", c -> {
            HistoricoOrientadorController ctrl = (HistoricoOrientadorController) c;
            ctrl.onRefreshData();
        });
    }

    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    public void goHomeCoord(){
        SceneManager.go("coordenacao/VisaoGeral.fxml");
    }

    public void goHome(){
        SceneManager.go("orientador/VisaoGeral.fxml");
    }

    public void logout(){
        SceneManager.go("login/Login.fxml");
    }

    public void goSolicitacoes() {
        SceneManager.go("orientador/SolicitacoesOrientador.fxml");
    }
}