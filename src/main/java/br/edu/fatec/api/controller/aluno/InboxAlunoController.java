package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class InboxAlunoController {

    @FXML private Label lblPeerName, lblStatus;
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatMessages;
    @FXML private TextField txtMessage;

    // Navegação
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){ SceneManager.go("aluno/Inbox.fxml"); }
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }

    @FXML
    private void initialize() {
        // Placeholder inicial (pode vir do service)
        lblPeerName.setText("Prof. Orientador");
        lblStatus.setText("online");
        appendPeer("Olá! Envie sua versão quando estiver pronta.");
        autoScroll();
    }

    // Enviar mensagem do aluno
    public void enviarMensagem() {
        var text = txtMessage.getText();
        if (text == null || text.isBlank()) return;
        appendSelf(text.trim());
        txtMessage.clear();
        autoScroll();
        // TODO: Persistir/Enviar ao backend (SQLite -> tabela 'notificacao' ou 'chat')
    }

    // Helpers para montar “bubbles”
    private void appendSelf(String msg)  { chatMessages.getChildren().add(buildBubble(msg, true)); }
    private void appendPeer(String msg)  { chatMessages.getChildren().add(buildBubble(msg, false)); }

    private Node buildBubble(String text, boolean self) {
        Label bubble = new Label(text);
        bubble.getStyleClass().addAll("msg", self ? "msg-self" : "msg-peer");
        bubble.setWrapText(true);
        bubble.setMaxWidth(480);

        HBox line = new HBox(bubble);
        HBox.setHgrow(bubble, Priority.NEVER);
        line.setFillHeight(true);
        line.setSpacing(0);
        line.setAlignment(self ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        return line;
    }

    private void autoScroll() {
        // Força scroll no fim após renderização
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
    }
}
