package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcVersoesTrabalhoDao;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

public class HistoricoAlunoController extends BaseController {

    @FXML private ListView<VersaoHistoricoDTO> listVersions;
    @FXML private TextArea txtMarkdownSource;
    @FXML private WebView webPreview;
    @FXML private Label lblVersaoSelecionada;
    @FXML private Label lblDataEnvio;
    @FXML private TextArea txtFeedback;
    @FXML private VBox feedbackContainer;

    private final JdbcVersoesTrabalhoDao dao = new JdbcVersoesTrabalhoDao();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

        // Configura sessão do aluno
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        // Configurar ListView com cell factory customizada
        listVersions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VersaoHistoricoDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
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

        // Carregar histórico
        carregarHistorico(u.getId());
    }

    private void carregarHistorico(Long alunoId) {
        try {
            Long trabalhoId = dao.obterTrabalhoIdPorAluno(alunoId);
            List<VersaoHistoricoDTO> versoes = dao.listarHistoricoCompleto(trabalhoId);
            
            if (versoes.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Histórico Vazio");
                alert.setHeaderText(null);
                alert.setContentText("Ainda não há versões do TG salvas.");
                alert.showAndWait();
                return;
            }
            
            listVersions.getItems().setAll(versoes);
            
            // Selecionar primeira versão (mais recente)
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
        // Atualizar informações da versão
        lblVersaoSelecionada.setText(versao.versao());
        lblDataEnvio.setText(versao.createdAt().format(dateFormatter));
        
        // Exibir markdown no lado esquerdo
        txtMarkdownSource.setText(versao.conteudoMd());
        
        // Renderizar markdown no WebView (lado direito)
        renderMarkdown(versao.conteudoMd());
        
        // Exibir feedback do orientador (se houver)
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
    public void goHome() { SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout() { SceneManager.go("login/Login.fxml"); }
    public void goDashboard() { SceneManager.go("aluno/Dashboard.fxml"); }
    
    public void goInbox() {
        SceneManager.go("aluno/Inbox.fxml", c -> {
            var ctrl = (InboxAlunoController) c;
            User u = Session.getUser();
            if (u == null) { SceneManager.go("login/Login.fxml"); return; }
            ctrl.setAlunoContext(u.getId());
            ctrl.onReady();
        });
    }
    
    public void goEditor() { SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar() { SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao() { SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico() { SceneManager.go("aluno/Historico.fxml"); }
}
