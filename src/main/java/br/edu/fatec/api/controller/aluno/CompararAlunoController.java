package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcVersoesTrabalhoDao;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional; // Import necessário

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

    @FXML
    private void initialize() {
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }
        // Carrega os dados assim que a tela é inicializada
        onReady();
    }

    /**
     * Ponto de entrada chamado pelo SceneManager ou initialize
     */
    public void onReady() {
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }
        loadVersions(u.getId());
    }

    /**
     * Carrega as versões, renderiza e calcula a similaridade.
     */
    private void loadVersions(Long alunoId) {
        try {
            Long trabalhoId = dao.obterTrabalhoIdPorAluno(alunoId);

            // ================== INÍCIO DA CORREÇÃO ==================

            // 1. Busca a Versão Atual (B) - A mais recente (sempre)
            List<VersaoHistoricoDTO> historico = dao.listarHistoricoCompleto(trabalhoId);
            VersaoHistoricoDTO versaoAtual = null;
            if (!historico.isEmpty()) {
                versaoAtual = historico.get(0);
            }

            // 2. Busca a Versão Corrigida (A) - Usando o novo método do DAO
            //    Este método agora procura na tabela 'pareceres'
            Optional<VersaoHistoricoDTO> optVersaoCorrigida = dao.findUltimaVersaoCorrigida(trabalhoId);

            // 3. Renderiza os conteúdos
            String mdA = optVersaoCorrigida.map(VersaoHistoricoDTO::conteudoMd)
                    .orElse("# Nenhuma versão corrigida encontrada.");
            String mdB = (versaoAtual != null) ? versaoAtual.conteudoMd() : "# Nenhuma versão atual encontrada.";

            renderMarkdown(webA, mdA);
            renderMarkdown(webB, mdB);

            // 4. Atualiza os Rótulos
            lblVersaoA.setText(optVersaoCorrigida.map(v -> "(" + v.versao() + ")")
                    .orElse(""));
            lblVersaoB.setText((versaoAtual != null) ? "(" + versaoAtual.versao() + ")" : "");

            // ================== FIM DA CORREÇÃO ==================


            // 5. Calcula e exibe a similaridade (Lógica inalterada)
            double similaridade = calculateSimilarity(mdA, mdB);
            similarityBar.setProgress(similaridade);
            similarityPct.setText(String.format("%.0f%%", similaridade * 100));

        } catch (SQLException e) {
            e.printStackTrace();
            // Tratar erro (ex: exibir um alerta)
            renderMarkdown(webA, "# Erro ao carregar versão: " + e.getMessage());
            renderMarkdown(webB, "# Erro ao carregar versão: " + e.getMessage());
        }
    }

    /**
     * Calcula a similaridade entre dois textos usando Levenshtein.
     * Retorna um valor entre 0.0 (totalmente diferente) e 1.0 (igual).
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0; // Ambos vazios são 100% similares
        }

        int distance = levenshtein.apply(s1, s2);

        // A similaridade é o inverso da distância normalizada
        double similarity = 1.0 - ((double) distance / maxLength);

        return Math.max(0.0, similarity); // Garante que não seja negativo
    }

    // --- Métodos de Renderização (Copiados do HistoricoAlunoController) ---

    private void renderMarkdown(WebView webView, String markdown) {
        WebEngine engine = webView.getEngine();
        String escapedMarkdown = escapeForJavaScript(markdown);

        // HTML template (mesmo do seu Histórico)
        String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                            line-height: 1.6; padding: 20px; color: #333;
                            background: #fff; max-width: 100%%; margin: 0 auto;
                        }
                        h1, h2, h3, h4, h5, h6 { margin-top: 24px; margin-bottom: 16px; font-weight: 600; line-height: 1.25; }
                        h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                        h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                        h3 { font-size: 1.25em; }
                        code { background-color: #f6f8fa; border-radius: 3px; padding: 2px 4px; font-family: 'Courier New', Courier, monospace; font-size: 85%%; }
                        pre { background-color: #f6f8fa; border-radius: 3px; padding: 16px; overflow: auto; margin-bottom: 16px; }
                        blockquote { padding: 0 1em; color: #6a737d; border-left: 0.25em solid #dfe2e5; margin: 0 0 16px 0; }
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