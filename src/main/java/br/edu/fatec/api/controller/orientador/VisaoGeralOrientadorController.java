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

public class VisaoGeralOrientadorController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnVisaoGeral;
    @FXML private Button btnSouCoordenador;

    // KPIs (com os nomes semânticos corretos)
    @FXML private Label lblPendentes;
    @FXML private Label lblTotalOrientandos;
    @FXML private Label lblAlunosReprovados;
    @FXML private Label lblTGsConcluidos;

    // Instancia o seu Service
    private final DashboardOrientadorService service = new DashboardOrientadorService();

    private Kpis kpis;

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

        // =======================================================
        // TESTE 1: A SESSÃO ESTÁ NULA?
        System.out.println("### DEBUG [SESSÃO]: Usuário da Sessão: " + u);
        // =======================================================

        boolean isCoord = (u != null && u.getRole() == Role.COORDENADOR);
        if (btnSouCoordenador != null) {
            btnSouCoordenador.setVisible(isCoord);
            btnSouCoordenador.setManaged(isCoord);
        }

        // Chama o método carregarKpis() do seu Service
        if (u != null) {
            this.kpis = service.carregarKpis(u.getId())
                    .orElse(new Kpis(0, 0, 0, 0));
        } else {
            // Fallback se não houver usuário (ex: erro de sessão)
            System.err.println("### DEBUG [SESSÃO]: Usuário NULO! Usando zeros.");
            this.kpis = new Kpis(0, 0, 0, 0);
        }

        // =======================================================
        // TESTE 2: O QUE VEIO DO BANCO?
        System.out.println("### DEBUG [BANCO]: KPIs carregados: " + kpis);
        // =======================================================

        // Popula os labels com os dados do DTO
        setPendentes(kpis.pendencias());
        setTotalOrientandos(kpis.orientandos());
        setAlunosReprovados(kpis.reprovacoes());
        setTGsConcluidos(kpis.concluidos());
    }

    // Métodos 'set' (com os nomes semânticos)
    private void setPendentes(int v)        { if (lblPendentes != null) lblPendentes.setText(Integer.toString(v)); }
    private void setTotalOrientandos(int v) { if (lblTotalOrientandos != null) lblTotalOrientandos.setText(Integer.toString(v)); }
    private void setAlunosReprovados(int v) { if (lblAlunosReprovados != null) lblAlunosReprovados.setText(Integer.toString(v)); }
    private void setTGsConcluidos(int v)    { if (lblTGsConcluidos != null) lblTGsConcluidos.setText(Integer.toString(v)); }

    // ===== Navegação (Métodos completos) =====
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }

    public void goHistorico() {
        SceneManager.go("orientador/Historico.fxml");
    }

    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    // MÉTODO QUE ESTAVA QUEBRADO (agora corrigido)
    public void goHomeCoord(){
        SceneManager.go("coordenacao/VisaoGeral.fxml");
    }

    public void goHome(){
        SceneManager.go("orientador/VisaoGeral.fxml");
    }

    public void logout(){
        SceneManager.go("login/Login.fxml");
    }

}