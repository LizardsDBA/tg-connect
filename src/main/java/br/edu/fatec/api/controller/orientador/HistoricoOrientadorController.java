package br.edu.fatec.api.controller.orientador;

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
import java.util.List;

public class HistoricoOrientadorController extends BaseController {

    @FXML private TableView<AlunoTableItem> tblAlunos;
    @FXML private TableColumn<AlunoTableItem, String> colNome;
    @FXML private TextField txtBuscaAluno;
    
    @FXML private ListView<VersaoHistoricoDTO> listVersions;
    @FXML private TextArea txtMarkdownSource;
    @FXML private WebView webPreview;
    @FXML private Label lblAlunoSelecionado;
    @FXML private Label lblVersaoSelecionada;
    @FXML private Label lblDataEnvio;
    @FXML private TextArea txtFeedback;
    @FXML private VBox feedbackContainer;
    @FXML private VBox previewContainer;

    private final JdbcFeedbackDao feedbackDao = new JdbcFeedbackDao();
    private final JdbcVersoesTrabalhoDao versoesDao = new JdbcVersoesTrabalhoDao();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private final ObservableList<AlunoTableItem> alunos = FXCollections.observableArrayList();
    private Long orientadorId;
    private Long alunoSelecionadoId;
    private Long trabalhoIdSelecionado;

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
        
        // Exibir markdown
        txtMarkdownSource.setText(versao.conteudoMd());
        
        // Renderizar preview
        renderMarkdown(versao.conteudoMd());
        
        // Exibir feedback (se houver)
        if (versao.comentario() != null && !versao.comentario().trim().isEmpty()) {
            feedbackContainer.setVisible(true);
            feedbackContainer.setManaged(true);
            txtFeedback.setText(versao.comentario());
        } else {
            feedbackContainer.setVisible(false);
            feedbackContainer.setManaged(false);
        }
    }

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
                            line-height: 1.6;
                            padding: 20px;
                            color: #333;
                            background: #fff;
                            max-width: 100%%;
                            margin: 0 auto;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            margin-top: 24px;
                            margin-bottom: 16px;
                            font-weight: 600;
                            line-height: 1.25;
                        }
                        h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                        h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                        h3 { font-size: 1.25em; }
                        p { margin-bottom: 16px; }
                        ul, ol { padding-left: 2em; margin-bottom: 16px; }
                        code {
                            background-color: #f6f8fa;
                            border-radius: 3px;
                            padding: 2px 4px;
                            font-family: 'Courier New', Courier, monospace;
                            font-size: 85%%;
                        }
                        pre {
                            background-color: #f6f8fa;
                            border-radius: 3px;
                            padding: 16px;
                            overflow: auto;
                            margin-bottom: 16px;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin-bottom: 16px;
                        }
                        table th, table td {
                            border: 1px solid #dfe2e5;
                            padding: 6px 13px;
                        }
                        table th {
                            background-color: #f6f8fa;
                            font-weight: 600;
                        }
                        strong { font-weight: 600; }
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

    // Navegação
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
