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
// Removidos imports de Node e Parent
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
// Removido import do ScrollPane
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject; // NOVO: Import para a ponte JS
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

    // NOVO: Flag de sincronia em nível de classe
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

    private void loadVersions(Long alunoId) {
        try {
            Long trabalhoId = dao.obterTrabalhoIdPorAluno(alunoId);
            List<VersaoHistoricoDTO> historico = dao.listarHistoricoCompleto(trabalhoId);

            VersaoHistoricoDTO versaoAtual = null;
            VersaoHistoricoDTO versaoAnterior = null;

            if (historico.size() >= 1) {
                versaoAtual = historico.get(0);
            }
            if (historico.size() >= 2) {
                versaoAnterior = historico.get(1);
            }

            String mdA = (versaoAnterior != null) ? versaoAnterior.conteudoMd() : "# Nenhuma versão anterior encontrada.";
            String mdB = (versaoAtual != null) ? versaoAtual.conteudoMd() : "# Nenhuma versão atual encontrada.";

            renderDiff(webA, webB, mdA, mdB); // Método atualizado

            lblVersaoA.setText((versaoAnterior != null) ? "(" + versaoAnterior.versao() + ")" : "");
            lblVersaoB.setText((versaoAtual != null) ? "(" + versaoAtual.versao() + ")" : "");

            double similaridade = calculateSimilarity(mdA, mdB);
            similarityBar.setProgress(similaridade);
            similarityPct.setText(String.format("%.0f%%", similaridade * 100));

        } catch (SQLException e) {
            e.printStackTrace();
            // Configura a sincronia mesmo em caso de erro
            setupScrollBinding(webA.getEngine(), webB.getEngine());
            renderMarkdown(webA, "# Erro ao carregar versão: " + e.getMessage());
            renderMarkdown(webB, "# Erro ao carregar versão: " + e.getMessage());
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

            // ... (Lógica do Diff - inalterada) ...
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

        // Configura os listeners de scroll ANTES de carregar o conteúdo
        setupScrollBinding(webViewA.getEngine(), webViewB.getEngine());

        // Carrega o HTML final
        webViewA.getEngine().loadContent(getHtmlTemplate(htmlA.toString(), true));
        webViewB.getEngine().loadContent(getHtmlTemplate(htmlB.toString(), true));
    }


    // ==========================================================
    // MÉTODOS PARA SINCRONIZAR O SCROLL (Plano E: Ponte Java-JS)
    // ==========================================================

    /**
     * Classe interna que servirá como "ponte" entre o JS (WebView) e o Java.
     */
    public class ScrollBridge {
        private final WebEngine otherEngine; // O motor do "outro" WebView

        public ScrollBridge(WebEngine otherEngine) {
            this.otherEngine = otherEngine;
        }

        /**
         * Este método é CHAMADO PELO JAVASCRIPT (do onscroll).
         * @param scrollY A posição Y do scroll (ex: 500.5)
         */
        public void onScroll(double scrollY) {
            if (isSyncing) {
                // Este scroll foi causado por nós (loop), então ignore.
                return;
            }

            // 1. Marca que estamos sincronizando (evita o loop)
            isSyncing = true;

            // 2. Executa o scroll no outro WebView
            // Usamos Platform.runLater para garantir que isso rode na thread do JavaFX
            Platform.runLater(() -> {
                String script = String.format("window.scrollTo(0, %f);", scrollY);
                otherEngine.executeScript(script);
            });

            // 3. Reseta a flag (após um pequeno delay para o outro scroll terminar)
            // Esta é a forma mais simples de "debounce"
            new Thread(() -> {
                try {
                    Thread.sleep(50); // Espera 50ms
                    isSyncing = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Injeta os objetos "ScrollBridge" no contexto JavaScript de cada WebView
     * assim que eles terminarem de carregar.
     */
    private void setupScrollBinding(WebEngine engineA, WebEngine engineB) {

        engineA.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Quando A carregar, injeta a ponte que controla B
                JSObject windowA = (JSObject) engineA.executeScript("window");
                windowA.setMember("javaBridge", new ScrollBridge(engineB));
                System.out.println("[SCROLL_DEBUG] Ponte JS-Java injetada em A (controla B)");
            }
        });

        engineB.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Quando B carregar, injeta a ponte que controla A
                JSObject windowB = (JSObject) engineB.executeScript("window");
                windowB.setMember("javaBridge", new ScrollBridge(engineA));
                System.out.println("[SCROLL_DEBUG] Ponte JS-Java injetada em B (controla A)");
            }
        });
    }

    // ==========================================================
    // MÉTODOS DE RENDERIZAÇÃO E NAVEGAÇÃO
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

    /**
     * Retorna o template HTML (ATUALIZADO com onscroll)
     */
    private String getHtmlTemplate(String contentHtml, boolean isDiff) {
        String script;
        if (isDiff) {
            // Script de Diff (vazio, pois o onscroll está no body)
            script = "<script>\n    // Diff content is pre-rendered HTML\n</script>";
        } else {
            // Script de Fallback (Erro)
            String escapedMarkdown = escapeForJavaScript(contentHtml);
            script = "<script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>\n" +
                    "<script>\n" +
                    "    const markdown = `" + escapedMarkdown + "`;\n" +
                    "    document.getElementById('content').innerHTML = marked.parse(markdown);\n" +
                    "</script>";
            contentHtml = "";
        }

        // Adicionamos 'onscroll="javaBridge.onScroll(window.scrollY)"' na tag <body>
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    /* ... (Todo o seu CSS de antes está aqui) ... */
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        line-height: 1.6; padding: 10px; color: #333;
                        background: #fff; margin: 0;
                    }
                    h1, h2, h3 { 
                        margin-top: 16px; margin-bottom: 10px; font-weight: 600; 
                        line-height: 1.25; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em;
                    }
                    h1 { font-size: 1.8em; } h2 { font-size: 1.4em; } h3 { font-size: 1.2em; }
                    code { background-color: #f6f8fa; border-radius: 3px; padding: 2px 4px; font-family: 'Courier New', Courier, monospace; font-size: 85%%; }
                    pre { background-color: #f6f8fa; border-radius: 3px; padding: 12px; overflow: auto; margin-bottom: 12px; }
                    blockquote { padding: 0 1em; color: #6a737d; border-left: 0.25em solid #dfe2e5; margin: 0 0 16px 0; }
                    .diff-line {
                        font-family: 'Courier New', monospace;
                        font-size: 13px;
                        white-space: pre-wrap;
                        padding: 2px 10px;
                        min-height: 1.2em;
                    }
                    .line-added {
                        background-color: #e6ffed;
                        border-left: 4px solid #28a745;
                    }
                    .line-removed {
                        background-color: #ffeef0;
                        border-left: 4px solid #dc3545;
                        text-decoration: line-through;
                    }
                    .line-unchanged {
                        color: #6a737d;
                    }
                    .line-placeholder {
                        background-color: #f6f8fa;
                        color: #f6f8fa;
                        min-height: 1.2em;
                    }
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
        SceneManager.go("aluno/Editor.fxml");
    }
    public void goComparar(){ /* Já está aqui */ }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }
}