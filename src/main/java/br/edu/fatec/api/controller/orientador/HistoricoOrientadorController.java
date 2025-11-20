package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dto.HistoricoItemDTO;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.Mensagem;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.service.HistoricoService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

public class HistoricoOrientadorController extends BaseController {

    @FXML private Button btnSouCoordenador;
    @FXML private Button btnToggleSidebar;
    private Long professorId;

    // --- CAMPOS FXML ---
    @FXML private TableView<AlunoTableItem> tblAlunos;
    @FXML private TableColumn<AlunoTableItem, String> colNome;
    @FXML private TextField txtBuscaAluno;
    @FXML private ListView<HistoricoItemDTO> listVersions;
    @FXML private TextArea txtMarkdownSource;
    @FXML private WebView webPreview;
    @FXML private Label lblAlunoSelecionado;
    @FXML private Label lblVersaoSelecionada;
    @FXML private Label lblDataEnvio;
    @FXML private TextArea txtFeedback;
    @FXML private VBox feedbackContainer;
    @FXML private VBox previewContainer;
    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private Button btnFiltrar;

    // --- DAOs E SERVIÇOS ---
    private final JdbcFeedbackDao feedbackDao = new JdbcFeedbackDao();
    private final HistoricoService historicoService = new HistoricoService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // --- VARIÁVEIS DE ESTADO ---
    private final ObservableList<AlunoTableItem> alunos = FXCollections.observableArrayList();
    private Long orientadorId;
    private AlunoTableItem alunoSelecionado; // Salva o aluno, não só o ID

    /**
     * Método principal, executado quando a tela é carregada UMA VEZ.
     */
    @FXML
    private void initialize() {

        initUserAndLoad();

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
        listVersions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HistoricoItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String data = item.data().format(dateFormatter);
                    String desc = item.descricao();
                    String descCurta = desc.length() > 60 ? desc.substring(0, 60) + "..." : desc;

                    String texto = String.format("[%s] %s (%s)",
                            item.tipo(),
                            descCurta.replace("\n", " "),
                            data);
                    setText(texto);

                    if (item.tipo() == HistoricoItemDTO.TipoHistorico.VERSAO) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });

        // Listener da lista
        listVersions.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) return;

            if (selected.tipo() == HistoricoItemDTO.TipoHistorico.VERSAO) {
                VersaoHistoricoDTO versao = (VersaoHistoricoDTO) selected.payload();
                exibirVersao(versao);
            } else {
                Mensagem msg = (Mensagem) selected.payload();
                exibirFeedback(msg);
            }
        });

        // Ação do botão de filtro
        btnFiltrar.setOnAction(e -> carregarHistoricoAluno());

        // Carrega os dados na primeira vez que a tela abre
        onRefreshData();
    }

    private void initUserAndLoad() {
        User user = Session.getUser();
        if (user != null) {
            this.professorId = user.getId();
            carregarOrientandos();
            boolean isCoord = (user.getRole() == Role.COORDENADOR);
            if (btnSouCoordenador != null) {
                btnSouCoordenador.setVisible(isCoord);
                btnSouCoordenador.setManaged(isCoord);
            }
        }
        if (btnToggleSidebar != null) btnToggleSidebar.setText("☰");
    }

    /**
     * NOVO MÉTODO PÚBLICO
     * Esta é a função que será chamada por outros controllers para
     * garantir que os dados sejam sempre os mais recentes.
     */
    public void onRefreshData() {
        System.out.println("HistoricoOrientadorController: Atualizando dados...");
        // Recarrega a lista de alunos
        carregarOrientandos();

        // Se um aluno já estava selecionado, recarrega o histórico dele
        if (alunoSelecionado != null) {
            // Re-seleciona o aluno na tabela (caso a lista tenha mudado)
            tblAlunos.getSelectionModel().select(alunoSelecionado);
            // Recarrega o histórico
            carregarHistoricoAluno();
        } else {
            // Se nenhum aluno estava selecionado, limpa o histórico
            listVersions.getItems().clear();
            previewContainer.setVisible(false);
            previewContainer.setManaged(false);
        }
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
            showAlert("Erro", "Erro ao carregar orientandos", e.getMessage());
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
        this.alunoSelecionado = item; // Salva o objeto AlunoTableItem

        lblAlunoSelecionado.setText(item.getNome());

        // Mostrar container de preview
        previewContainer.setVisible(true);
        previewContainer.setManaged(true);

        // Limpar seleções e filtros anteriores
        listVersions.getItems().clear();
        txtMarkdownSource.clear();
        webPreview.getEngine().loadContent("");
        feedbackContainer.setVisible(false);
        feedbackContainer.setManaged(false);
        dpDataInicio.setValue(null);
        dpDataFim.setValue(null);

        // Carregar histórico do aluno
        carregarHistoricoAluno();
    }

    /**
     * Carrega a lista unificada (Versões + Feedbacks) e aplica o filtro de data.
     */
    private void carregarHistoricoAluno() {
        if (alunoSelecionado == null || alunoSelecionado.getTrabalhoId() == null) {
            listVersions.getItems().clear();
            return;
        }

        LocalDate inicio = dpDataInicio.getValue();
        LocalDate fim = dpDataFim.getValue();

        try {
            List<HistoricoItemDTO> historicoCompleto = historicoService.getHistoricoUnificado(alunoSelecionado.getTrabalhoId());

            Stream<HistoricoItemDTO> stream = historicoCompleto.stream();
            if (inicio != null) {
                stream = stream.filter(item -> !item.data().toLocalDate().isBefore(inicio));
            }
            if (fim != null) {
                stream = stream.filter(item -> !item.data().toLocalDate().isAfter(fim));
            }
            List<HistoricoItemDTO> historicoFiltrado = stream.toList();

            if (historicoFiltrado.isEmpty()) {
                String msg = (inicio != null || fim != null)
                        ? "Nenhum item encontrado para este filtro de data."
                        : "Este aluno ainda não possui versões ou feedbacks.";

                showAlert("Histórico Vazio", null, msg);
                listVersions.getItems().clear();
                return;
            }

            listVersions.getItems().setAll(historicoFiltrado);

            if (!historicoFiltrado.isEmpty()) {
                listVersions.getSelectionModel().selectLast();
            }

        } catch (SQLException e) {
            showAlert("Erro", "Erro ao carregar histórico unificado", e.getMessage());
        }
    }

    // --- Métodos de Exibição (sem alteração) ---
    private void exibirVersao(VersaoHistoricoDTO versao) {
        lblVersaoSelecionada.setText(versao.versao());
        lblDataEnvio.setText(versao.createdAt().format(dateFormatter));
        txtMarkdownSource.setText(versao.conteudoMd());
        renderMarkdown(versao.conteudoMd());
        txtMarkdownSource.setVisible(true);
        txtMarkdownSource.setManaged(true);
        webPreview.setVisible(true);
        webPreview.setManaged(true);

        if (versao.comentario() != null && !versao.comentario().trim().isEmpty()) {
            feedbackContainer.setVisible(true);
            feedbackContainer.setManaged(true);
            txtFeedback.setText("Comentário da Submissão: " + versao.comentario());
        } else {
            feedbackContainer.setVisible(false);
            feedbackContainer.setManaged(false);
        }
    }

    private void exibirFeedback(Mensagem msg) {
        lblVersaoSelecionada.setText("Feedback (Chat)");
        lblDataEnvio.setText(msg.getCreatedAt().format(dateFormatter));
        txtMarkdownSource.setText("");
        txtMarkdownSource.setVisible(false);
        txtMarkdownSource.setManaged(false);
        webPreview.setVisible(false);
        webPreview.setManaged(false);
        renderMarkdown("### Este item é um feedback (mensagem de chat)\n\nNão há preview de TG associado.");
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
        txtFeedback.setText(msg.getConteudo());
    }

    // --- Métodos de Renderização e Helpers (sem alteração) ---
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

    private String escapeForJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- NAVEGAÇÃO (Sidebar) ---
    public void goHomeCoord(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goHome() { SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout() { SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral() { SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel() { SceneManager.go("orientador/Painel.fxml"); }
    public void goEditor() { SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer() { SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar() { SceneManager.go("orientador/Importar.fxml"); }

    public void goHistorico() {
        // Já estamos aqui, então apenas atualizamos os dados
        onRefreshData();
    }

    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    // --- Classe Interna (sem alteração) ---
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

    public void goSolicitacoes() {
        SceneManager.go("orientador/SolicitacoesOrientador.fxml");
    }
}