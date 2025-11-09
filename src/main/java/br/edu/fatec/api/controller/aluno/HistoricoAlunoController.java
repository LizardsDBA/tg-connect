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

    @FXML
    private void initialize() {
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
        WebEngine engine = webPreview.getEngine();
        
        String escapedMarkdown = escapeForJavaScript(markdown);
        
        // HTML template com estilização similar ao markdownlivepreview.com
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
                        h4 { font-size: 1em; }
                        h5 { font-size: 0.875em; }
                        h6 { font-size: 0.85em; color: #6a737d; }
                        p { margin-bottom: 16px; }
                        ul, ol { padding-left: 2em; margin-bottom: 16px; }
                        li { margin-bottom: 4px; }
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
                        pre code {
                            background: none;
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
                        table tr:nth-child(2n) {
                            background-color: #f6f8fa;
                        }
                        hr {
                            border: 0;
                            border-top: 1px solid #eaecef;
                            margin: 24px 0;
                        }
                        a {
                            color: #0366d6;
                            text-decoration: none;
                        }
                        a:hover {
                            text-decoration: underline;
                        }
                        strong { font-weight: 600; }
                        em { font-style: italic; }
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
