package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PainelOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnPainel;

    // Filtros
    @FXML private ChoiceBox<String> cbFiltroPrioridade;
    @FXML private TextField txtBuscaAluno;

    // Tabela
    @FXML private TableView<?> tblPendencias;
    @FXML private TableColumn<?,?> colAluno, colSecao, colPedido, colPrioridade, colAcoes;

    @FXML
    private void initialize() {
        // Destaca rota ativa na sidebar
        if (btnPainel != null && !btnPainel.getStyleClass().contains("active")) {
            btnPainel.getStyleClass().add("active");
        }

        // Placeholders e filtros iniciais
        if (tblPendencias != null) {
            tblPendencias.setPlaceholder(new Label("Sem pendências no momento."));
        }
        if (cbFiltroPrioridade != null) {
            cbFiltroPrioridade.getItems().setAll("Todas", "Alta", "Média", "Baixa");
            cbFiltroPrioridade.getSelectionModel().selectFirst();
        }
    }

    // ===== Ações da tabela/toolbar =====
    public void limparFiltros() {
        if (cbFiltroPrioridade != null) cbFiltroPrioridade.getSelectionModel().selectFirst();
        if (txtBuscaAluno != null) txtBuscaAluno.clear();
        // TODO: recarregar lista a partir do service
    }

    public void abrirEditor() {
        // TODO: opcional: validar seleção antes de abrir
        SceneManager.go("orientador/Editor.fxml");
    }

    public void abrirChat() {
        // TODO: opcional: enviar contexto do aluno selecionado
        SceneManager.go("orientador/Chat.fxml");
    }

    // ===== Navegação =====
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    // (ALTERADO POR MATHEUS - adicionado callback onReady)
    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }
}
