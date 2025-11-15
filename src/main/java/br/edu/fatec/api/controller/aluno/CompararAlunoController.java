package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcVersoesTrabalhoDao;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CompararAlunoController extends BaseController {

    // --- Componentes FXML ---
    @FXML private WebView webA;
    @FXML private WebView webB;
    @FXML private Label lblVersaoA;
    @FXML private Label lblVersaoB;
    @FXML private ProgressBar similarityBar;
    @FXML private Label similarityPct;

    // --- DAOs e Serviços ---
    private final JdbcVersoesTrabalhoDao dao = new JdbcVersoesTrabalhoDao();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    private volatile boolean isSyncing = false;

    @FXML
    private void initialize() {
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }
        onReady();
    }

    public void onReady() {
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }
        loadVersions(u.getId());
    }

    /**
     * ATUALIZADO: Carrega a versão ATUAL (B) e a ÚLTIMA CORRIGIDA (A).
     */
    private void loadVersions(Long alunoId) {
        String mdA = "# Nenhuma versão anterior corrigida encontrada.";
        String mdB = "# Nenhuma versão atual encontrada.";
        String tagA = "(N/A)";
        String tagB = "(N/A)";

        try {
            Long trabalhoId = dao.obterTrabalhoIdPorAluno(alunoId);

            // 1. Busca a Versão B (Atual - a mais recente de todas)
            List<VersaoHistoricoDTO> historico = dao.listarHistoricoCompleto(trabalhoId);
            if (!historico.isEmpty()) {
                VersaoHistoricoDTO versaoAtual = historico.get(0);
                mdB = versaoAtual.conteudoMd();
                tagB = "(" + versaoAtual.versao() + ")";
            }

            // 2. Busca a Versão A (A última que o orientador corrigiu)
            Optional<VersaoHistoricoDTO> optVersaoAnterior = dao.findUltimaVersaoCorrigida(trabalhoId);
            if (optVersaoAnterior.isPresent()) {
                VersaoHistoricoDTO versaoAnterior = optVersaoAnterior.get();
                mdA = versaoAnterior.conteudoMd();
                tagA = "(" + versaoAnterior.versao() + ")";
            }

            // 3. Renderiza a diferença
            renderDiff(webA, webB, mdA, mdB);

            lblVersaoA.setText(tagA);
            lblVersaoB.setText(tagB);

            // 4. Calcula a similaridade
            double similaridade = calculateSimilarity(mdA, mdB);
            similarityBar.setProgress(similaridade);
            similarityPct.setText(String.format("%.0f%%", similaridade * 100));

        } catch (SQLException e) {
            e.printStackTrace();
            setupScrollBinding(webA.getEngine(), webB.getEngine()); // Configura a sincronia mesmo em erro
            renderMarkdown(webA, "# Erro ao carregar versão A: " + e.getMessage());
            renderMarkdown(webB, "# Erro ao carregar versão B: " + e.getMessage());
        }
    }

    /**
     * GERA O HTML COM O DIFF COMPLETO
     */
    private void renderDiff(WebView webViewA, WebView webViewB, String textA, String textB) {
        List<String> linesA = Arrays.asList(textA.split("\n"));
        List<String> linesB = Arrays.asList(textB.split("\n"));
        Patch<String> patch = DiffUtils.diff(linesA, linesB);

        StringBuilder htmlA = new StringBuilder();
        StringBuilder htmlB = new StringBuilder();
        int lastA = 0;
        int lastB = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int startA = delta.getSource().getPosition();
            int startB = delta.getTarget().getPosition();

            for (int i = lastA; i < startA; i++) {
                htmlA.append(formatLine("line-unchanged", linesA.get(i)));
            }
            for (int i = lastB; i < startB; i++) {
                htmlB.append(formatLine("line-unchanged", linesB.get(i)));
            }
            List<String> sourceLines = delta.getSource().getLines();
            List<String> targetLines = delta.getTarget().getLines();
            switch (delta.getType()) {
                case CHANGE:
                    sourceLines.forEach(line -> htmlA.append(formatLine("line-removed", line)));
                    targetLines.forEach(line -> htmlB.append(formatLine("line-added", line)));
                    if (sourceLines.size() > targetLines.size()) {
                        for (int i = 0; i < sourceLines.size() - targetLines.size(); i++) {
                            htmlB.append(formatLine("line-placeholder", "&nbsp;"));
                        }
                    } else if (targetLines.size() > sourceLines.size()) {
                        for (int i = 0; i < targetLines.size() - sourceLines.size(); i++) {
                            htmlA.append(formatLine("line-placeholder", "&nbsp;"));
                        }
                    }
                    break;
                case DELETE:
                    sourceLines.forEach(line -> htmlA.append(formatLine("line-removed", line)));
                    sourceLines.forEach(line -> htmlB.append(formatLine("line-placeholder", "&nbsp;")));
                    break;
                case INSERT:
                    targetLines.forEach(line -> htmlA.append(formatLine("line-placeholder", "&nbsp;")));
                    targetLines.forEach(line -> htmlB.append(formatLine("line-added", line)));
                    break;
                default:
                    break;
            }
            lastA = delta.getSource().getPosition() + sourceLines.size();
            lastB = delta.getTarget().getPosition() + targetLines.size();
        }
        for (int i = lastA; i < linesA.size(); i++) {
            htmlA.append(formatLine("line-unchanged", linesA.get(i)));
        }
        for (int i = lastB; i < linesB.size(); i++) {
            htmlB.append(formatLine("line-unchanged", linesB.get(i)));
        }

        setupScrollBinding(webViewA.getEngine(), webViewB.getEngine());
        webViewA.getEngine().loadContent(getHtmlTemplate(htmlA.toString(), true));
        webViewB.getEngine().loadContent(getHtmlTemplate(htmlB.toString(), true));
    }


    // ==========================================================
    // MÉTODOS PARA SINCRONIZAR O SCROLL (Inalterados)
    // ==========================================================
    public class ScrollBridge {
        private final WebEngine otherEngine;
        public ScrollBridge(WebEngine otherEngine) {
            this.otherEngine = otherEngine;
        }
        public void onScroll(double scrollY) {
            if (isSyncing) {
                return;
            }
            isSyncing = true;
            Platform.runLater(() -> {
                String script = String.format("window.scrollTo(0, %f);", scrollY);
                otherEngine.executeScript(script);
            });
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    isSyncing = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void setupScrollBinding(WebEngine engineA, WebEngine engineB) {
        engineA.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject windowA = (JSObject) engineA.executeScript("window");
                windowA.setMember("javaBridge", new ScrollBridge(engineB));
            }
        });

        engineB.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject windowB = (JSObject) engineB.executeScript("window");
                windowB.setMember("javaBridge", new ScrollBridge(engineA));
            }
        });
    }

    // ==========================================================
    // MÉTODOS DE RENDERIZAÇÃO E NAVEGAÇÃO (Inalterados)
    // ==========================================================
    private String formatLine(String cssClass, String content) {
        String escapedContent = escapeHtml(content);
        return String.format("<div class=\"diff-line %s\">%s</div>\n", cssClass, escapedContent.isEmpty() ? "&nbsp;" : escapedContent);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getHtmlTemplate(String contentHtml, boolean isDiff) {
        String script;
        if (isDiff) {
            script = "<script>\n    // Diff content is pre-rendered HTML\n</script>";
        } else {
            String escapedMarkdown = escapeForJavaScript(contentHtml);
            script = "<script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>\n" +
                    "<script>\n" +
                    "    const markdown = `" + escapedMarkdown + "`;\n" +
                    "    document.getElementById('content').innerHTML = marked.parse(markdown);\n" +
                    "</script>";
            contentHtml = "";
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        line-height: 1.6; padding: 10px; color: #333;
                        background: #fff; margin: 0;
                    }
                    /* ... (seu CSS de diff) ... */
                    .diff-line {
                        font-family: 'Courier New', monospace;
                        font-size: 13px;
                        white-space: pre-wrap;
                        padding: 2px 10px;
                        min-height: 1.2em;
                    }
                    .line-added { background-color: #e6ffed; border-left: 4px solid #28a745; }
                    .line-removed { background-color: #ffeef0; border-left: 4px solid #dc3545; text-decoration: line-through; }
                    .line-unchanged { color: #6a737d; }
                    .line-placeholder { background-color: #f6f8fa; color: #f6f8fa; min-height: 1.2em; }
                </style>
            </head>
            <body onscroll="if (window.javaBridge) javaBridge.onScroll(window.scrollY);">
                <div id="content">"""
                + contentHtml +
                """
                </div>
                """
                + script +
                """
            </body>
            </html>
        """;
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        int distance = levenshtein.apply(s1, s2);
        double similarity = 1.0 - ((double) distance / maxLength);
        return Math.max(0.0, similarity);
    }

    private void renderMarkdown(WebView webView, String markdown) {
        String html = getHtmlTemplate(markdown, false);
        webView.getEngine().loadContent(html);
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

    // --- Métodos de Navegação Padrão ---
    public void voltar(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){
        SceneManager.go("aluno/Inbox.fxml", c -> {
            var ctrl = (InboxAlunoController) c;
            User u = Session.getUser();
            if (u == null) { SceneManager.go("login/Login.fxml"); return; }
            ctrl.setAlunoContext(u.getId());
            ctrl.onReady();
        });
    }
    public void goEditor(){
        SceneManager.go("aluno/Editor.fxml", c -> {
            ((EditorAlunoController) c).onReady();
        });
    }
    public void goComparar(){ /* Já está aqui */ }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }
}