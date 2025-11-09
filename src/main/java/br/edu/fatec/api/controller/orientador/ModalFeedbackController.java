package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApiCamposDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApresentacaoCamposDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ResumoCamposDTO;
import br.edu.fatec.api.model.auth.User; // NOVO IMPORT
import br.edu.fatec.api.nav.Session; // NOVO IMPORT
import br.edu.fatec.api.dao.JdbcTrabalhosGraduacaoDao;
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
import java.util.Optional; // NOVO IMPORT

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
    @FXML private TextArea txtParecerApresentacao; // NOVO

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
    // NOVOS TextAreas
    @FXML private TextArea txtParecerApi1, txtParecerApi2, txtParecerApi3, txtParecerApi4, txtParecerApi5, txtParecerApi6;


    // Aba Resumo
    @FXML private Tab tabResumo;
    @FXML private Label lblResumoVersaoStatus;
    @FXML private WebView webResumo;
    @FXML private TextArea txtParecerResumo; // NOVO

    // Aba Finais
    @FXML private Tab tabFinais;
    @FXML private Label lblFinaisStatus;
    @FXML private WebView webFinais;
    @FXML private TextArea txtParecerFinais; // NOVO

    // --- Estado Interno ---
    private Long trabalhoId;
    private Long orientadorId; // NOVO
    private String versao;
    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private Parser mdParser;
    private HtmlRenderer mdRenderer;

    // Mapeamento de Campos (Apresentação)
    private final Map<String, String> camposApresentacaoMap = new LinkedHashMap<>();
    private final JdbcTrabalhosGraduacaoDao tgDao = new JdbcTrabalhosGraduacaoDao();
    private ApresentacaoCamposDTO camposApresentacaoCache;
    private String campoApresentacaoSelecionado;

    // Mapeamento de Campos (API)
    private final Map<String, String> camposApiMap = new LinkedHashMap<>();
    private ApiCamposDTO camposApiCache;
    private String campoApiSelecionado;
    private int apiIndexAtual;

    @FXML
    public void initialize() {
        // ... (código do initialize, initMarkdown, mappers, e setupListViews) ...
        // ... (COLE SEU MÉTODO initialize() INTEIRO DA RESPOSTA ANTERIOR AQUI) ...
        // ... (Ele já está correto e completo) ...

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

        // Pega o orientador logado
        User user = Session.getUser();
        if (user == null) {
            erro("Sessão do orientador expirou. Feche este modal e abra novamente.", null);
            return;
        }
        this.orientadorId = user.getId();

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
            // ... (o switch case para preencher markdown e status) ...
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

        // Carrega o último parecer salvo para este campo
        carregarUltimoParecer(txtParecerApresentacao, "APRESENTACAO", campoApresentacaoSelecionado);
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
            String comentario = txtParecerApresentacao.getText();

            // 1. Atualiza o status na tabela tg_apresentacao
            dao.atualizarStatusCampoApresentacao(trabalhoId, versao, campoApresentacaoSelecionado, novoStatus);
            // 2. Salva o parecer (comentário) na tabela pareceres
            dao.salvarParecer(trabalhoId, versao, orientadorId, "APRESENTACAO", campoApresentacaoSelecionado, novoStatus, comentario);

            // 3. Atualiza a UI
            atualizarStatusLabel(lblCampoStatusApresentacao, novoStatus);
            camposApresentacaoCache = dao.carregarCamposApresentacao(trabalhoId);
            listCamposApresentacao.refresh();

            info("Parecer salvo com sucesso!");

        } catch (SQLException e) {
            erro("Falha ao salvar parecer do campo", e);
        }
    }

    // ... (getStatusFromApresentacaoCache) ...
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

            ListView<String> currentListView = getListViewApi(apiIndex);
            currentListView.setItems(nomesCampos);

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

        Label currentTitulo = getLabelTituloApi(apiIndexAtual);
        Label currentStatus = getLabelStatusApi(apiIndexAtual);
        WebView currentWeb = getWebViewApi(apiIndexAtual);
        TextArea currentParecer = getTextAreaParecerApi(apiIndexAtual);

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
                status = camposApiCache.contribuicoesStatus(); // CORRIGIDO
            }
            case "hard_skills" -> {
                markdown = camposApiCache.hardSkills();
                status = camposApiCache.hardSkillsStatus(); // CORRIGIDO
            }
            case "soft_skills" -> {
                markdown = camposApiCache.softSkills();
                status = camposApiCache.softSkillsStatus(); // CORRIGIDO
            }
        }

        renderMarkdown(currentWeb, markdown);
        atualizarStatusLabel(currentStatus, status);

        carregarUltimoParecer(currentParecer, "API" + apiIndexAtual, campoApiSelecionado);
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
            TextArea currentParecer = getTextAreaParecerApi(apiIndexAtual);
            String comentario = currentParecer.getText();
            String secao = "API" + apiIndexAtual;

            // 1. Atualiza o status na tabela tg_secao
            dao.atualizarStatusCampoApi(trabalhoId, versao, apiIndexAtual, campoApiSelecionado, novoStatus);
            // 2. Salva o parecer
            dao.salvarParecer(trabalhoId, versao, orientadorId, secao, campoApiSelecionado, novoStatus, comentario);

            // 3. Atualiza a UI
            atualizarStatusLabel(getLabelStatusApi(apiIndexAtual), novoStatus);
            camposApiCache = dao.carregarCamposApi(trabalhoId, apiIndexAtual);
            getListViewApi(apiIndexAtual).refresh();

            info("Parecer salvo com sucesso!");

        } catch (SQLException e) {
            erro("Falha ao atualizar status do campo de API", e);
        }
    }

    // ... (getStatusFromApiCache) ...
    private int getStatusFromApiCache(String coluna) {
        if (camposApiCache == null || coluna == null) return 0;
        return switch (coluna) {
            case "empresa_parceira" -> camposApiCache.empresaParceiraStatus();
            case "problema" -> camposApiCache.problemaStatus();
            case "solucao_resumo" -> camposApiCache.solucaoResumoStatus();
            case "link_repositorio" -> camposApiCache.linkRepositorioStatus();
            case "tecnologias" -> camposApiCache.tecnologiasStatus();
            case "contribuicoes" -> camposApiCache.contribuicoesStatus(); // CORRIGIDO
            case "hard_skills" -> camposApiCache.hardSkillsStatus(); // CORRIGIDO
            case "soft_skills" -> camposApiCache.softSkillsStatus(); // CORRIGIDO
            default -> 0;
        };
    }


    // --- Lógica da Aba Resumo ---

    private void carregarAbaResumo() {
        try {
            ResumoCamposDTO dto = dao.carregarCamposResumo(trabalhoId);
            renderMarkdown(webResumo, dto.resumoMd());
            atualizarStatusLabel(lblResumoVersaoStatus, dto.versaoValidada());
            carregarUltimoParecer(txtParecerResumo, "RESUMO", "resumo_md");
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
            String comentario = txtParecerResumo.getText();
            // 1. Atualiza status
            dao.atualizarStatusResumo(trabalhoId, versao, novoStatus);
            // 2. Salva parecer
            dao.salvarParecer(trabalhoId, versao, orientadorId, "RESUMO", "resumo_md", novoStatus, comentario);

            // 3. Atualiza UI
            atualizarStatusLabel(lblResumoVersaoStatus, novoStatus);
            info("Parecer salvo com sucesso!");
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
            carregarUltimoParecer(txtParecerFinais, "FINAIS", "consideracoes_finais");
        } catch (SQLException e) {
            erro("Falha ao carregar aba Finais", e);
        }
    }

    @FXML private void onAprovarFinais() {
        atualizarStatusFinais(1);
    }

    @FXML private void onReprovarFinais() {
        atualizarStatusFinais(2);
    }

    private void atualizarStatusFinais(int novoStatus) {
        try {
            String comentario = txtParecerFinais.getText();
            String campoChave = "consideracoes_finais";

            // 1. Atualiza status (é um campo da tabela apresentacao)
            dao.atualizarStatusCampoApresentacao(trabalhoId, versao, campoChave, novoStatus);
            // 2. Salva parecer
            dao.salvarParecer(trabalhoId, versao, orientadorId, "FINAIS", campoChave, novoStatus, comentario);

            // 3. Atualiza UI
            atualizarStatusLabel(lblFinaisStatus, novoStatus);
            info("Parecer salvo com sucesso!");
        } catch (SQLException e) {
            erro("Falha ao salvar parecer de Finais", e);
        }
    }


    // --- Handshake (Finalizar Devolutiva) ---

    @FXML
    private void onFinalizarDevolutiva() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Finalizar Devolutiva");
        confirm.setHeaderText("Enviar feedback para o aluno?");
        confirm.setContentText("Isso irá atualizar o status do TG do aluno para 'Aprovado' ou 'Reprovado' (com pendências).\n\nEsta ação não pode ser desfeita.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return; // Usuário cancelou
        }

        try {
            // 1. Verifica o número de pendências (0 ou 2)
            int pendencias = dao.countPendenciasByVersao(trabalhoId, versao);

            String novoStatus;
            String msgSucesso;

            if (pendencias == 0) {
                // TUDO APROVADO
                novoStatus = "APROVADO";
                msgSucesso = "TG Aprovado! O aluno foi notificado.";
            } else {
                // AINDA TEM PENDÊNCIAS (0 ou 2)
                novoStatus = "REPROVADO";
                msgSucesso = "Devolutiva enviada com " + pendencias + " pendências. O aluno foi notificado.";
            }

            // 2. Atualiza o status principal do TG
            tgDao.updateStatus(trabalhoId, novoStatus);

            // 3. Informa o orientador e fecha o modal
            info(msgSucesso);
            fecharModal();

        } catch (Exception e) {
            erro("Falha grave ao finalizar a devolutiva.", e);
        }
    }


    // --- Métodos Utilitários ---

    /**
     * NOVO: Carrega o último comentário salvo em um TextArea.
     */
    private void carregarUltimoParecer(TextArea textArea, String secao, String campoChave) {
        if (textArea == null) return;
        try {
            Optional<String> parecer = dao.findUltimoParecer(trabalhoId, versao, secao, campoChave);
            textArea.setText(parecer.orElse("")); // Preenche com o parecer ou com vazio
        } catch (SQLException e) {
            textArea.setText("Erro ao carregar parecer: " + e.getMessage());
        }
    }

    @FXML
    private void fecharModal() {
        Stage stage = (Stage) lblTituloModal.getScene().getWindow();
        stage.close();
    }

    // ... (renderMarkdown, atualizarStatusLabel, helpers de API, erro, info) ...

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
    // NOVO: Helper para pegar o TextArea de parecer da API correta
    private TextArea getTextAreaParecerApi(int index) {
        return switch (index) {
            case 1 -> txtParecerApi1;
            case 2 -> txtParecerApi2;
            case 3 -> txtParecerApi3;
            case 4 -> txtParecerApi4;
            case 5 -> txtParecerApi5;
            case 6 -> txtParecerApi6;
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
    private void info(String msg) { // NOVO
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}