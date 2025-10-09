package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ChatOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnChat;

    // Lista de alunos
    @FXML private ListView<String> lstAlunos;

    // Chat
    @FXML private Label lblChatHeader;
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatMessages;
    @FXML private TextField txtMessage;

    // ===== Ciclo de vida =====
    @FXML
    private void initialize() {
        if (btnChat != null && !btnChat.getStyleClass().contains("active")) {
            btnChat.getStyleClass().add("active");
        }

        // Mock de alunos (substituir por service/DAO)
        if (lstAlunos != null) {
            lstAlunos.setItems(FXCollections.observableArrayList(
                    "Ana Souza (RA 12345)",
                    "Bruno Lima (RA 23456)",
                    "Carla Mendes (RA 34567)"
            ));
            lstAlunos.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selecionarAluno(newV));
            lstAlunos.getSelectionModel().selectFirst(); // seleciona o primeiro por padrão
        }

        // Placeholder inicial
        appendPeer("Selecione um aluno e envie sua mensagem.");
        autoScroll();
    }

    // ===== Ações do chat =====
    public void enviarMensagem() {
        String text = (txtMessage != null) ? txtMessage.getText() : null;
        if (text == null || text.isBlank()) return;
        appendSelf(text.trim());
        txtMessage.clear();
        autoScroll();

        // TODO: persistir mensagem (SQLite) ex.: chat_msg(aluno_id, professor_id, autor, texto, criado_em)
    }

    private void selecionarAluno(String aluno) {
        if (aluno == null || aluno.isBlank()) return;
        if (lblChatHeader != null) lblChatHeader.setText(aluno);

        // TODO: carregar histórico do aluno selecionado
        chatMessages.getChildren().clear();
        appendPeer("Conversa iniciada com " + aluno + ".");
        autoScroll();
    }

    // ===== Helpers de UI (bubbles) =====
    private void appendSelf(String msg)  { chatMessages.getChildren().add(buildBubble(msg, true)); }
    private void appendPeer(String msg)  { chatMessages.getChildren().add(buildBubble(msg, false)); }

    private Node buildBubble(String text, boolean self) {
        Label bubble = new Label(text);
        bubble.getStyleClass().addAll("msg", self ? "msg-self" : "msg-peer");
        bubble.setWrapText(true);
        bubble.setMaxWidth(520);

        HBox line = new HBox(bubble);
        HBox.setHgrow(bubble, Priority.NEVER);
        line.setFillHeight(true);
        line.setAlignment(self ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return line;
    }

    private void autoScroll() {
        if (chatScroll == null) return;
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
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
}
