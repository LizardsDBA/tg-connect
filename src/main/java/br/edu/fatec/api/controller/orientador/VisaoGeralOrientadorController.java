package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class VisaoGeralOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnVisaoGeral;

    // KPIs
    @FXML private Label lblPendentes;
    @FXML private Label lblHoje;
    @FXML private Label lblAtrasados;
    @FXML private Label lblPrazo;

    @FXML
    private void initialize() {
        // Destaca rota ativa
        if (btnVisaoGeral != null && !btnVisaoGeral.getStyleClass().contains("active")) {
            btnVisaoGeral.getStyleClass().add("active");
        }

        // TODO: popular a partir do service (SQLite)
        setPendentes(0);
        setHoje(0);
        setAtrasados(0);
        setPrazo(0);
    }

    private void setPendentes(int v) { if (lblPendentes != null) lblPendentes.setText(Integer.toString(v)); }
    private void setHoje(int v)      { if (lblHoje != null) lblHoje.setText(Integer.toString(v)); }
    private void setAtrasados(int v) { if (lblAtrasados != null) lblAtrasados.setText(Integer.toString(v)); }
    private void setPrazo(int v)     { if (lblPrazo != null) lblPrazo.setText(Integer.toString(v)); }

    // ===== Navegação =====
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); } // alias explícito
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    public void goChat(){ SceneManager.go("orientador/Chat.fxml"); }

    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
}
