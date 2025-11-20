package br.edu.fatec.api.controller.coordenacao;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcVersoesTrabalhoDao;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
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
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.Collections;
import java.util.List;

public class HistoricoCoordController extends BaseController {

    @FXML private TableView<AlunoTableItem> tblAlunos;
    @FXML private TableColumn<AlunoTableItem, String> colNome;
    @FXML private TextField txtBuscaAluno;

    @FXML private ListView<VersaoHistoricoDTO> listVersions;
    @FXML private WebView webPreview;
    @FXML private Label lblAlunoSelecionado;
    @FXML private Label lblVersaoSelecionada;
    @FXML private Label lblDataEnvio;
    @FXML private VBox feedbackContainer;
    @FXML private VBox previewContainer;

    private final JdbcFeedbackDao feedbackDao = new JdbcFeedbackDao();
    private final JdbcVersoesTrabalhoDao versoesDao = new JdbcVersoesTrabalhoDao();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObservableList<AlunoTableItem> alunos = FXCollections.observableArrayList();
    private Long orientadorId;
    private Long alunoSelecionadoId;
    private Long trabalhoIdSelecionado;
    // Ferramentas do Flexmark (Java)
    private Parser parser;
    private HtmlRenderer renderer;

    @FXML
    private void initialize() {
        // --- CONFIGURAÇÃO FLEXMARK ---
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Collections.singletonList(TablesExtension.create()));

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
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

        // Configurar tabela de alunos
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

        // Configurar ListView de versões
        listVersions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VersaoHistoricoDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.versao() + " - " + item.createdAt().format(dateFormatter));
                }
            }
        });

        // Listener para seleção de versão
        listVersions.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                exibirVersao(selected);
            }
        });

        // Carregar lista de alunos
        carregarOrientandos();
    }

    private void carregarOrientandos() {
        alunos.clear();
        // Não precisamos mais do 'orientadorId' da sessão

        try {
            // Chama o NOVO método do versoesDao
            var lista = versoesDao.listarTodosAlunosParaHistorico().stream()
                    .map(dto -> new AlunoTableItem(dto.alunoId(), dto.nome(), dto.trabalhoId()))
                    .toList();
            alunos.addAll(lista);
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao carregar lista de alunos");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

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

    private void selecionarAluno(AlunoTableItem item) {
        this.alunoSelecionadoId = item.getAlunoId();
        this.trabalhoIdSelecionado = item.getTrabalhoId();

        lblAlunoSelecionado.setText(item.getNome());

        // Mostrar preview
        previewContainer.setVisible(true);
        previewContainer.setManaged(true);

        // Carregar histórico do aluno
        carregarHistoricoAluno();
    }

    private void carregarHistoricoAluno() {
        if (trabalhoIdSelecionado == null) return;

        try {
            List<VersaoHistoricoDTO> versoes = versoesDao.listarHistoricoCompleto(trabalhoIdSelecionado);

            if (versoes.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Histórico Vazio");
                alert.setHeaderText(null);
                alert.setContentText("Este aluno ainda não possui versões do TG salvas.");
                alert.showAndWait();
                return;
            }

            listVersions.getItems().setAll(versoes);

            // Selecionar primeira versão
            if (!versoes.isEmpty()) {
                listVersions.getSelectionModel().select(0);
            }

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao carregar histórico");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void exibirVersao(VersaoHistoricoDTO versao) {
        // Atualizar informações
        lblVersaoSelecionada.setText(versao.versao());
        lblDataEnvio.setText(versao.createdAt().format(dateFormatter));

        // Renderizar preview
        renderMarkdown(versao.conteudoMd());
    }

    private void renderMarkdown(String markdown) {
        if (webPreview == null) return;

        // 1. Converte Markdown para HTML usando Java (Flexmark)
        String md = (markdown == null) ? "" : markdown;
        String htmlBody = renderer.render(parser.parse(md));

        // 2. Monta o HTML final (CORRIGIDO: Note os '%%' no CSS)
        String htmlCompleto = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        font-size: 14px;
                        line-height: 1.6;
                        color: #24292e;
                        background-color: #ffffff;
                        padding: 20px;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: 600;
                        line-height: 1.25;
                        border-bottom: 1px solid #eaecef;
                        padding-bottom: 0.3em;
                    }
                    p { margin-bottom: 16px; }
                    code {
                        padding: 0.2em 0.4em;
                        margin: 0;
                        font-size: 85%%; /* Corrigido: %% */
                        background-color: #f6f8fa;
                        border-radius: 3px;
                        font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
                    }
                    pre {
                        padding: 16px;
                        overflow: auto;
                        font-size: 85%%; /* Corrigido: %% */
                        line-height: 1.45;
                        background-color: #f6f8fa;
                        border-radius: 3px;
                    }
                    pre code {
                        background-color: transparent;
                        padding: 0;
                    }
                    blockquote {
                        padding: 0 1em;
                        color: #6a737d;
                        border-left: 0.25em solid #dfe2e5;
                        margin: 0 0 16px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%%; /* Corrigido: %% */
                        margin-bottom: 16px;
                    }
                    table th, table td {
                        padding: 6px 13px;
                        border: 1px solid #dfe2e5;
                    }
                    table th {
                        font-weight: 600;
                        background-color: #f6f8fa;
                    }
                    tr:nth-child(2n) {
                        background-color: #f8f8f8;
                    }
                    ul, ol { padding-left: 2em; margin-bottom: 16px; }
                    hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """.formatted(htmlBody);

        // 3. Carrega no WebView
        webPreview.getEngine().loadContent(htmlCompleto);
    }

    // Navegação
    public void goHomeOrient(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goHome() { SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void logout() { SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral() { SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goMapa() { SceneManager.go("coordenacao/Mapa.fxml"); }
    public void goAndamento() { SceneManager.go("coordenacao/Andamento.fxml"); }
    public void goHistorico() { SceneManager.go("coordenacao/HistoricoCoord.fxml"); }


    // Classe interna para item da tabela
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
