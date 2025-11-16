package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dto.HistoricoItemDTO; // <-- DTO NOVO
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.Mensagem; // <-- MODEL PARA FEEDBACK
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.service.HistoricoService; // <-- SERVIÇO NOVO
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoricoOrientadorController extends BaseController {

    // --- CAMPOS FXML (DA SUA TELA) ---
    @FXML private TableView<AlunoTableItem> tblAlunos;
    @FXML private TableColumn<AlunoTableItem, String> colNome;
    @FXML private TextField txtBuscaAluno;

    // ATUALIZADO: O tipo da ListView agora é o nosso DTO unificado
    @FXML private ListView<HistoricoItemDTO> listVersions;

    @FXML private TextArea txtMarkdownSource;
    @FXML private WebView webPreview;
    @FXML private Label lblAlunoSelecionado;
    @FXML private Label lblVersaoSelecionada;
    @FXML private Label lblDataEnvio;
    @FXML private TextArea txtFeedback;
    @FXML private VBox feedbackContainer;
    @FXML private VBox previewContainer;

    // --- DAOs E SERVIÇOS ---
    private final JdbcFeedbackDao feedbackDao = new JdbcFeedbackDao();

    // NOVO: Instancia o serviço que criamos
    private final HistoricoService historicoService = new HistoricoService();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // --- VARIÁVEIS DE ESTADO ---
    private final ObservableList<AlunoTableItem> alunos = FXCollections.observableArrayList();
    private Long orientadorId;
    private Long alunoSelecionadoId;
    private Long trabalhoIdSelecionado;

    /**
     * Método principal, executado quando a tela é carregada.
     */
    @FXML
    private void initialize() {
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Configurar sessão
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }
        this.orientadorId = u.getId();

        // Ocultar preview inicialmente
        previewContainer.setVisible(false);
        previewContainer.setManaged(false);

        // --- CONFIGURAÇÃO DA TABELA DE ALUNOS ---
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tblAlunos.setItems(alunos);

        // Duplo clique para selecionar aluno
        tblAlunos.setRowFactory(tv -> {
            TableRow<AlunoTableItem> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty()) {
                    selecionarAluno(row.getItem());
                }
            });
            return row;
        });

        // Filtro de busca
        txtBuscaAluno.textProperty().addListener((obs, old, val) -> filtrarAlunos(val));

        // --- CONFIGURAÇÃO DA LISTA DE HISTÓRICO (ListView) ---
        // ATUALIZADO: Configura a ListView para o DTO unificado
        listVersions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HistoricoItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(""); // Limpa o estilo
                } else {
                    // Formata a data e a descrição
                    String data = item.data().format(dateFormatter);
                    String desc = item.descricao();
                    String descCurta = desc.length() > 60 ? desc.substring(0, 60) + "..." : desc;

                    String texto = String.format("[%s] %s (%s)",
                            item.tipo(),
                            descCurta.replace("\n", " "), // remove quebra de linha
                            data);
                    setText(texto);

                    // Adiciona um estilo visual
                    if (item.tipo() == HistoricoItemDTO.TipoHistorico.VERSAO) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });

        // ATUALIZADO: O listener agora trata os dois tipos de item
        listVersions.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) return;

            if (selected.tipo() == HistoricoItemDTO.TipoHistorico.VERSAO) {
                // É uma Versão, extrai o DTO original e chama o método antigo
                VersaoHistoricoDTO versao = (VersaoHistoricoDTO) selected.payload();
                exibirVersao(versao);
            } else {
                // É um Feedback (Mensagem), chama o novo método
                Mensagem msg = (Mensagem) selected.payload();
                exibirFeedback(msg);
            }
        });

        // Carregar lista de alunos
        carregarOrientandos();
    }

    /**
     * Carrega a lista de alunos do orientador na tabela da esquerda.
     */
    private void carregarOrientandos() {
        alunos.clear();
        if (orientadorId == null) return;

        try {
            var lista = feedbackDao.listarOrientandos(orientadorId).stream()
                    .map(dto -> new AlunoTableItem(dto.alunoId(), dto.nome(), dto.trabalhoId()))
                    .toList();
            alunos.addAll(lista);
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao carregar orientandos");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Filtra a tabela de alunos com base no texto digitado.
     */
    private void filtrarAlunos(String query) {
        if (query == null || query.trim().isEmpty()) {
            tblAlunos.setItems(alunos);
            return;
        }

        String q = query.trim().toLowerCase();
        var filtered = alunos.filtered(item ->
                item.getNome().toLowerCase().contains(q)
        );
        tblAlunos.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * Chamado ao dar duplo clique em um aluno na tabela.
     */
    private void selecionarAluno(AlunoTableItem item) {
        this.alunoSelecionadoId = item.getAlunoId();
        this.trabalhoIdSelecionado = item.getTrabalhoId();

        lblAlunoSelecionado.setText(item.getNome());

        // Mostrar container de preview
        previewContainer.setVisible(true);
        previewContainer.setManaged(true);

        // Limpar seleções anteriores
        listVersions.getItems().clear();
        txtMarkdownSource.clear();
        webPreview.getEngine().loadContent("");
        feedbackContainer.setVisible(false);
        feedbackContainer.setManaged(false);

        // Carregar histórico do aluno
        carregarHistoricoAluno();
    }

    /**
     * ATUALIZADO: Carrega a lista unificada (Versões + Feedbacks) do Service.
     */
    private void carregarHistoricoAluno() {
        if (trabalhoIdSelecionado == null) return;

        try {
            // USA O NOVO SERVIÇO para buscar a lista unificada
            List<HistoricoItemDTO> historico = historicoService.getHistoricoUnificado(trabalhoIdSelecionado);

            if (historico.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Histórico Vazio");
                alert.setHeaderText(null);
                alert.setContentText("Este aluno ainda não possui versões ou feedbacks.");
                alert.showAndWait();
                listVersions.getItems().clear(); // Limpa a lista
                return;
            }

            listVersions.getItems().setAll(historico);

            // Selecionar o último item (mais recente)
            if (!historico.isEmpty()) {
                listVersions.getSelectionModel().selectLast();
            }

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao carregar histórico unificado");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Exibe os detalhes de um item do tipo VERSAO.
     */
    private void exibirVersao(VersaoHistoricoDTO versao) {
        // Atualizar informações
        lblVersaoSelecionada.setText(versao.versao());
        lblDataEnvio.setText(versao.createdAt().format(dateFormatter));

        // Exibir markdown e preview
        txtMarkdownSource.setText(versao.conteudoMd());
        renderMarkdown(versao.conteudoMd());

        // Garante que os painéis de preview estão visíveis
        txtMarkdownSource.setVisible(true);
        txtMarkdownSource.setManaged(true);
        webPreview.setVisible(true);
        webPreview.setManaged(true);

        // Exibir feedback da versão (comentário de submissão)
        if (versao.comentario() != null && !versao.comentario().trim().isEmpty()) {
            feedbackContainer.setVisible(true);
            feedbackContainer.setManaged(true);
            txtFeedback.setText("Comentário da Submissão: " + versao.comentario());
        } else {
            feedbackContainer.setVisible(false);
            feedbackContainer.setManaged(false);
        }
    }

    /**
     * NOVO: Exibe os detalhes de um item do tipo FEEDBACK (Mensagem).
     */
    private void exibirFeedback(Mensagem msg) {
        // Atualizar informações
        lblVersaoSelecionada.setText("Feedback (Chat)");
        lblDataEnvio.setText(msg.getCreatedAt().format(dateFormatter));

        // Esconde os painéis de markdown e preview
        txtMarkdownSource.setText("");
        txtMarkdownSource.setVisible(false);
        txtMarkdownSource.setManaged(false);
        webPreview.setVisible(false);
        webPreview.setManaged(false);
        renderMarkdown("### Este item é um feedback (mensagem de chat)\n\nNão há preview de TG associado.");

        // Mostra o painel de feedback com o conteúdo da mensagem
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
        txtFeedback.setText(msg.getConteudo());
    }

    /**
     * Renderiza o texto Markdown no WebView.
     */
    private void renderMarkdown(String markdown) {
        WebEngine engine = webPreview.getEngine();
        String escapedMarkdown = escapeForJavaScript(markdown);

        String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                            line-height: 1.6; padding: 20px; color: #333; background: #fff;
                            max-width: 100%%; margin: 0 auto;
                        }
                        h1, h2, h3 { border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                        h1 { font-size: 2em; } h2 { font-size: 1.5em; } h3 { font-size: 1.25em; }
                        pre { background-color: #f6f8fa; border-radius: 3px; padding: 16px; overflow: auto; }
                        code { background-color: #f6f8fa; border-radius: 3px; padding: 2px 4px; font-family: 'Courier New', monospace; }
                        table { border-collapse: collapse; }
                        table th, table td { border: 1px solid #dfe2e5; padding: 6px 13px; }
                    </style>
                    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                </head>
                <body>
                    <div id="content"></div>
                    <script>
                        const markdown = `%s`;
                        document.getElementById('content').innerHTML = marked.parse(markdown);
                    </script>
                </body>
                </html>
                """, escapedMarkdown);

        engine.loadContent(html);
    }

    /**
     * Escapa caracteres especiais para inserir o Markdown dentro do script JS.
     */
    private String escapeForJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    // --- NAVEGAÇÃO (Sidebar) ---
    public void goHome() { SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout() { SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral() { SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel() { SceneManager.go("orientador/Painel.fxml"); }
    public void goEditor() { SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer() { SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar() { SceneManager.go("orientador/Importar.fxml"); }
    public void goHistorico() { SceneManager.go("orientador/Historico.fxml"); }

    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    /**
     * Classe interna (DTO) para popular a Tabela de Alunos.
     * (Esta é a classe que estava faltando e causando os 7 erros)
     */
    public static class AlunoTableItem {
        private final SimpleLongProperty alunoId;
        private final SimpleStringProperty nome;
        private final Long trabalhoId;

        public AlunoTableItem(Long alunoId, String nome, Long trabalhoId) {
            this.alunoId = new SimpleLongProperty(alunoId != null ? alunoId : 0L);
            this.nome = new SimpleStringProperty(nome != null ? nome : "—");
            this.trabalhoId = trabalhoId;
        }

        public long getAlunoId() { return alunoId.get(); }
        public String getNome() { return nome.get(); }
        public Long getTrabalhoId() { return trabalhoId; }
        public SimpleStringProperty nomeProperty() { return nome; }
    }
}