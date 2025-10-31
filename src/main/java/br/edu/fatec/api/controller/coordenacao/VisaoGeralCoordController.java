package br.edu.fatec.api.controller.coordenacao;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import br.edu.fatec.api.controller.BaseController;

public class VisaoGeralCoordController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnVisaoGeral;

    // KPIs
    @FXML private Label lblCursos, lblAlunos, lblOrientadores;

    @FXML
    private void initialize() {
        // Marca a rota ativa na sidebar
        if (btnVisaoGeral != null && !btnVisaoGeral.getStyleClass().contains("active")) {
            btnVisaoGeral.getStyleClass().add("active");
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // TODO: puxar de um service/DAO (placeholders)
        setCursos(15);
        setAlunos(250);
        setOrientadores(10);
    }

    private void setCursos(int n){ if (lblCursos != null) lblCursos.setText(Integer.toString(n)); }
    private void setAlunos(int n){ if (lblAlunos != null) lblAlunos.setText(Integer.toString(n)); }
    private void setOrientadores(int n){ if (lblOrientadores != null) lblOrientadores.setText(Integer.toString(n)); }

    // ===== Navegação =====
    public void goHomeOrient(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goMapa(){ SceneManager.go("coordenacao/Mapa.fxml"); }
    public void goAndamento(){ SceneManager.go("coordenacao/Andamento.fxml"); }

    // (Opcional) futuros handlers:
    // public void goRelatorios(){ SceneManager.go("coordenacao/Relatorios.fxml"); }
}
