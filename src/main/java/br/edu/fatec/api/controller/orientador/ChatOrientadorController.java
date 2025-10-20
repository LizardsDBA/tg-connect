package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller para o Chat do Orientador - Comunicação com alunos
 *
 * HISTÓRICO DE ALTERAÇÕES:
 * - Versão original: Mock com dados estáticos
 * - 18/10/2025 (ALTERADO POR MATHEUS): Implementação completa do chat real
 *   - Integração com banco de dados MySQL
 *   - Sistema de polling a cada 2 segundos
 *   - Lista dinâmica de conversas com badges
 *   - Marcação automática de lidas
 *   - Suporte para TableView e ListView (fallback)
 *
 * @author Matheus
 * @date 18/10/2025
 */
public class ChatOrientadorController {

    @FXML private Button btnChat;

    // COMPONENTES FXML
    // Suporte para TableView
    @FXML private TableView<ConversaVM> tblConversas;
    @FXML private TableColumn<ConversaVM, String> colAluno;
    @FXML private TableColumn<ConversaVM, String> colBadge;

    // Suporte para ListView
    @FXML private ListView<String> lstAlunos;

    // Chat
    @FXML private Label lblChatHeader;
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatMessages;
    @FXML private TextField txtMessage;

    // Alias para compatibilidade
    private Label lblAlunoNome;

    // ESTADO DA APLICAÇÃO
    private Long orientadorId;
    private Long trabalhoIdAtual;
    private Long alunoIdAtual;
    private LocalDateTime lastCreatedAt;
    private Timeline poller;
    private boolean ready = false;

    private ObservableList<ConversaVM> conversas = FXCollections.observableArrayList();

    //CICLO DE VID

    /**
     * Inicialização do controller
     * ORIGINAL: Setup básico de sidebar
     * (ALTERADO POR MATHEUS): Suporte para TableView e ListView
     */
    @FXML
    private void initialize() {

        if (btnChat != null && !btnChat.getStyleClass().contains("active")) {
            btnChat.getStyleClass().add("active");
        }

        //Alias para compatibilidade
        lblAlunoNome = lblChatHeader;

        //Configuração TableView (se existir no FXML)
        if (tblConversas != null) {
            if (colAluno != null) colAluno.setCellValueFactory(d -> d.getValue().alunoNome);
            if (colBadge != null) colBadge.setCellValueFactory(d -> d.getValue().badge);

            tblConversas.setItems(conversas);
            tblConversas.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
                if (novo != null) selecionarConversa(novo);
            });
        }
        //Fallback para ListView
        else if (lstAlunos != null) {
            lstAlunos.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
                if (novo != null) selecionarPorNome(novo);
            });
        }
    }

    /**
     * Carrega dados após initialize
     */
    public void onReady() {
        ready = false;
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }
        this.orientadorId = u.getId();

        try {
            carregarListaConversas();

            //Preenche TableView ou ListView
            if (tblConversas != null && !conversas.isEmpty()) {
                tblConversas.getSelectionModel().selectFirst();
            } else if (lstAlunos != null && !conversas.isEmpty()) {
                //converte ConversaVM para String para ListView
                ObservableList<String> nomes = FXCollections.observableArrayList();
                for (ConversaVM c : conversas) {
                    String badge = c.badge.get().isEmpty() ? "" : " (" + c.badge.get() + " não lidas)";
                    nomes.add(c.alunoNome.get() + badge);
                }
                lstAlunos.setItems(nomes);
                lstAlunos.getSelectionModel().selectFirst();
            } else {
                if (lblAlunoNome != null) lblAlunoNome.setText("Nenhuma conversa disponível");
            }

            startPolling();
            ready = true;
        } catch (Exception e) {
            showError("Falha ao carregar conversas", e);
        }
    }

    /**
     * Para o polling ao sair da tela
     */
    public void stopPolling() {
        if (poller != null) poller.stop();
    }

    /**
     * Carrega lista de conversas do orientador
     * (ADICIONADO POR MATHEUS)
     */
    private void carregarListaConversas() throws SQLException {
        conversas.clear();
        String sql = """
            SELECT
              t.id AS trabalho_id,
              a.id AS aluno_id,
              a.nome AS aluno_nome,
              COALESCE(SUM(CASE WHEN m.destinatario_id = ? AND m.lida = FALSE THEN 1 ELSE 0 END), 0) AS nao_lidas,
              MAX(m.created_at) AS ultima_msg_em
            FROM trabalhos_graduacao t
            JOIN usuarios a ON a.id = t.aluno_id
            LEFT JOIN mensagens m ON m.trabalho_id = t.id
            WHERE t.orientador_id = ?
            GROUP BY t.id, a.id, a.nome
            ORDER BY (ultima_msg_em IS NULL), ultima_msg_em DESC, aluno_nome ASC
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orientadorId);
            ps.setLong(2, orientadorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long tid = rs.getLong("trabalho_id");
                    long aid = rs.getLong("aluno_id");
                    String nome = rs.getString("aluno_nome");
                    int naoLidas = rs.getInt("nao_lidas");
                    conversas.add(new ConversaVM(tid, aid, nome, naoLidas));
                }
            }
        }
    }

    /**
     * Ao selecionar conversa (para TableView
     */
    private void selecionarConversa(ConversaVM conv) {
        this.trabalhoIdAtual = conv.trabalhoId;
        this.alunoIdAtual = conv.alunoId;
        if (lblAlunoNome != null) lblAlunoNome.setText(conv.alunoNome.get());

        try {
            loadHistorico();
            marcarComoLidas();
            atualizarBadge(conv);
        } catch (Exception e) {
            showError("Falha ao carregar conversa", e);
        }
    }

    /**
     * Ao selecionar por nome (fallback para ListView)
     */
    private void selecionarPorNome(String nomeDisplay) {
        for (ConversaVM conv : conversas) {
            if (nomeDisplay.contains(conv.alunoNome.get())) {
                selecionarConversa(conv);
                break;
            }
        }
    }

    /**
     * Carrega histórico completo da convers
     */
    private void loadHistorico() throws SQLException {
        chatMessages.getChildren().clear();
        List<MensagemRow> msgs = listarHistorico(trabalhoIdAtual);
        for (MensagemRow m : msgs) {
            if (m.remetenteId != null && m.remetenteId.equals(orientadorId)) {
                appendSelf(m.conteudo);
            } else {
                appendPeer(m.conteudo);
            }
            lastCreatedAt = m.createdAt;
        }
        autoScroll();
        if (lastCreatedAt == null) lastCreatedAt = LocalDateTime.now().minusSeconds(1);
    }

    // POLLING

    /**
     * Inicia polling a cada 2 segundos
     */
    private void startPolling() {
        if (poller != null) poller.stop();
        if (lastCreatedAt == null) lastCreatedAt = LocalDateTime.now().minusSeconds(1);

        poller = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
            if (trabalhoIdAtual == null) return;
            try {
                List<MensagemRow> novas = listarNovas(trabalhoIdAtual, lastCreatedAt);
                if (!novas.isEmpty()) {
                    boolean recebeuDoAluno = false;
                    for (MensagemRow m : novas) {
                        if (m.remetenteId != null && m.remetenteId.equals(orientadorId)) {
                            appendSelf(m.conteudo);
                        } else {
                            appendPeer(m.conteudo);
                            recebeuDoAluno = true;
                        }
                        lastCreatedAt = m.createdAt != null ? m.createdAt : lastCreatedAt;
                    }
                    autoScroll();
                    if (recebeuDoAluno) {
                        marcarComoLidas();
                        // Atualiza badge
                        if (tblConversas != null) {
                            ConversaVM atual = tblConversas.getSelectionModel().getSelectedItem();
                            if (atual != null) atualizarBadge(atual);
                        } else if (lstAlunos != null) {
                            // Recarrega lista (fallback)
                            try {
                                carregarListaConversas();
                                ObservableList<String> nomes = FXCollections.observableArrayList();
                                for (ConversaVM c : conversas) {
                                    String badge = c.badge.get().isEmpty() ? "" : " (" + c.badge.get() + ")";
                                    nomes.add(c.alunoNome.get() + badge);
                                }
                                Platform.runLater(() -> lstAlunos.setItems(nomes));
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    //ENVIO DE MENSAGEM

    /**
     * Handler do botão Enviar
     * Mock apenas visual
     * Agora persiste no banco de dados
     */
    public void enviarMensagem() {
        String text = txtMessage.getText();
        if (text == null || text.isBlank()) return;
        if (!ready || orientadorId == null || trabalhoIdAtual == null || alunoIdAtual == null) {
            showError("Inbox", new IllegalStateException("Contexto não carregado. Aguarde o carregamento completo."));
            return;
        }

        String conteudo = text.trim();
        try {
            inserirMensagemTexto(trabalhoIdAtual, orientadorId, alunoIdAtual, conteudo);
            appendSelf(conteudo);
            lastCreatedAt = LocalDateTime.now();
            txtMessage.clear();
            autoScroll();
        } catch (SQLException e) {
            showError("Falha ao enviar mensagem", e);
        }
    }

    //PERSISTÊNCIA - JDBC

    /**
     * DTO interno para mensagen
     */
    private static class MensagemRow {
        Long id, remetenteId, destinatarioId;
        String conteudo;
        LocalDateTime createdAt;
    }

    /**
     * Busca histórico complet
     */
    private List<MensagemRow> listarHistorico(Long tid) throws SQLException {
        String sql = """
            SELECT id, remetente_id, destinatario_id, conteudo, created_at
            FROM mensagens
            WHERE trabalho_id = ?
            ORDER BY created_at ASC, id ASC
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tid);
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

    /**
     * Busca mensagens novas
     */
    private List<MensagemRow> listarNovas(Long tid, LocalDateTime after) throws SQLException {
        String sql = """
            SELECT id, remetente_id, destinatario_id, conteudo, created_at
            FROM mensagens
            WHERE trabalho_id = ?
              AND created_at > ?
            ORDER BY created_at ASC, id ASC
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tid);
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

    /**
     * Insere mensagem no banco
     */
    private Long inserirMensagemTexto(Long tid, Long remetenteId, Long destinatarioId, String conteudo) throws SQLException {
        String sql = """
            INSERT INTO mensagens (trabalho_id, remetente_id, destinatario_id, tipo, conteudo)
            VALUES (?, ?, ?, 'TEXTO', ?)
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, tid);
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

    /**
     * Marca mensagens como lidas
     */
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
            ps.setLong(1, trabalhoIdAtual);
            ps.setLong(2, orientadorId);
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza badge de não lida
     */
    private void atualizarBadge(ConversaVM conv) {
        try {
            String sql = """
                SELECT COUNT(*) AS nao_lidas
                FROM mensagens
                WHERE trabalho_id = ?
                  AND destinatario_id = ?
                  AND lida = FALSE
                """;
            try (Connection c = Database.get();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, conv.trabalhoId);
                ps.setLong(2, orientadorId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int nao = rs.getInt("nao_lidas");
                        Platform.runLater(() -> conv.setBadge(nao));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========== UI HELPERS ==========

    /**
     * ORIGINAL: buildBubble e append methods
     */
    private void appendSelf(String msg) { chatMessages.getChildren().add(buildBubble(msg, true)); }
    private void appendPeer(String msg) { chatMessages.getChildren().add(buildBubble(msg, false)); }

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

    /**
     * Exibe erro em Alert
     */
    private void showError(String title, Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, title + "\n" + e.getMessage()).showAndWait());
    }

    //VIEW MODEL

    /**
     * ViewModel para conversas
     */
    public static class ConversaVM {
        final long trabalhoId;
        final long alunoId;
        final SimpleStringProperty alunoNome = new SimpleStringProperty();
        final SimpleStringProperty badge = new SimpleStringProperty();

        public ConversaVM(long tid, long aid, String nome, int naoLidas) {
            this.trabalhoId = tid;
            this.alunoId = aid;
            this.alunoNome.set(nome);
            setBadge(naoLidas);
        }

        public void setBadge(int n) {
            badge.set(n > 0 ? String.valueOf(n) : "");
        }
    }

    // NAVEGAÇÃO

    public void goHome() {
        stopPolling();
        SceneManager.go("orientador/VisaoGeral.fxml");
    }

    public void logout() {
        stopPolling();
        SceneManager.go("login/Login.fxml");
    }

    public void goVisaoGeral() {
        stopPolling();
        SceneManager.go("orientador/VisaoGeral.fxml");
    }

    public void goPainel() {
        stopPolling();
        SceneManager.go("orientador/Painel.fxml");
    }

    public void goNotificacoes() {
        stopPolling();
        SceneManager.go("orientador/Notificacoes.fxml");
    }

    public void goEditor() {
        stopPolling();
        SceneManager.go("orientador/Editor.fxml");
    }

    public void goParecer() {
        stopPolling();
        SceneManager.go("orientador/Parecer.fxml");
    }

    public void goImportar() {
        stopPolling();
        SceneManager.go("orientador/Importar.fxml");
    }

    /**
     *  Adicionado callback onReady()
     */
    public void goChat() {
        stopPolling();
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }
}