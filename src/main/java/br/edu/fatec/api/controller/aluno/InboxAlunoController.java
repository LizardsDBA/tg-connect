package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.nav.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InboxAlunoController {

    @FXML private Label lblPeerName, lblStatus;
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatMessages;
    @FXML private TextField txtMessage;

    // Contexto de sessão
    private Long alunoId;                // definido via setAlunoContext(...) antes do onReady()
    private Long trabalhoId;             // resolvido no loadContext()
    private Long orientadorId;           // resolvido no loadContext()
    private LocalDateTime lastCreatedAt; // controle do polling
    private Timeline poller;             // polling a cada 10s
    private boolean ready = false;       // sinaliza que contexto foi carregado

    // Navegação
    public void goHome(){ stopPolling(); SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ stopPolling(); SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ stopPolling(); SceneManager.go("aluno/Dashboard.fxml"); }

    public void goInbox(){
        stopPolling();
        SceneManager.go("aluno/Inbox.fxml", c -> {
            InboxAlunoController ctrl = (InboxAlunoController) c;
            ctrl.setAlunoContext(this.alunoId); // reaproveita o aluno atual
            ctrl.onReady();
        });
    }

    public void goEditor(){ stopPolling(); SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ stopPolling(); SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ stopPolling(); SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ stopPolling(); SceneManager.go("aluno/Historico.fxml"); }

    /** Defina o id do aluno logado ANTES de chamar onReady(). */
    public void setAlunoContext(Long alunoId) {
        this.alunoId = alunoId;
    }

    @FXML
    private void initialize() {
        // Setup visual apenas — NÃO acessar BD aqui (alunoId ainda pode estar null)
        lblPeerName.setText("(carregando orientador...)");
        lblStatus.setText(""); // status online está fora do escopo
    }

    /** Chame após setAlunoContext(...). Aqui ocorre a carga de dados. */
    public void onReady() {
        ready = false;
        if (alunoId == null) {
            showError("Inbox", new IllegalStateException("alunoId não definido antes do onReady()"));
            return;
        }
        try {
            loadContext();     // resolve trabalhoId, orientadorId e nome do orientador
            loadHistorico();   // carrega histórico completo
            marcarComoLidas(); // marca recebidas como lidas
            startPolling();    // inicia polling de 10s
            ready = true;
        } catch (Exception e) {
            showError("Falha ao carregar Inbox", e);
            ready = false;
        }
    }

    /** Opcional: parar polling ao sair da tela. */
    public void stopPolling() {
        if (poller != null) poller.stop();
    }

    // ============ Fluxo principal ============

    private void loadContext() throws SQLException {
        String sql = """
            SELECT t.id AS trabalho_id, t.orientador_id, u.nome AS orientador_nome
            FROM trabalhos_graduacao t
            JOIN usuarios u ON u.id = t.orientador_id
            WHERE t.aluno_id = ?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.trabalhoId = rs.getLong("trabalho_id");
                    this.orientadorId = rs.getLong("orientador_id");
                    String orientadorNome = rs.getString("orientador_nome");
                    lblPeerName.setText(orientadorNome != null ? orientadorNome : "(orientador não definido)");
                } else {
                    lblPeerName.setText("(sem TG ativo)");
                    throw new IllegalStateException("Nenhum TG encontrado para o aluno " + alunoId);
                }
            }
        }
    }

    private void loadHistorico() throws SQLException {
        chatMessages.getChildren().clear();
        List<MensagemRow> mensagens = listarHistorico(trabalhoId);
        for (MensagemRow m : mensagens) {
            if (m.remetenteId != null && m.remetenteId.equals(alunoId)) appendSelf(m.conteudo);
            else appendPeer(m.conteudo);
            lastCreatedAt = m.createdAt; // mantém o último timestamp
        }
        autoScroll();
        if (lastCreatedAt == null) lastCreatedAt = LocalDateTime.now().minusSeconds(1);
    }

    private void startPolling() {
        if (poller != null) poller.stop();
        if (lastCreatedAt == null) lastCreatedAt = LocalDateTime.now().minusSeconds(1);
        poller = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
            try {
                List<MensagemRow> novas = listarNovas(trabalhoId, lastCreatedAt);
                if (!novas.isEmpty()) {
                    boolean recebeuDoOrientador = false;
                    for (MensagemRow m : novas) {
                        if (m.remetenteId != null && m.remetenteId.equals(alunoId)) appendSelf(m.conteudo);
                        else { appendPeer(m.conteudo); recebeuDoOrientador = true; }
                        lastCreatedAt = m.createdAt != null ? m.createdAt : lastCreatedAt;
                    }
                    autoScroll();
                    if (recebeuDoOrientador) {
                        try { marcarComoLidas(); } catch (SQLException ignore) {}
                    }
                }
            } catch (SQLException e) {
                showError("Falha ao atualizar mensagens", e);
            }
        }));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    // ============ Envio ============

    public void enviarMensagem() {
        var text = txtMessage.getText();
        if (text == null || text.isBlank()) return;

        // Cinto de segurança: garante contexto carregado
        if (!ready || alunoId == null || trabalhoId == null || orientadorId == null) {
            showError("Inbox", new IllegalStateException(
                    "Contexto não carregado (alunoId/trabalhoId/orientadorId nulos). Abra a tela via SceneManager.go(..., init)."));
            return;
        }

        String conteudo = text.trim();
        try {
            inserirMensagemTexto(trabalhoId, alunoId, orientadorId, conteudo);
            appendSelf(conteudo);                    // feedback imediato
            lastCreatedAt = LocalDateTime.now();     // move o cursor de tempo
            txtMessage.clear();
            autoScroll();
        } catch (SQLException e) {
            showError("Falha ao enviar mensagem", e);
        }
    }

    // ============ Persistência mínima (JDBC direto, MVP) ============

    private static final class MensagemRow {
        Long id, remetenteId, destinatarioId;
        String conteudo;
        LocalDateTime createdAt;
    }

    private List<MensagemRow> listarHistorico(Long trabalhoId) throws SQLException {
        String sql = """
            SELECT id, remetente_id, destinatario_id, conteudo, created_at
            FROM mensagens
            WHERE trabalho_id = ?
            ORDER BY created_at ASC, id ASC
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MensagemRow> out = new ArrayList<>();
                while (rs.next()) {
                    MensagemRow m = new MensagemRow();
                    m.id = rs.getLong("id");
                    m.remetenteId = rs.getLong("remetente_id");
                    m.destinatarioId = rs.getLong("destinatario_id");
                    m.conteudo = rs.getString("conteudo");
                    Timestamp ts = rs.getTimestamp("created_at");
                    m.createdAt = ts != null ? ts.toLocalDateTime() : null;
                    out.add(m);
                }
                return out;
            }
        }
    }

    private List<MensagemRow> listarNovas(Long trabalhoId, LocalDateTime after) throws SQLException {
        String sql = """
            SELECT id, remetente_id, destinatario_id, conteudo, created_at
            FROM mensagens
            WHERE trabalho_id = ?
              AND created_at > ?
            ORDER BY created_at ASC, id ASC
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setTimestamp(2, Timestamp.valueOf(after));
            try (ResultSet rs = ps.executeQuery()) {
                List<MensagemRow> out = new ArrayList<>();
                while (rs.next()) {
                    MensagemRow m = new MensagemRow();
                    m.id = rs.getLong("id");
                    m.remetenteId = rs.getLong("remetente_id");
                    m.destinatarioId = rs.getLong("destinatario_id");
                    m.conteudo = rs.getString("conteudo");
                    Timestamp ts = rs.getTimestamp("created_at");
                    m.createdAt = ts != null ? ts.toLocalDateTime() : null;
                    out.add(m);
                }
                return out;
            }
        }
    }

    private Long inserirMensagemTexto(Long trabalhoId, Long remetenteId, Long destinatarioId, String conteudo) throws SQLException {
        String sql = """
            INSERT INTO mensagens (trabalho_id, remetente_id, destinatario_id, tipo, conteudo)
            VALUES ( ?, ?, ?, 'TEXTO', ? )
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, trabalhoId);
            ps.setLong(2, remetenteId);
            ps.setLong(3, destinatarioId);
            ps.setString(4, conteudo);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }

    private void marcarComoLidas() throws SQLException {
        String sql = """
            UPDATE mensagens
               SET lida = TRUE
             WHERE trabalho_id = ?
               AND destinatario_id = ?
               AND lida = FALSE
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setLong(2, alunoId);
            ps.executeUpdate();
        }
    }

    // ============ UI helpers ============

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
        // garante rolagem após render
        Platform.runLater(() -> {
            chatScroll.layout();
            chatScroll.setVvalue(1.0);
        });
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, title + "\n" + e.getMessage()).showAndWait());
    }
}
