package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApiCamposDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApresentacaoCamposDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ResumoCamposDTO;
import br.edu.fatec.api.dao.JdbcTrabalhosGraduacaoDao;
import br.edu.fatec.api.dao.JdbcVersoesTrabalhoDao;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.Session;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node; // Import correto para o 'Node'
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModalFeedbackController {

    // --- Injeção FXML ---
    @FXML private Label lblTituloModal, lblVersao;
    @FXML private TabPane tabPane;

    // --- (Aba Apresentação) ---
    @FXML private Tab tabApresentacao;
    @FXML private ListView<String> listCamposApresentacao;
    @FXML private VBox panePreviewApresentacao; // CORRIGIDO
    @FXML private Label lblCampoTitulo, lblCampoStatusApresentacao;
    @FXML private ToggleButton btnToggleCorrecao;
    @FXML private VBox paneVersaoAnterior;
    @FXML private Label lblTituloAnterior;
    @FXML private WebView webApresentacao_Anterior;
    @FXML private TextArea txtParecerApresentacao_Anterior;
    @FXML private WebView webApresentacao_Atual;
    @FXML private TextArea txtParecerApresentacao_Atual;

    // --- (Abas API 1-6) ---
    @FXML private Tab tabApi1, tabApi2, tabApi3, tabApi4, tabApi5, tabApi6;
    @FXML private ListView<String> listCamposApi1, listCamposApi2, listCamposApi3, listCamposApi4, listCamposApi5, listCamposApi6;
    @FXML private VBox panePreviewApi1, panePreviewApi2, panePreviewApi3, panePreviewApi4, panePreviewApi5, panePreviewApi6; // CORRIGIDO
    @FXML private Label lblCampoTituloApi1, lblCampoStatusApi1;
    @FXML private Label lblCampoTituloApi2, lblCampoStatusApi2;
    @FXML private Label lblCampoTituloApi3, lblCampoStatusApi3;
    @FXML private Label lblCampoTituloApi4, lblCampoStatusApi4;
    @FXML private Label lblCampoTituloApi5, lblCampoStatusApi5;
    @FXML private Label lblCampoTituloApi6, lblCampoStatusApi6;

    @FXML private ToggleButton btnToggleCorrecaoApi1, btnToggleCorrecaoApi2, btnToggleCorrecaoApi3, btnToggleCorrecaoApi4, btnToggleCorrecaoApi5, btnToggleCorrecaoApi6;
    @FXML private VBox paneVersaoAnteriorApi1, paneVersaoAnteriorApi2, paneVersaoAnteriorApi3, paneVersaoAnteriorApi4, paneVersaoAnteriorApi5, paneVersaoAnteriorApi6;
    @FXML private Label lblTituloAnteriorApi1, lblTituloAnteriorApi2, lblTituloAnteriorApi3, lblTituloAnteriorApi4, lblTituloAnteriorApi5, lblTituloAnteriorApi6;
    @FXML private WebView webApi1_Anterior, webApi2_Anterior, webApi3_Anterior, webApi4_Anterior, webApi5_Anterior, webApi6_Anterior;
    @FXML private TextArea txtParecerApi1_Anterior, txtParecerApi2_Anterior, txtParecerApi3_Anterior, txtParecerApi4_Anterior, txtParecerApi5_Anterior, txtParecerApi6_Anterior;
    @FXML private WebView webApi1_Atual, webApi2_Atual, webApi3_Atual, webApi4_Atual, webApi5_Atual, webApi6_Atual;
    @FXML private TextArea txtParecerApi1_Atual, txtParecerApi2_Atual, txtParecerApi3_Atual, txtParecerApi4_Atual, txtParecerApi5_Atual, txtParecerApi6_Atual;


    // --- (Aba Resumo) ---
    @FXML private Tab tabResumo;
    @FXML private Label lblResumoVersaoStatus;
    @FXML private ToggleButton btnToggleCorrecaoResumo;
    @FXML private VBox paneVersaoAnteriorResumo;
    @FXML private Label lblTituloAnteriorResumo;
    @FXML private WebView webResumo_Anterior;
    @FXML private TextArea txtParecerResumo_Anterior;
    @FXML private WebView webResumo_Atual;
    @FXML private TextArea txtParecerResumo_Atual;

    // --- (Aba Finais) ---
    @FXML private Tab tabFinais;
    @FXML private Label lblFinaisStatus;
    @FXML private ToggleButton btnToggleCorrecaoFinais;
    @FXML private VBox paneVersaoAnteriorFinais;
    @FXML private Label lblTituloAnteriorFinais;
    @FXML private WebView webFinais_Anterior;
    @FXML private TextArea txtParecerFinais_Anterior;
    @FXML private WebView webFinais_Atual;
    @FXML private TextArea txtParecerFinais_Atual;


    // --- Estado Interno ---
    private Long trabalhoId;
    private Long orientadorId;
    private String versaoAtual;
    private String versaoAnteriorCorrigida;

    // DAOs
    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private final JdbcTrabalhosGraduacaoDao tgDao = new JdbcTrabalhosGraduacaoDao();
    private final JdbcVersoesTrabalhoDao versoesDao = new JdbcVersoesTrabalhoDao();

    // Caches
    private ApresentacaoCamposDTO camposApresentacaoCache;
    private ApresentacaoCamposDTO camposApresentacaoCache_Anterior;
    private ApiCamposDTO camposApiCache;
    private ApiCamposDTO camposApiCache_Anterior;

    // Mapeamentos
    private final Map<String, String> camposApresentacaoMap = new LinkedHashMap<>();
    private final Map<String, String> camposApiMap = new LinkedHashMap<>();

    private String campoApresentacaoSelecionado;
    private String campoApiSelecionado;
    private int apiIndexAtual;

    // Renderizador
    private Parser mdParser;
    private HtmlRenderer mdRenderer;


    @FXML
    public void initialize() {
        // 1. Configurar o renderizador
        MutableDataSet opts = new MutableDataSet();
        opts.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        mdParser = Parser.builder(opts).build();
        mdRenderer = HtmlRenderer.builder(opts).build();

        // 2. Mapear os campos
        mapearCampos();

        // 3. Configurar Listeners (Listas e Abas)
        setupListViewApresentacao();
        setupListViewApi(listCamposApi1, 1);
        setupListViewApi(listCamposApi2, 2);
        setupListViewApi(listCamposApi3, 3);
        setupListViewApi(listCamposApi4, 4);
        setupListViewApi(listCamposApi5, 5);
        setupListViewApi(listCamposApi6, 6);

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> carregarDadosAba(newTab)
        );

        // 4. Configurar Toggles de Comparação
        setupToggleBinding(btnToggleCorrecao, paneVersaoAnterior);
        setupToggleBinding(btnToggleCorrecaoApi1, paneVersaoAnteriorApi1);
        setupToggleBinding(btnToggleCorrecaoApi2, paneVersaoAnteriorApi2);
        setupToggleBinding(btnToggleCorrecaoApi3, paneVersaoAnteriorApi3);
        setupToggleBinding(btnToggleCorrecaoApi4, paneVersaoAnteriorApi4);
        setupToggleBinding(btnToggleCorrecaoApi5, paneVersaoAnteriorApi5);
        setupToggleBinding(btnToggleCorrecaoApi6, paneVersaoAnteriorApi6);
        setupToggleBinding(btnToggleCorrecaoResumo, paneVersaoAnteriorResumo);
        setupToggleBinding(btnToggleCorrecaoFinais, paneVersaoAnteriorFinais);

        // 5. Ocultar painéis
        VBox[] paineis = {
                panePreviewApresentacao, panePreviewApi1, panePreviewApi2, panePreviewApi3,
                panePreviewApi4, panePreviewApi5, panePreviewApi6
        };
        for (VBox pane : paineis) {
            if (pane != null) pane.setVisible(false);
        }
    }

    private void mapearCampos() {
        camposApresentacaoMap.put("Informações Pessoais", "nome_completo");
        camposApresentacaoMap.put("Histórico Acadêmico", "historico_academico");
        camposApresentacaoMap.put("Motivação para entrar na Fatec", "motivacao_fatec");
        camposApresentacaoMap.put("Histórico Profissional", "historico_profissional");
        camposApresentacaoMap.put("Contatos", "contatos_email");
        camposApresentacaoMap.put("Principais Conhecimentos", "principais_conhecimentos");

        camposApiMap.put("Empresa Parceira", "empresa_parceira");
        camposApiMap.put("Problema", "problema");
        camposApiMap.put("Solução Resumo", "solucao_resumo");
        camposApiMap.put("Link Repositório", "link_repositorio");
        camposApiMap.put("Tecnologias", "tecnologias");
        camposApiMap.put("Contribuições", "contribuicoes");
        camposApiMap.put("Hard Skills", "hard_skills");
        camposApiMap.put("Soft Skills", "soft_skills");
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

    private void setupListViewApi(ListView<String> listView, int apiIndex) {
        if (listView == null) return;

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
                    if (apiIndexAtual == apiIndex) {
                        int status = getStatusFromApiCache(coluna);
                        atualizarStatusLabel(labelStatus, status);
                    }
                    setGraphic(hbox);
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null && tabPane.getSelectionModel().getSelectedItem() == getTabApi(apiIndex)) {
                        exibirCampoApi(newVal);
                    }
                }
        );
    }

    private void setupToggleBinding(ToggleButton toggle, VBox pane) {
        if (toggle == null || pane == null) return;

        pane.visibleProperty().bind(toggle.selectedProperty());
        pane.managedProperty().bind(toggle.selectedProperty());

        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (pane.getParent() != null && pane.getParent() instanceof SplitPane) {
                SplitPane splitPane = (SplitPane) pane.getParent();
                splitPane.requestLayout();
                if(newVal) {
                    splitPane.setDividerPositions(0.5);
                }
            }
        });
    }

    public void initData(Long trabalhoId, String versaoAtual) {
        this.trabalhoId = trabalhoId;
        this.versaoAtual = versaoAtual;
        this.lblVersao.setText(versaoAtual);

        User user = Session.getUser();
        if (user == null) {
            erro("Sessão do orientador expirou. Feche este modal e abra novamente.", null);
            return;
        }
        this.orientadorId = user.getId();

        try {
            this.versaoAnteriorCorrigida = versoesDao.findUltimaVersaoCorrigida(trabalhoId)
                    .map(VersaoHistoricoDTO::versao)
                    .orElse(null);
        } catch (SQLException e) {
            erro("Falha ao buscar histórico de versões", e);
        }

        carregarDadosAba(tabApresentacao);
    }

    private void carregarDadosAba(Tab aba) {
        try {
            if (aba == tabApresentacao) carregarAbaApresentacao();
            else if (aba == tabApi1) carregarAbaApi(1);
            else if (aba == tabApi2) carregarAbaApi(2);
            else if (aba == tabApi3) carregarAbaApi(3);
            else if (aba == tabApi4) carregarAbaApi(4);
            else if (aba == tabApi5) carregarAbaApi(5);
            else if (aba == tabApi6) carregarAbaApi(6);
            else if (aba == tabResumo) carregarAbaResumo();
            else if (aba == tabFinais) carregarAbaFinais();
        } catch (SQLException e) {
            erro("Falha crítica ao carregar dados da aba", e);
        }
    }

    // --- Lógica da Aba Apresentação ---

    private void carregarAbaApresentacao() throws SQLException {
        camposApresentacaoCache = dao.carregarCamposApresentacao(trabalhoId, versaoAtual);

        if (versaoAnteriorCorrigida != null) {
            // USA O NOVO MÉTODO SOBRECARREGADO
            camposApresentacaoCache_Anterior = dao.carregarCamposApresentacao(trabalhoId, versaoAnteriorCorrigida);
            lblTituloAnterior.setText("Versão Anterior (" + versaoAnteriorCorrigida + ")");
            btnToggleCorrecao.setDisable(false);
        } else {
            btnToggleCorrecao.setDisable(true);
            btnToggleCorrecao.setSelected(false);
        }

        if (listCamposApresentacao.getItems().isEmpty()) {
            ObservableList<String> nomesCampos = FXCollections.observableArrayList(camposApresentacaoMap.keySet());
            listCamposApresentacao.setItems(nomesCampos);
        }

        panePreviewApresentacao.setVisible(true);
        listCamposApresentacao.getSelectionModel().selectFirst();
    }

    private void exibirCampoApresentacao(String nomeCampo) {
        if (nomeCampo == null) return;
        this.campoApresentacaoSelecionado = camposApresentacaoMap.get(nomeCampo);
        if (campoApresentacaoSelecionado == null) return;

        lblCampoTitulo.setText(nomeCampo);

        if (camposApresentacaoCache != null) {
            String markdownAtual = getApresentacaoMarkdown(camposApresentacaoCache, campoApresentacaoSelecionado);
            int statusAtual = getStatusFromApresentacaoCache(campoApresentacaoSelecionado);

            renderMarkdown(webApresentacao_Atual, markdownAtual);
            atualizarStatusLabel(lblCampoStatusApresentacao, statusAtual);
            carregarUltimoParecer(txtParecerApresentacao_Atual, "APRESENTACAO", campoApresentacaoSelecionado, versaoAtual, false); // <-- false
        }

        if (versaoAnteriorCorrigida != null && camposApresentacaoCache_Anterior != null) {
            String markdownAnterior = getApresentacaoMarkdown(camposApresentacaoCache_Anterior, campoApresentacaoSelecionado);
            renderMarkdown(webApresentacao_Anterior, markdownAnterior);
            carregarUltimoParecer(txtParecerApresentacao_Anterior, "APRESENTACAO", campoApresentacaoSelecionado, versaoAnteriorCorrigida, true); // <-- true
        }
    }

    @FXML private void onAprovarCampo() {
        atualizarStatusCampoApresentacao(1);
    }

    @FXML private void onReprovarCampo() {
        atualizarStatusCampoApresentacao(2);
    }

    private void atualizarStatusCampoApresentacao(int novoStatus) {
        if (campoApresentacaoSelecionado == null) return;
        try {
            String comentario = txtParecerApresentacao_Atual.getText();

            dao.atualizarStatusCampoApresentacao(trabalhoId, versaoAtual, campoApresentacaoSelecionado, novoStatus);
            dao.salvarParecer(trabalhoId, versaoAtual, orientadorId, "APRESENTACAO", campoApresentacaoSelecionado, novoStatus, comentario);

            atualizarStatusLabel(lblCampoStatusApresentacao, novoStatus);
            camposApresentacaoCache = dao.carregarCamposApresentacao(trabalhoId, versaoAtual);
            listCamposApresentacao.refresh();

            info("Parecer salvo com sucesso!");
        } catch (SQLException e) {
            erro("Falha ao salvar parecer do campo", e);
        }
    }

    private String getApresentacaoMarkdown(ApresentacaoCamposDTO cache, String coluna) {
        if (cache == null) return "";
        return switch (coluna) {
            case "nome_completo" -> cache.nomeCompleto();
            case "historico_academico" -> cache.historicoAcademico();
            case "motivacao_fatec" -> cache.motivacaoFatec();
            case "historico_profissional" -> cache.historicoProfissional();
            case "contatos_email" -> cache.contatosEmail();
            case "principais_conhecimentos" -> cache.principaisConhecimentos();
            default -> "";
        };
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

    private void carregarAbaApi(int apiIndex) throws SQLException {
        this.apiIndexAtual = apiIndex;

        // 1. Carrega dados da VERSÃO ATUAL
        camposApiCache = dao.carregarCamposApi(trabalhoId, versaoAtual, apiIndex);

        // 2. Carrega dados da VERSÃO ANTERIOR (se existir)
        ToggleButton toggle = getToggleApi(apiIndex);
        Label lblTituloAnterior = getLabelTituloAnteriorApi(apiIndex);

        if (versaoAnteriorCorrigida != null) {
            camposApiCache_Anterior = dao.carregarCamposApi(trabalhoId, versaoAnteriorCorrigida, apiIndex);
            if (lblTituloAnterior != null) lblTituloAnterior.setText("Versão Anterior (" + versaoAnteriorCorrigida + ")");
            if (toggle != null) toggle.setDisable(false);
        } else {
            if (toggle != null) {
                toggle.setDisable(true);
                toggle.setSelected(false);
            }
        }

        // 3. Preenche a lista da esquerda
        ListView<String> currentListView = getListViewApi(apiIndex);
        if (currentListView.getItems().isEmpty()) {
            ObservableList<String> nomesCampos = FXCollections.observableArrayList(camposApiMap.keySet());
            currentListView.setItems(nomesCampos);
        }

        VBox previewPane = getPanePreviewApi(apiIndex);
        if (previewPane != null) previewPane.setVisible(true);
        currentListView.getSelectionModel().selectFirst();
    }

    private void exibirCampoApi(String nomeCampo) {
        if (nomeCampo == null) return;
        this.campoApiSelecionado = camposApiMap.get(nomeCampo);
        if (campoApiSelecionado == null) return;

        Label currentTitulo = getLabelTituloApi(apiIndexAtual);
        Label currentStatus = getLabelStatusApi(apiIndexAtual);
        WebView currentWeb = getWebViewApi_Atual(apiIndexAtual);
        TextArea currentParecer = getTextAreaParecerApi_Atual(apiIndexAtual);

        if (currentTitulo == null) return;

        currentTitulo.setText(nomeCampo);

        if (camposApiCache != null) {
            String markdownAtual = getApiMarkdown(camposApiCache, campoApiSelecionado);
            int statusAtual = getStatusFromApiCache(campoApiSelecionado);

            renderMarkdown(currentWeb, markdownAtual);
            atualizarStatusLabel(currentStatus, statusAtual);
            carregarUltimoParecer(currentParecer, "API" + apiIndexAtual, campoApiSelecionado, versaoAtual, false); // <-- false
        }

        if (versaoAnteriorCorrigida != null && camposApiCache_Anterior != null) {
            WebView webAnterior = getWebViewApi_Anterior(apiIndexAtual);
            TextArea parecerAnterior = getTextAreaParecerApi_Anterior(apiIndexAtual);

            String markdownAnterior = getApiMarkdown(camposApiCache_Anterior, campoApiSelecionado);
            renderMarkdown(webAnterior, markdownAnterior);
            carregarUltimoParecer(parecerAnterior, "API" + apiIndexAtual, campoApiSelecionado, versaoAnteriorCorrigida, true); // <-- true
        }
    }

    @FXML private void onAprovarCampoApi() {
        atualizarStatusCampoApi(1);
    }

    @FXML private void onReprovarCampoApi() {
        atualizarStatusCampoApi(2);
    }

    private void atualizarStatusCampoApi(int novoStatus) {
        if (campoApiSelecionado == null) return;
        try {
            TextArea currentParecer = getTextAreaParecerApi_Atual(apiIndexAtual);
            String comentario = currentParecer.getText();
            String secao = "API" + apiIndexAtual;

            dao.atualizarStatusCampoApi(trabalhoId, versaoAtual, apiIndexAtual, campoApiSelecionado, novoStatus);
            dao.salvarParecer(trabalhoId, versaoAtual, orientadorId, secao, campoApiSelecionado, novoStatus, comentario);

            atualizarStatusLabel(getLabelStatusApi(apiIndexAtual), novoStatus);
            camposApiCache = dao.carregarCamposApi(trabalhoId, versaoAtual, apiIndexAtual);
            getListViewApi(apiIndexAtual).refresh();

            info("Parecer salvo com sucesso!");
        } catch (SQLException e) {
            erro("Falha ao atualizar status do campo de API", e);
        }
    }

    private String getApiMarkdown(ApiCamposDTO cache, String coluna) {
        if (cache == null) return "";
        return switch (coluna) {
            case "empresa_parceira" -> cache.empresaParceira();
            case "problema" -> cache.problema();
            case "solucao_resumo" -> cache.solucaoResumo();
            case "link_repositorio" -> cache.linkRepositorio();
            case "tecnologias" -> cache.tecnologias();
            case "contribuicoes" -> cache.contribuicoes();
            case "hard_skills" -> cache.hardSkills();
            case "soft_skills" -> cache.softSkills();
            default -> "";
        };
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

    private void carregarAbaResumo() throws SQLException {
        ResumoCamposDTO dtoAtual = dao.carregarCamposResumo(trabalhoId, versaoAtual);
        renderMarkdown(webResumo_Atual, dtoAtual.resumoMd());
        atualizarStatusLabel(lblResumoVersaoStatus, dtoAtual.versaoValidada());
        carregarUltimoParecer(txtParecerResumo_Atual, "RESUMO", "resumo_md", versaoAtual, false); // <-- false

        if (versaoAnteriorCorrigida != null) {
            ResumoCamposDTO dtoAnterior = dao.carregarCamposResumo(trabalhoId, versaoAnteriorCorrigida);
            lblTituloAnteriorResumo.setText("Versão Anterior (" + versaoAnteriorCorrigida + ")");
            renderMarkdown(webResumo_Anterior, dtoAnterior.resumoMd());
            carregarUltimoParecer(txtParecerResumo_Anterior, "RESUMO", "resumo_md", versaoAnteriorCorrigida, true); // <-- true
            btnToggleCorrecaoResumo.setDisable(false);
        } else {
            btnToggleCorrecaoResumo.setDisable(true);
            btnToggleCorrecaoResumo.setSelected(false);
        }
    }

    @FXML private void onAprovarResumo() {
        atualizarStatusResumo(1);
    }

    @FXML private void onReprovarResumo() {
        atualizarStatusResumo(2);
    }

    private void atualizarStatusResumo(int novoStatus) {
        try {
            String comentario = txtParecerResumo_Atual.getText();
            dao.atualizarStatusResumo(trabalhoId, versaoAtual, novoStatus);
            dao.salvarParecer(trabalhoId, versaoAtual, orientadorId, "RESUMO", "resumo_md", novoStatus, comentario);
            atualizarStatusLabel(lblResumoVersaoStatus, novoStatus);
            info("Parecer salvo com sucesso!");
        } catch (SQLException e) {
            erro("Falha ao atualizar status do Resumo", e);
        }
    }


    // --- Lógica da Aba Finais ---

    private void carregarAbaFinais() throws SQLException {
        // CORRIGIDO: Chamando o método sobrecarregado correto
        ApresentacaoCamposDTO dtoAtual = dao.carregarCamposApresentacao(trabalhoId, versaoAtual);
        renderMarkdown(webFinais_Atual, dtoAtual.consideracoesFinais());
        atualizarStatusLabel(lblFinaisStatus, dtoAtual.consideracoesFinaisStatus());
        carregarUltimoParecer(txtParecerFinais_Atual, "FINAIS", "consideracoes_finais", versaoAtual, false); // <-- false

        if (versaoAnteriorCorrigida != null) {
            // CORRIGIDO: Chamando o método sobrecarregado correto
            ApresentacaoCamposDTO dtoAnterior = dao.carregarCamposApresentacao(trabalhoId, versaoAnteriorCorrigida);
            lblTituloAnteriorFinais.setText("Versão Anterior (" + versaoAnteriorCorrigida + ")");
            renderMarkdown(webFinais_Anterior, dtoAnterior.consideracoesFinais());
            carregarUltimoParecer(txtParecerFinais_Anterior, "FINAIS", "consideracoes_finais", versaoAnteriorCorrigida, true); // <-- true
            btnToggleCorrecaoFinais.setDisable(false);
        } else {
            btnToggleCorrecaoFinais.setDisable(true);
            btnToggleCorrecaoFinais.setSelected(false);
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
            String comentario = txtParecerFinais_Atual.getText();
            String campoChave = "consideracoes_finais";

            dao.atualizarStatusCampoApresentacao(trabalhoId, versaoAtual, campoChave, novoStatus);
            dao.salvarParecer(trabalhoId, versaoAtual, orientadorId, "FINAIS", campoChave, novoStatus, comentario);

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
            return;
        }
        try {
            int pendencias = dao.countPendenciasByVersao(trabalhoId, versaoAtual);
            String novoStatus = (pendencias == 0) ? "APROVADO" : "REPROVADO";
            String msgSucesso = (pendencias == 0) ? "TG Aprovado! O aluno foi notificado." : "Devolutiva enviada com " + pendencias + " pendências. O aluno foi notificado.";

            try (Connection con = Database.get()) {
                tgDao.updateStatus(con, trabalhoId, novoStatus);
            }
            info(msgSucesso);
            fecharModal();
        } catch (Exception e) {
            erro("Falha grave ao finalizar a devolutiva.", e);
        }
    }


    // --- Métodos Utilitários ---

    private void carregarUltimoParecer(TextArea textArea, String secao, String campoChave, String versaoAlvo, boolean isReadOnly) {
        if (textArea == null) return;
        try {
            if (versaoAlvo == null) {
                textArea.setText("Não há versão anterior corrigida.");
                textArea.setDisable(true);
                return;
            }

            // Define se o campo é editável (será true para o painel "Anterior")
            textArea.setDisable(isReadOnly);

            Optional<String> parecer = dao.findUltimoParecer(trabalhoId, versaoAlvo, secao, campoChave);

            // --- ESTA É A CORREÇÃO ---
            if (isReadOnly) {
                // Se for o painel "Anterior", mostra a mensagem
                textArea.setText(parecer.orElse("Nenhum parecer registrado para esta versão."));
            } else {
                // Se for o painel "Atual" (o de digitação), deixa em branco
                textArea.setText(parecer.orElse(""));
            }
            // --- FIM DA CORREÇÃO ---

        } catch (SQLException e) {
            textArea.setText("Erro ao carregar parecer: " + e.getMessage());
        }
    }

    @FXML
    private void fecharModal() {
        Stage stage = (Stage) lblTituloModal.getScene().getWindow();
        stage.close();
    }

    private void renderMarkdown(WebView webView, String markdown) {
        if (webView == null) return;
        String md = (markdown == null) ? "" : markdown;

        // CORRIGIDO: O erro de sintaxe estava aqui.
        Node document = mdParser.parse(md);
        String htmlBody = mdRenderer.render(document);

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
            case 1 -> { label.setText("Aprovado"); label.getStyleClass().add("badge-ok"); }
            case 2 -> { label.setText("Reprovado"); label.getStyleClass().add("badge-reprovado"); }
            default -> { label.setText("Pendente"); label.getStyleClass().add("badge-pendente"); }
        }
    }

    // --- Helpers de Roteamento de API (Completos) ---

    private Tab getTabApi(int index) {
        return switch (index) {
            case 1 -> tabApi1; case 2 -> tabApi2; case 3 -> tabApi3;
            case 4 -> tabApi4; case 5 -> tabApi5; case 6 -> tabApi6;
            default -> null;
        };
    }
    private ListView<String> getListViewApi(int index) {
        return switch (index) {
            case 1 -> listCamposApi1; case 2 -> listCamposApi2; case 3 -> listCamposApi3;
            case 4 -> listCamposApi4; case 5 -> listCamposApi5; case 6 -> listCamposApi6;
            default -> null;
        };
    }
    private VBox getPanePreviewApi(int index) {
        return switch (index) {
            case 1 -> panePreviewApi1; case 2 -> panePreviewApi2; case 3 -> panePreviewApi3;
            case 4 -> panePreviewApi4; case 5 -> panePreviewApi5; case 6 -> panePreviewApi6;
            default -> null;
        };
    }
    private Label getLabelTituloApi(int index) {
        return switch (index) {
            case 1 -> lblCampoTituloApi1; case 2 -> lblCampoTituloApi2; case 3 -> lblCampoTituloApi3;
            case 4 -> lblCampoTituloApi4; case 5 -> lblCampoTituloApi5; case 6 -> lblCampoTituloApi6;
            default -> null;
        };
    }
    private Label getLabelStatusApi(int index) {
        return switch (index) {
            case 1 -> lblCampoStatusApi1; case 2 -> lblCampoStatusApi2; case 3 -> lblCampoStatusApi3;
            case 4 -> lblCampoStatusApi4; case 5 -> lblCampoStatusApi5; case 6 -> lblCampoStatusApi6;
            default -> null;
        };
    }
    private WebView getWebViewApi_Atual(int index) {
        return switch (index) {
            case 1 -> webApi1_Atual; case 2 -> webApi2_Atual; case 3 -> webApi3_Atual;
            case 4 -> webApi4_Atual; case 5 -> webApi5_Atual; case 6 -> webApi6_Atual;
            default -> null;
        };
    }
    private WebView getWebViewApi_Anterior(int index) {
        return switch (index) {
            case 1 -> webApi1_Anterior; case 2 -> webApi2_Anterior; case 3 -> webApi3_Anterior;
            case 4 -> webApi4_Anterior; case 5 -> webApi5_Anterior; case 6 -> webApi6_Anterior;
            default -> null;
        };
    }
    private TextArea getTextAreaParecerApi_Atual(int index) {
        return switch (index) {
            case 1 -> txtParecerApi1_Atual; case 2 -> txtParecerApi2_Atual; case 3 -> txtParecerApi3_Atual;
            case 4 -> txtParecerApi4_Atual; case 5 -> txtParecerApi5_Atual; case 6 -> txtParecerApi6_Atual;
            default -> null;
        };
    }
    private TextArea getTextAreaParecerApi_Anterior(int index) {
        return switch (index) {
            case 1 -> txtParecerApi1_Anterior; case 2 -> txtParecerApi2_Anterior; case 3 -> txtParecerApi3_Anterior;
            case 4 -> txtParecerApi4_Anterior; case 5 -> txtParecerApi5_Anterior; case 6 -> txtParecerApi6_Anterior;
            default -> null;
        };
    }
    private ToggleButton getToggleApi(int index) {
        return switch (index) {
            case 1 -> btnToggleCorrecaoApi1; case 2 -> btnToggleCorrecaoApi2; case 3 -> btnToggleCorrecaoApi3;
            case 4 -> btnToggleCorrecaoApi4; case 5 -> btnToggleCorrecaoApi5; case 6 -> btnToggleCorrecaoApi6;
            default -> null;
        };
    }
    private Label getLabelTituloAnteriorApi(int index) {
        return switch (index) {
            case 1 -> lblTituloAnteriorApi1; case 2 -> lblTituloAnteriorApi2; case 3 -> lblTituloAnteriorApi3;
            case 4 -> lblTituloAnteriorApi4; case 5 -> lblTituloAnteriorApi5; case 6 -> lblTituloAnteriorApi6;
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
    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}