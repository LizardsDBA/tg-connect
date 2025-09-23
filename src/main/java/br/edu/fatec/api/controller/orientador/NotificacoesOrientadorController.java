package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class NotificacoesOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnNotificacoes;

    // Tabela
    @FXML private TableView<NotificacaoVM> notificationsTable;
    @FXML private TableColumn<NotificacaoVM, String> colQuando;
    @FXML private TableColumn<NotificacaoVM, String> colAluno;
    @FXML private TableColumn<NotificacaoVM, String> colVersao;
    @FXML private TableColumn<NotificacaoVM, String> colMensagem;

    @FXML
    private void initialize() {
        // Destaque na sidebar
        if (btnNotificacoes != null && !btnNotificacoes.getStyleClass().contains("active")) {
            btnNotificacoes.getStyleClass().add("active");
        }

        // Configura colunas
        if (colQuando != null)   colQuando.setCellValueFactory(c -> c.getValue().quandoProperty());
        if (colAluno != null)    colAluno.setCellValueFactory(c -> c.getValue().alunoProperty());
        if (colVersao != null)   colVersao.setCellValueFactory(c -> c.getValue().versaoProperty());
        if (colMensagem != null) colMensagem.setCellValueFactory(c -> c.getValue().mensagemProperty());

        // Placeholder
        if (notificationsTable != null) {
            notificationsTable.setPlaceholder(new Label("Sem notificações no momento."));
        }

        // TODO: carregar dados do service (SQLite)
        // notificationsTable.setItems(notificacaoService.listar());
    }

    // Ações
    public void atualizar() {
        // TODO: recarregar do service
        // notificationsTable.setItems(notificacaoService.listar());
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
    public void goChat(){ SceneManager.go("orientador/Chat.fxml"); }

    /* ===== VM simples para bind (substitua pelo seu modelo/DTO) ===== */
    public static class NotificacaoVM {
        private final javafx.beans.property.SimpleStringProperty quando =
                new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty aluno =
                new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty versao =
                new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty mensagem =
                new javafx.beans.property.SimpleStringProperty();

        public NotificacaoVM(String quando, String aluno, String versao, String mensagem) {
            this.quando.set(quando);
            this.aluno.set(aluno);
            this.versao.set(versao);
            this.mensagem.set(mensagem);
        }
        public javafx.beans.property.StringProperty quandoProperty()  { return quando; }
        public javafx.beans.property.StringProperty alunoProperty()   { return aluno; }
        public javafx.beans.property.StringProperty versaoProperty()  { return versao; }
        public javafx.beans.property.StringProperty mensagemProperty(){ return mensagem; }
    }
}
