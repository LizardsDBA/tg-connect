package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.dao.JdbcFeedbackDao;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class ModalPreviewController {

    @FXML
    private WebView webViewPreview;

    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private Parser mdParser;
    private HtmlRenderer mdRenderer;

    @FXML
    public void initialize() {
        // Configurar o renderizador de Markdown
        MutableDataSet opts = new MutableDataSet();
        opts.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        mdParser = Parser.builder(opts).build();
        mdRenderer = HtmlRenderer.builder(opts).build();
    }

    /**
     * Ponto de entrada: Recebe os dados, busca no banco e renderiza.
     */
    public void initData(Long trabalhoId, String versao) {
        if (trabalhoId == null || versao == null) {
            renderMarkdown("# Erro\n\nTrabalho ou versão não informados.");
            return;
        }

        try {
            String conteudoMd = dao.carregarPreviewCompleto(trabalhoId, versao);
            renderMarkdown(conteudoMd);
        } catch (SQLException e) {
            erro("Falha ao carregar preview", e);
            renderMarkdown("# Erro de SQL\n\n" + e.getMessage());
        }
    }

    private void renderMarkdown(String markdown) {
        if (webViewPreview == null) return;
        String md = (markdown == null) ? "" : markdown;
        String htmlBody = mdRenderer.render(mdParser.parse(md));
        String page =
                "<!doctype html><html><head>" +
                        "<meta charset='UTF-8'>" +
                        "<style>" +
                        "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; padding: 10px; }" +
                        "  pre, code { white-space: pre-wrap; }" +
                        "  table { border-collapse: collapse; width: 100%; }" +
                        "  th, td { border: 1px solid #ddd; padding: 6px; }" +
                        "  h1,h2,h3 { margin-top: 0.8em; }" +
                        "</style>" +
                        "</head><body>" +
                        htmlBody +
                        "</body></html>";
        webViewPreview.getEngine().loadContent(page);
    }

    @FXML
    private void fecharModal() {
        Stage stage = (Stage) webViewPreview.getScene().getWindow();
        stage.close();
    }

    private void erro(String msg, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Ops!");
        a.setContentText(msg + (e != null ? "\n\n" + e.getMessage() : ""));
        a.showAndWait();
        if (e != null) e.printStackTrace();
    }
}