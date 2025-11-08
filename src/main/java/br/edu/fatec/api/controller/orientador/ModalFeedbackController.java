package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApiCamposDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApresentacaoCamposDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ResumoCamposDTO;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModalFeedbackController {

    // --- Injeção FXML ---
    @FXML private Label lblTituloModal, lblVersao;
    @FXML private TabPane tabPane;

    // Aba Apresentação
    @FXML private Tab tabApresentacao;
    @FXML private ListView<String> listCamposApresentacao;
    @FXML private VBox panePreviewApresentacao;
    @FXML private Label lblCampoTitulo, lblCampoStatusApresentacao;
    @FXML private WebView webApresentacao;

    // Abas API 1-6
    @FXML private Tab tabApi1, tabApi2, tabApi3, tabApi4, tabApi5, tabApi6;
    @FXML private ListView<String> listCamposApi1, listCamposApi2, listCamposApi3, listCamposApi4, listCamposApi5, listCamposApi6;
    @FXML private VBox panePreviewApi1, panePreviewApi2, panePreviewApi3, panePreviewApi4, panePreviewApi5, panePreviewApi6;
    @FXML private Label lblCampoTituloApi1, lblCampoStatusApi1;
    @FXML private Label lblCampoTituloApi2, lblCampoStatusApi2;
    @FXML private Label lblCampoTituloApi3, lblCampoStatusApi3;
    @FXML private Label lblCampoTituloApi4, lblCampoStatusApi4;
    @FXML private Label lblCampoTituloApi5, lblCampoStatusApi5;
    @FXML private Label lblCampoTituloApi6, lblCampoStatusApi6;
    @FXML private WebView webApi1, webApi2, webApi3, webApi4, webApi5, webApi6;

    // Aba Resumo
    @FXML private Tab tabResumo;
    @FXML private Label lblResumoVersaoStatus;
    @FXML private WebView webResumo;

    // Aba Finais
    @FXML private Tab tabFinais;
    @FXML private Label lblFinaisStatus;
    @FXML private WebView webFinais;

    // --- Estado Interno ---
    private Long trabalhoId;
    private String versao;
    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private Parser mdParser;
    private HtmlRenderer mdRenderer;

    // Mapeamento de Campos (Apresentação)
    private final Map<String, String> camposApresentacaoMap = new LinkedHashMap<>();
    private ApresentacaoCamposDTO camposApresentacaoCache;
    private String campoApresentacaoSelecionado; // Coluna do BD (ex: "nome_completo")

    // Mapeamento de Campos (API)
    private final Map<String, String> camposApiMap = new LinkedHashMap<>();
    private ApiCamposDTO camposApiCache; // Cache para a API *selecionada*
    private String campoApiSelecionado; // Coluna do BD (ex: "problema")
    private int apiIndexAtual; // Guarda qual API estamos vendo (1-6)


    @FXML
    public void initialize() {
        // 1. Configurar o renderizador de Markdown
        MutableDataSet opts = new MutableDataSet();
        opts.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        mdParser = Parser.builder(opts).build();
        mdRenderer = HtmlRenderer.builder(opts).build();

        // 2. Mapear os campos da apresentação
        camposApresentacaoMap.put("Informações Pessoais", "nome_completo");
        camposApresentacaoMap.put("Histórico Acadêmico", "historico_academico");
        camposApresentacaoMap.put("Motivação para entrar na Fatec", "motivacao_fatec");
        camposApresentacaoMap.put("Histórico Profissional", "historico_profissional");
        camposApresentacaoMap.put("Contatos", "contatos_email");
        camposApresentacaoMap.put("Principais Conhecimentos", "principais_conhecimentos");

        // 3. Mapear os campos da API
        camposApiMap.put("Empresa Parceira", "empresa_parceira");
        camposApiMap.put("Problema", "problema");
        camposApiMap.put("Solução Resumo", "solucao_resumo");
        camposApiMap.put("Link Repositório", "link_repositorio");
        camposApiMap.put("Tecnologias", "tecnologias");
        camposApiMap.put("Contribuições", "contribuicoes");
        camposApiMap.put("Hard Skills", "hard_skills");
        camposApiMap.put("Soft Skills", "soft_skills");

        // 4. Configurar Factory e Listeners
        setupListViewApresentacao();
        setupListViewApi(listCamposApi1);
        setupListViewApi(listCamposApi2);
        setupListViewApi(listCamposApi3);
        setupListViewApi(listCamposApi4);
        setupListViewApi(listCamposApi5);
        setupListViewApi(listCamposApi6);

        // 5. Configurar o listener das Abas
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> carregarDadosAba(newTab)
        );

        // 6. Ocultar painéis de preview
        panePreviewApresentacao.setVisible(false);
        panePreviewApi1.setVisible(false);
        panePreviewApi2.setVisible(false);
        panePreviewApi3.setVisible(false);
        panePreviewApi4.setVisible(false);
        panePreviewApi5.setVisible(false);
        panePreviewApi6.setVisible(false);
    }

    /** Configura o Cell Factory e o Listener da aba Apresentação */
    private void setupListViewApresentacao() {
        listCamposApresentacao.setCellFactory(param -> new ListCell<>() {
            private final HBox hbox = new HBox();
            private final Label labelTitulo = new Label();
            private final Region spacer = new Region();
            private final Label labelStatus = new Label();
            {
                labelStatus.getStyleClass().add("badge");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                hbox.setSpacing(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(labelTitulo, spacer, labelStatus);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    labelTitulo.setText(item);
                    String coluna = camposApresentacaoMap.get(item);
                    int status = getStatusFromApresentacaoCache(coluna);
                    atualizarStatusLabel(labelStatus, status);
                    setGraphic(hbox);
                }
            }
        });

        listCamposApresentacao.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> exibirCampoApresentacao(newVal)
        );
    }

    /** Configura o Cell Factory genérico para uma ListView de API */
    private void setupListViewApi(ListView<String> listView) {
        listView.setCellFactory(param -> new ListCell<>() {
            private final HBox hbox = new HBox();
            private final Label labelTitulo = new Label();
            private final Region spacer = new Region();
            private final Label labelStatus = new Label();
            {
                labelStatus.getStyleClass().add("badge");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                hbox.setSpacing(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(labelTitulo, spacer, labelStatus);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    labelTitulo.setText(item);
                    String coluna = camposApiMap.get(item);
                    int status = getStatusFromApiCache(coluna);
                    atualizarStatusLabel(labelStatus, status);
                    setGraphic(hbox);
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> exibirCampoApi(newVal)
        );
    }

    /**
     * Ponto de entrada chamado pelo EditorOrientadorController.
     */
    public void initData(Long trabalhoId, String versao) {
        this.trabalhoId = trabalhoId;
        this.versao = versao;
        this.lblVersao.setText(versao);
        carregarDadosAba(tabApresentacao); // Carrega a primeira aba
    }

    /**
     * Roteador: Chamado quando o usuário troca de aba.
     */
    private void carregarDadosAba(Tab aba) {
        if (aba == tabApresentacao) carregarAbaApresentacao();
        else if (aba == tabApi1) carregarAbaApi(1);
        else if (aba == tabApi2) carregarAbaApi(2);
        else if (aba == tabApi3) carregarAbaApi(3);
        else if (aba == tabApi4) carregarAbaApi(4);
        else if (aba == tabApi5) carregarAbaApi(5);
        else if (aba == tabApi6) carregarAbaApi(6);
        else if (aba == tabResumo) carregarAbaResumo();
        else if (aba == tabFinais) carregarAbaFinais();
    }

    // --- Lógica da Aba Apresentação ---

    private void carregarAbaApresentacao() {
        try {
            camposApresentacaoCache = dao.carregarCamposApresentacao(trabalhoId);
            ObservableList<String> nomesCampos = FXCollections.observableArrayList(camposApresentacaoMap.keySet());
            listCamposApresentacao.setItems(nomesCampos);
            panePreviewApresentacao.setVisible(true);
            listCamposApresentacao.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            erro("Falha ao carregar campos da Apresentação", e);
        }
    }

    private void exibirCampoApresentacao(String nomeCampo) {
        if (nomeCampo == null || camposApresentacaoCache == null) return;

        this.campoApresentacaoSelecionado = camposApresentacaoMap.get(nomeCampo);
        if (campoApresentacaoSelecionado == null) return;

        lblCampoTitulo.setText(nomeCampo);

        String markdown = "";
        int status = 0;

        switch (campoApresentacaoSelecionado) {
            case "nome_completo" -> {
                markdown = camposApresentacaoCache.nomeCompleto();
                status = camposApresentacaoCache.nomeCompletoStatus();
            }
            case "historico_academico" -> {
                markdown = camposApresentacaoCache.historicoAcademico();
                status = camposApresentacaoCache.historicoAcademicoStatus();
            }
            case "motivacao_fatec" -> {
                markdown = camposApresentacaoCache.motivacaoFatec();
                status = camposApresentacaoCache.motivacaoFatecStatus();
            }
            case "historico_profissional" -> {
                markdown = camposApresentacaoCache.historicoProfissional();
                status = camposApresentacaoCache.historicoProfissionalStatus();
            }
            case "contatos_email" -> {
                markdown = camposApresentacaoCache.contatosEmail();
                status = camposApresentacaoCache.contatosEmailStatus();
            }
            case "principais_conhecimentos" -> {
                markdown = camposApresentacaoCache.principaisConhecimentos();
                status = camposApresentacaoCache.principaisConhecimentosStatus();
            }
        }

        renderMarkdown(webApresentacao, markdown);
        atualizarStatusLabel(lblCampoStatusApresentacao, status);
    }

    @FXML private void onAprovarCampo() {
        atualizarStatusCampoApresentacao(1); // 1 = Aprovado
    }

    @FXML private void onReprovarCampo() {
        atualizarStatusCampoApresentacao(2); // 2 = Reprovado
    }

    private void atualizarStatusCampoApresentacao(int novoStatus) {
        if (campoApresentacaoSelecionado == null) return;
        try {
            dao.atualizarStatusCampoApresentacao(trabalhoId, versao, campoApresentacaoSelecionado, novoStatus);
            atualizarStatusLabel(lblCampoStatusApresentacao, novoStatus);
            camposApresentacaoCache = dao.carregarCamposApresentacao(trabalhoId);
            listCamposApresentacao.refresh();
        } catch (SQLException e) {
            erro("Falha ao atualizar status do campo", e);
        }
    }

    private int getStatusFromApresentacaoCache(String coluna) {
        if (camposApresentacaoCache == null || coluna == null) return 0;
        return switch (coluna) {
            case "nome_completo" -> camposApresentacaoCache.nomeCompletoStatus();
            case "historico_academico" -> camposApresentacaoCache.historicoAcademicoStatus();
            case "motivacao_fatec" -> camposApresentacaoCache.motivacaoFatecStatus();
            case "historico_profissional" -> camposApresentacaoCache.historicoProfissionalStatus();
            case "contatos_email" -> camposApresentacaoCache.contatosEmailStatus();
            case "principais_conhecimentos" -> camposApresentacaoCache.principaisConhecimentosStatus();
            default -> 0;
        };
    }


    // --- Lógica das Abas de API ---

    private void carregarAbaApi(int apiIndex) {
        this.apiIndexAtual = apiIndex;
        try {
            camposApiCache = dao.carregarCamposApi(trabalhoId, apiIndex);
            ObservableList<String> nomesCampos = FXCollections.observableArrayList(camposApiMap.keySet());

            // Direciona para a ListView correta
            ListView<String> currentListView = getListViewApi(apiIndex);
            currentListView.setItems(nomesCampos);

            // Exibe o painel de preview correto
            getPanePreviewApi(apiIndex).setVisible(true);
            currentListView.getSelectionModel().selectFirst();

        } catch (SQLException e) {
            erro("Falha ao carregar campos da API " + apiIndex, e);
        }
    }

    private void exibirCampoApi(String nomeCampo) {
        if (nomeCampo == null || camposApiCache == null) return;

        this.campoApiSelecionado = camposApiMap.get(nomeCampo);
        if (campoApiSelecionado == null) return;

        // Pega os componentes FXML corretos para a aba atual
        Label currentTitulo = getLabelTituloApi(apiIndexAtual);
        Label currentStatus = getLabelStatusApi(apiIndexAtual);
        WebView currentWeb = getWebViewApi(apiIndexAtual);

        currentTitulo.setText(nomeCampo);

        String markdown = "";
        int status = 0;

        switch (campoApiSelecionado) {
            case "empresa_parceira" -> {
                markdown = camposApiCache.empresaParceira();
                status = camposApiCache.empresaParceiraStatus();
            }
            case "problema" -> {
                markdown = camposApiCache.problema();
                status = camposApiCache.problemaStatus();
            }
            case "solucao_resumo" -> {
                markdown = camposApiCache.solucaoResumo();
                status = camposApiCache.solucaoResumoStatus();
            }
            case "link_repositorio" -> {
                markdown = camposApiCache.linkRepositorio();
                status = camposApiCache.linkRepositorioStatus();
            }
            case "tecnologias" -> {
                markdown = camposApiCache.tecnologias();
                status = camposApiCache.tecnologiasStatus();
            }
            case "contribuicoes" -> {
                markdown = camposApiCache.contribuicoes();
                status = camposApiCache.contribuicoesStatus();
            }
            case "hard_skills" -> {
                markdown = camposApiCache.hardSkills();
                status = camposApiCache.hardSkillsStatus();
            }
            case "soft_skills" -> {
                markdown = camposApiCache.softSkills();
                status = camposApiCache.softSkillsStatus();
            }
        }

        renderMarkdown(currentWeb, markdown);
        atualizarStatusLabel(currentStatus, status);
    }

    @FXML private void onAprovarCampoApi() {
        atualizarStatusCampoApi(1); // 1 = Aprovado
    }

    @FXML private void onReprovarCampoApi() {
        atualizarStatusCampoApi(2); // 2 = Reprovado
    }

    private void atualizarStatusCampoApi(int novoStatus) {
        if (campoApiSelecionado == null) return;
        try {
            // 1. Atualiza o BD
            dao.atualizarStatusCampoApi(trabalhoId, versao, apiIndexAtual, campoApiSelecionado, novoStatus);

            // 2. Atualiza o label de status (à direita)
            atualizarStatusLabel(getLabelStatusApi(apiIndexAtual), novoStatus);

            // 3. Recarrega o cache
            camposApiCache = dao.carregarCamposApi(trabalhoId, apiIndexAtual);

            // 4. Atualiza a lista (à esquerda)
            getListViewApi(apiIndexAtual).refresh();

        } catch (SQLException e) {
            erro("Falha ao atualizar status do campo de API", e);
        }
    }

    private int getStatusFromApiCache(String coluna) {
        if (camposApiCache == null || coluna == null) return 0;
        return switch (coluna) {
            case "empresa_parceira" -> camposApiCache.empresaParceiraStatus();
            case "problema" -> camposApiCache.problemaStatus();
            case "solucao_resumo" -> camposApiCache.solucaoResumoStatus();
            case "link_repositorio" -> camposApiCache.linkRepositorioStatus();
            case "tecnologias" -> camposApiCache.tecnologiasStatus();
            case "contribuicoes" -> camposApiCache.contribuicoesStatus();
            case "hard_skills" -> camposApiCache.hardSkillsStatus();
            case "soft_skills" -> camposApiCache.softSkillsStatus();
            default -> 0;
        };
    }


    // --- Lógica da Aba Resumo ---

    private void carregarAbaResumo() {
        try {
            ResumoCamposDTO dto = dao.carregarCamposResumo(trabalhoId);
            renderMarkdown(webResumo, dto.resumoMd());
            atualizarStatusLabel(lblResumoVersaoStatus, dto.versaoValidada());
        } catch (SQLException e) {
            erro("Falha ao carregar aba Resumo", e);
        }
    }

    @FXML private void onAprovarResumo() {
        atualizarStatusResumo(1); // 1 = Aprovado
    }

    @FXML private void onReprovarResumo() {
        atualizarStatusResumo(2); // 2 = Reprovado
    }

    private void atualizarStatusResumo(int novoStatus) {
        try {
            dao.atualizarStatusResumo(trabalhoId, versao, novoStatus);
            atualizarStatusLabel(lblResumoVersaoStatus, novoStatus);
        } catch (SQLException e) {
            erro("Falha ao atualizar status do Resumo", e);
        }
    }


    // --- Lógica da Aba Finais ---

    private void carregarAbaFinais() {
        try {
            ApresentacaoCamposDTO dto = dao.carregarCamposFinais(trabalhoId);
            renderMarkdown(webFinais, dto.consideracoesFinais());
            atualizarStatusLabel(lblFinaisStatus, dto.consideracoesFinaisStatus());
        } catch (SQLException e) {
            erro("Falha ao carregar aba Finais", e);
        }
    }

    @FXML private void onAprovarFinais() {
        try {
            dao.atualizarStatusCampoApresentacao(trabalhoId, versao, "consideracoes_finais", 1);
            atualizarStatusLabel(lblFinaisStatus, 1);
        } catch (SQLException e) {
            erro("Falha ao aprovar Finais", e);
        }
    }

    @FXML private void onReprovarFinais() {
        try {
            dao.atualizarStatusCampoApresentacao(trabalhoId, versao, "consideracoes_finais", 2);
            atualizarStatusLabel(lblFinaisStatus, 2);
        } catch (SQLException e) {
            erro("Falha ao reprovar Finais", e);
        }
    }


    // --- Métodos Utilitários ---

    @FXML
    private void fecharModal() {
        Stage stage = (Stage) lblTituloModal.getScene().getWindow();
        stage.close();
    }

    private void renderMarkdown(WebView webView, String markdown) {
        if (webView == null) return;
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
        webView.getEngine().loadContent(page);
    }

    private void atualizarStatusLabel(Label label, int status) {
        if (label == null) return;
        label.getStyleClass().removeAll("badge-ok", "badge-pendente", "badge-reprovado");
        switch (status) {
            case 1 -> {
                label.setText("Aprovado");
                label.getStyleClass().add("badge-ok");
            }
            case 2 -> {
                label.setText("Reprovado");
                label.getStyleClass().add("badge-reprovado");
            }
            default -> {
                label.setText("Pendente");
                label.getStyleClass().add("badge-pendente");
            }
        }
    }

    // --- Helpers de Roteamento de API ---

    private ListView<String> getListViewApi(int index) {
        return switch (index) {
            case 1 -> listCamposApi1;
            case 2 -> listCamposApi2;
            case 3 -> listCamposApi3;
            case 4 -> listCamposApi4;
            case 5 -> listCamposApi5;
            case 6 -> listCamposApi6;
            default -> null;
        };
    }

    private VBox getPanePreviewApi(int index) {
        return switch (index) {
            case 1 -> panePreviewApi1;
            case 2 -> panePreviewApi2;
            case 3 -> panePreviewApi3;
            case 4 -> panePreviewApi4;
            case 5 -> panePreviewApi5;
            case 6 -> panePreviewApi6;
            default -> null;
        };
    }

    private Label getLabelTituloApi(int index) {
        return switch (index) {
            case 1 -> lblCampoTituloApi1;
            case 2 -> lblCampoTituloApi2;
            case 3 -> lblCampoTituloApi3;
            case 4 -> lblCampoTituloApi4;
            case 5 -> lblCampoTituloApi5;
            case 6 -> lblCampoTituloApi6;
            default -> null;
        };
    }

    private Label getLabelStatusApi(int index) {
        return switch (index) {
            case 1 -> lblCampoStatusApi1;
            case 2 -> lblCampoStatusApi2;
            case 3 -> lblCampoStatusApi3;
            case 4 -> lblCampoStatusApi4;
            case 5 -> lblCampoStatusApi5;
            case 6 -> lblCampoStatusApi6;
            default -> null;
        };
    }

    private WebView getWebViewApi(int index) {
        return switch (index) {
            case 1 -> webApi1;
            case 2 -> webApi2;
            case 3 -> webApi3;
            case 4 -> webApi4;
            case 5 -> webApi5;
            case 6 -> webApi6;
            default -> null;
        };
    }

    private void erro(String msg, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Ops!");
        a.setContentText(msg + (e != null ? "\n\n" + e.getMessage() : ""));
        a.showAndWait();
        if (e != null) e.printStackTrace();
    }
}