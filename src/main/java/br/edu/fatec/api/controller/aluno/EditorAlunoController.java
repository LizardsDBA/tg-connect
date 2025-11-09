package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.dao.JdbcFeedbackDao;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.controller.BaseController;
import java.util.*;
import java.util.stream.Collectors;

// imports
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.service.EditorAlunoService;
import br.edu.fatec.api.controller.orientador.ModalPreviewController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.Priority; // Import que faltava

public class EditorAlunoController extends BaseController {

    // ... (Cole TODOS os seus @FXMLs aqui) ...
    @FXML private VBox commentsSection;
    @FXML private TabPane tabPane;
    @FXML private Label lbStatusAba;
    @FXML private TextArea taInfoPessoais, taHistoricoAcad, taMotivacao, taHistoricoProf, taContatos, taConhecimentos;
    @FXML private Label lblInfoPessoaisStatus, lblHistoricoAcadStatus, lblMotivacaoStatus, lblHistoricoProfStatus, lblContatosStatus, lblConhecimentosStatus;
    @FXML private TextField tfApi1Empresa, tfApi2Empresa, tfApi3Empresa, tfApi4Empresa, tfApi5Empresa, tfApi6Empresa;
    @FXML private TextArea taApi1Problema, taApi1Solucao, taApi1Tecnologias, taApi1Contrib, taApi1Hard, taApi1Soft;
    @FXML private TextField tfApi1Repo;
    @FXML private TextArea taApi2Problema, taApi2Solucao, taApi2Tecnologias, taApi2Contrib, taApi2Hard, taApi2Soft;
    @FXML private TextField tfApi2Repo;
    @FXML private TextArea taApi3Problema, taApi3Solucao, taApi3Tecnologias, taApi3Contrib, taApi3Hard, taApi3Soft;
    @FXML private TextField tfApi3Repo;
    @FXML private TextArea taApi4Problema, taApi4Solucao, taApi4Tecnologias, taApi4Contrib, taApi4Hard, taApi4Soft;
    @FXML private TextField tfApi4Repo;
    @FXML private TextArea taApi5Problema, taApi5Solucao, taApi5Tecnologias, taApi5Contrib, taApi5Hard, taApi5Soft;
    @FXML private TextField tfApi5Repo;
    @FXML private TextArea taApi6Problema, taApi6Solucao, taApi6Tecnologias, taApi6Contrib, taApi6Hard, taApi6Soft;
    @FXML private TextField tfApi6Repo;
    @FXML private TextField tfSem1, tfSem2, tfSem3, tfSem4, tfSem5, tfSem6;
    @FXML private TextField tfEmp1, tfEmp2, tfEmp3, tfEmp4, tfEmp5, tfEmp6;
    @FXML private TextArea taSol1, taSol2, taSol3, taSol4, taSol5, taSol6;
    @FXML private TextArea taConclusoes;

    // --- Estado ---
    private TextInputControl focusedTextInput;
    @FXML private Button btnSalvarTudo;
    @FXML private Button btnSolicitarRevisao; // NOVO
    private final EditorAlunoService service = new EditorAlunoService();
    private final JdbcFeedbackDao feedbackDao = new JdbcFeedbackDao();
    private final Map<String, CampoInfo> campoMap = new HashMap<>();
    private long trabalhoId;
    private String versaoAtual;
    private String statusAtualDoFluxo; // NOVO: Guarda o status (EM_ANDAMENTO, ENTREGUE, etc.)

    // NOVO: Lista de todos os campos editáveis
    private final List<TextInputControl> todosOsCamposEditaveis = new ArrayList<>();


    // ====== Navegação (inalterada) ======
    // ... (Cole seus métodos de navegação goHome, logout, etc. aqui) ...
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
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }


    // ====== Ciclo de vida ======
    @FXML
    public void initialize() {
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        mapearCampos();
        coletarCamposEditaveis(); // NOVO: Coloca todos os campos na lista

        onReady();
        hookFocusHandlers();
        applyTips();
        wireTabStatus();
    }

    // NOVO: Coleta todos os TextInputs em uma lista para fácil habilitação/desabilitação
    private void coletarCamposEditaveis() {
        // Aba 1
        todosOsCamposEditaveis.addAll(List.of(
                taInfoPessoais, taHistoricoAcad, taMotivacao, taHistoricoProf, taContatos, taConhecimentos
        ));
        // Abas 2-7
        todosOsCamposEditaveis.addAll(List.of(
                tfApi1Empresa, taApi1Problema, taApi1Solucao, tfApi1Repo, taApi1Tecnologias, taApi1Contrib, taApi1Hard, taApi1Soft,
                tfApi2Empresa, taApi2Problema, taApi2Solucao, tfApi2Repo, taApi2Tecnologias, taApi2Contrib, taApi2Hard, taApi2Soft,
                tfApi3Empresa, taApi3Problema, taApi3Solucao, tfApi3Repo, taApi3Tecnologias, taApi3Contrib, taApi3Hard, taApi3Soft,
                tfApi4Empresa, taApi4Problema, taApi4Solucao, tfApi4Repo, taApi4Tecnologias, taApi4Contrib, taApi4Hard, taApi4Soft,
                tfApi5Empresa, taApi5Problema, taApi5Solucao, tfApi5Repo, taApi5Tecnologias, taApi5Contrib, taApi5Hard, taApi5Soft,
                tfApi6Empresa, taApi6Problema, taApi6Solucao, tfApi6Repo, taApi6Tecnologias, taApi6Contrib, taApi6Hard, taApi6Soft
        ));
        // Aba 8
        todosOsCamposEditaveis.addAll(List.of(
                tfSem1, tfEmp1, taSol1, tfSem2, tfEmp2, taSol2, tfSem3, tfEmp3, taSol3,
                tfSem4, tfEmp4, taSol4, tfSem5, tfEmp5, taSol5, tfSem6, tfEmp6, taSol6
        ));
        // Aba 9
        todosOsCamposEditaveis.add(taConclusoes);
    }

    // NOVO: Trava ou destrava a UI com base no status do fluxo
    private void atualizarTravaEdicao(String status) {
        boolean camposHabilitados = false;
        boolean podeSalvar = false;
        boolean podeSolicitar = false;

        switch (status) {
            case "EM_ANDAMENTO", "REPROVADO" -> {
                // Aluno pode editar e salvar
                camposHabilitados = true;
                podeSalvar = true;
                podeSolicitar = true;
            }
            case "ENTREGUE" -> {
                // Aluno entregou, aguardando orientador. Não pode editar.
                camposHabilitados = false;
                podeSalvar = false;
                podeSolicitar = false;
            }
            case "APROVADO" -> {
                // Trabalho concluído. Não pode editar.
                camposHabilitados = false;
                podeSalvar = false;
                podeSolicitar = false;
            }
        }

        // Aplica a trava em todos os campos
        for (TextInputControl campo : todosOsCamposEditaveis) {
            if (campo != null) {
                campo.setDisable(!camposHabilitados);
            }
        }

        // Trava os botões
        if (btnSalvarTudo != null) btnSalvarTudo.setDisable(!podeSalvar);
        if (btnSolicitarRevisao != null) btnSolicitarRevisao.setDisable(!podeSolicitar);
    }

    // ... (Métodos wireTabStatus, refreshTabStatus, hookFocusHandlers, carregarParecerDoCampo, etc. - Sem alterações) ...
    private void mapearCampos() {
        // Aba 1
        campoMap.put("taInfoPessoais", new CampoInfo("APRESENTACAO", "nome_completo"));
        campoMap.put("taHistoricoAcad", new CampoInfo("APRESENTACAO", "historico_academico"));
        campoMap.put("taMotivacao", new CampoInfo("APRESENTACAO", "motivacao_fatec"));
        campoMap.put("taHistoricoProf", new CampoInfo("APRESENTACAO", "historico_profissional"));
        campoMap.put("taContatos", new CampoInfo("APRESENTACAO", "contatos_email"));
        campoMap.put("taConhecimentos", new CampoInfo("APRESENTACAO", "principais_conhecimentos"));
        // Aba 9
        campoMap.put("taConclusoes", new CampoInfo("FINAIS", "consideracoes_finais"));
        // Aba 8 (Resumo)
        String[][] camposResumo = {
                {"tfSem1", "tfEmp1", "taSol1"}, {"tfSem2", "tfEmp2", "taSol2"}, {"tfSem3", "tfEmp3", "taSol3"},
                {"tfSem4", "tfEmp4", "taSol4"}, {"tfSem5", "tfEmp5", "taSol5"}, {"tfSem6", "tfEmp6", "taSol6"}
        };
        for (String[] linha : camposResumo) {
            for (String id : linha) {
                campoMap.put(id, new CampoInfo("RESUMO", "resumo_md"));
            }
        }
        // Abas 2-7 (APIs)
        for (int i = 1; i <= 6; i++) {
            campoMap.put("tfApi" + i + "Empresa", new CampoInfo("API" + i, "empresa_parceira"));
            campoMap.put("taApi" + i + "Problema", new CampoInfo("API" + i, "problema"));
            campoMap.put("taApi" + i + "Solucao", new CampoInfo("API" + i, "solucao_resumo"));
            campoMap.put("tfApi" + i + "Repo", new CampoInfo("API" + i, "link_repositorio"));
            campoMap.put("taApi" + i + "Tecnologias", new CampoInfo("API" + i, "tecnologias"));
            campoMap.put("taApi" + i + "Contrib", new CampoInfo("API" + i, "contribuicoes"));
            campoMap.put("taApi" + i + "Hard", new CampoInfo("API" + i, "hard_skills"));
            campoMap.put("taApi" + i + "Soft", new CampoInfo("API" + i, "soft_skills"));
        }
    }
    private record CampoInfo(String secao, String campoChave) {}
    private void wireTabStatus(){
        if (tabPane == null) return;
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) refreshTabStatus(newV.intValue());
        });
        refreshTabStatus(tabPane.getSelectionModel().getSelectedIndex());
    }
    private void refreshTabStatus(int idx){
        if (lbStatusAba == null) return;
        int abaNumero = idx + 1; // 1..9
        String statusTxt = "Pendente Validação";
        try {
            boolean validada = service.isAbaValidada(trabalhoId, abaNumero);
            statusTxt = validada ? "Concluída" : "Pendente Validação";
        } catch (Exception ignore){ /* mantém default */ }
        lbStatusAba.setText("Aba " + abaNumero + " - Status: " + statusTxt);
        lbStatusAba.getStyleClass().removeAll("badge-ok","badge-pendente");
        lbStatusAba.getStyleClass().add(statusTxt.equals("Concluída") ? "badge-ok" : "badge-pendente");
    }
    private void hookFocusHandlers() {
        tabPane.addEventFilter(MouseEvent.MOUSE_PRESSED, evt -> {
            Node n = evt.getPickResult().getIntersectedNode();
            TextInputControl campoFocado = null;
            while (n != null) {
                if (n instanceof TextInputControl) {
                    campoFocado = (TextInputControl) n;
                    break;
                }
                n = n.getParent();
            }
            focusedTextInput = campoFocado;
            if (campoFocado != null) {
                carregarParecerDoCampo(campoFocado);
            }
        });
    }
    private void carregarParecerDoCampo(TextInputControl campo) {
        if (commentsSection == null || campo == null || campo.getId() == null) {
            return;
        }
        commentsSection.getChildren().clear();
        CampoInfo info = campoMap.get(campo.getId());
        if (info == null) {
            commentsSection.getChildren().add(new Label("Comentários do Orientador (Aba)"));
            return;
        }
        try {
            Optional<String> parecer = feedbackDao.findUltimoParecer(this.trabalhoId, this.versaoAtual, info.secao(), info.campoChave());
            Label titulo = new Label("Feedback: " + info.secao() + " / " + info.campoChave());
            titulo.setStyle("-fx-font-weight: bold;");
            commentsSection.getChildren().add(titulo);
            if (parecer.isPresent() && !parecer.get().isBlank()) {
                TextArea parecerTexto = new TextArea(parecer.get());
                parecerTexto.setEditable(false);
                parecerTexto.setWrapText(true);
                parecerTexto.getStyleClass().add("msg-peer");
                VBox.setVgrow(parecerTexto, Priority.ALWAYS);
                commentsSection.getChildren().add(parecerTexto);
            } else {
                Label semParecer = new Label("Nenhum parecer específico do orientador para este campo.");
                semParecer.setWrapText(true);
                commentsSection.getChildren().add(semParecer);
            }
        } catch (Exception e) {
            commentsSection.getChildren().add(new Label("Erro ao carregar parecer: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    private void applyTips() {
        if (taApi1Problema != null) taApi1Problema.setTooltip(new Tooltip("Descreva o problema (mín. 3 linhas)."));
        if (taApi1Solucao != null) taApi1Solucao.setTooltip(new Tooltip("Explique a solução (≈5 linhas; tipo do sistema)."));
        if (tfApi1Repo != null) tfApi1Repo.setTooltip(new Tooltip("URL do repositório no GitHub."));
    }
    private void insertAtCaret(String text){
        TextInputControl target = (focusedTextInput != null) ? focusedTextInput : getFirstVisibleTextInput();
        if (target == null) return;
        replaceSelection(target, text);
    }
    private void replaceSelection(TextInputControl target, String text){
        var i = target.getSelection();
        target.replaceText(i.getStart(), i.getEnd(), text);
        target.positionCaret(i.getStart() + text.length());
    }
    private TextInputControl getFirstVisibleTextInput(){
        Node content = tabPane.getSelectionModel().getSelectedItem().getContent();
        return findFirst(content);
    }
    private TextInputControl findFirst(Node n){
        if (n instanceof TextInputControl tic) return tic;
        if (n instanceof Parent p){
            for (Node c : p.getChildrenUnmodifiable()){
                TextInputControl r = findFirst(c);
                if (r != null) return r;
            }
        }
        return null;
    }
    private void insertAroundBold(String wrapper){
        TextInputControl target = (focusedTextInput != null) ? focusedTextInput : getFirstVisibleTextInput();
        if (target == null) return;
        String sel = target.getSelectedText();
        if (sel == null || sel.isEmpty()) sel = "Negrito";
        replaceSelection(target, wrapper + sel + wrapper);
    }
    private void insertAroundItalic(String wrapper){
        TextInputControl target = (focusedTextInput != null) ? focusedTextInput : getFirstVisibleTextInput();
        if (target == null) return;
        String sel = target.getSelectedText();
        if (sel == null || sel.isEmpty()) sel = "Italico";
        replaceSelection(target, wrapper + sel + wrapper);
    }
    private void insertAroundUnderline(String wrapper){
        TextInputControl target = (focusedTextInput != null) ? focusedTextInput : getFirstVisibleTextInput();
        if (target == null) return;
        String sel = target.getSelectedText();
        if (sel == null || sel.isEmpty()) sel = "Sublinhado";
        replaceSelection(target, wrapper + sel + wrapper);
    }
    public void toggleBold(){ insertAroundBold("**"); }
    public void toggleItalic(){ insertAroundItalic("*"); }
    public void toggleUnderline(){ insertAroundUnderline("__"); }
    public void insertH1(){ insertAtCaret("\n# Título\n"); }
    public void insertLink(){ insertAtCaret("[Texto](https://)"); }
    public void preview() {
        // ... (seu método preview() está OK) ...
        String mdCompleto = montarMarkdownCompleto();
        try {
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Preview do TG Completo");
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/views/orientador/ModalPreview.fxml"));
            Parent root = loader.load();
            ModalPreviewController controller = loader.getController();
            controller.initData(mdCompleto);
            Scene scene = new Scene(root);
            modalStage.setScene(scene);
            modalStage.showAndWait();
        } catch (Exception e) {
            erro("Falha ao abrir o modal de preview.", e);
        }
    }
    private void erro(String msg, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Ops!");
        a.setContentText(msg + (e != null ? "\n\n" + e.getMessage() : ""));
        a.showAndWait();
        if (e != null) e.printStackTrace();
    }
    public void salvarTudo() {
        // ... (seu método salvarTudo() está OK) ...
        try {
            long trabalhoId = service.resolveTrabalhoIdDoAlunoLogado();
            EditorAlunoService.DadosEditor d = new EditorAlunoService.DadosEditor();
            d.infoPessoais   = val(taInfoPessoais);
            d.historicoAcad  = val(taHistoricoAcad);
            d.motivacao      = val(taMotivacao);
            d.historicoProf  = val(taHistoricoProf);
            d.contatos       = val(taContatos);
            d.conhecimentos  = val(taConhecimentos);
            d.api1Empresa=val(tfApi1Empresa); d.api1Problema=val(taApi1Problema); d.api1Solucao=val(taApi1Solucao); d.api1Repo=val(tfApi1Repo);
            d.api1Tecnologias=val(taApi1Tecnologias); d.api1Contrib=val(taApi1Contrib); d.api1Hard=val(taApi1Hard); d.api1Soft=val(taApi1Soft);
            d.api2Empresa=val(tfApi2Empresa); d.api2Problema=val(taApi2Problema); d.api2Solucao=val(taApi2Solucao); d.api2Repo=val(tfApi2Repo);
            d.api2Tecnologias=val(taApi2Tecnologias); d.api2Contrib=val(taApi2Contrib); d.api2Hard=val(taApi2Hard); d.api2Soft=val(taApi2Soft);
            d.api3Empresa=val(tfApi3Empresa); d.api3Problema=val(taApi3Problema); d.api3Solucao=val(taApi3Solucao); d.api3Repo=val(tfApi3Repo);
            d.api3Tecnologias=val(taApi3Tecnologias); d.api3Contrib=val(taApi3Contrib); d.api3Hard=val(taApi3Hard); d.api3Soft=val(taApi3Soft);
            d.api4Empresa=val(tfApi4Empresa); d.api4Problema=val(taApi4Problema); d.api4Solucao=val(taApi4Solucao); d.api4Repo=val(tfApi4Repo);
            d.api4Tecnologias=val(taApi4Tecnologias); d.api4Contrib=val(taApi4Contrib); d.api4Hard=val(taApi4Hard); d.api4Soft=val(taApi4Soft);
            d.api5Empresa=val(tfApi5Empresa); d.api5Problema=val(taApi5Problema); d.api5Solucao=val(taApi5Solucao); d.api5Repo=val(tfApi5Repo);
            d.api5Tecnologias=val(taApi5Tecnologias); d.api5Contrib=val(taApi5Contrib); d.api5Hard=val(taApi5Hard); d.api5Soft=val(taApi5Soft);
            d.api6Empresa=val(tfApi6Empresa); d.api6Problema=val(taApi6Problema); d.api6Solucao=val(taApi6Solucao); d.api6Repo=val(tfApi6Repo);
            d.api6Tecnologias=val(taApi6Tecnologias); d.api6Contrib=val(taApi6Contrib); d.api6Hard=val(taApi6Hard); d.api6Soft=val(taApi6Soft);
            d.resumoMd = montarMdResumo();
            d.consideracoesFinais = val(taConclusoes);
            d.mdCompleto = montarMarkdownCompleto();
            String novaVersao = service.salvarTudo(trabalhoId, d);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Salvo com sucesso!");
            ok.setContentText("Nova versão: " + novaVersao);
            ok.showAndWait();
            onReady();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText("Falha ao salvar");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    /**
     * NOVO: Handler para o botão "Solicitar Revisão"
     */
    @FXML
    private void handleSolicitarRevisao() {
        // Confirmação
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Envio");
        confirm.setHeaderText("Solicitar Revisão do Orientador");
        confirm.setContentText("Você tem certeza que deseja enviar esta versão para revisão?\n\nApós o envio, você não poderá editar o trabalho até receber a devolutiva.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // 1. Chama o Service para mudar o status para 'ENTREGUE'
                service.solicitarRevisao(this.trabalhoId);

                // 2. Mostra sucesso
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setHeaderText("Enviado com Sucesso!");
                info.setContentText("Sua versão (" + this.versaoAtual + ") foi enviada para o orientador.");
                info.showAndWait();

                // 3. Recarrega a tela (que agora estará travada)
                onReady();

            } catch (Exception e) {
                erro("Falha ao solicitar revisão.", e);
            }
        }
    }


    // ... (Cole aqui: montarMdAba1, montarMdProjeto, montarMdResumo, montarMdConclusoes, montarMarkdownCompleto) ...
    // ... (Cole aqui: preencherResumoAPartirDoMarkdown, extrairLinhasTabela) ...
    // ... (Cole aqui: utils (val, safe, alertWarn, appendIfNotEmpty)) ...
    private String montarMdAba1(){
        StringBuilder sb = new StringBuilder();
        sb.append("# APRESENTAÇÃO DO ALUNO\n\n");
        appendIfNotEmpty(sb, "## Informações Pessoais", taInfoPessoais);
        appendIfNotEmpty(sb, "## Histórico Acadêmico", taHistoricoAcad);
        appendIfNotEmpty(sb, "## Motivação para Ingressar na FATEC", taMotivacao);
        appendIfNotEmpty(sb, "## Histórico Profissional", taHistoricoProf);
        appendIfNotEmpty(sb, "## Contatos", taContatos);
        appendIfNotEmpty(sb, "## Principais Conhecimentos", taConhecimentos);
        return sb.toString();
    }
    private String montarMdProjeto(int semestre){
        String titulo = switch (semestre){
            case 1 -> "## API 1º Semestre";
            case 2 -> "## API 2º Semestre";
            case 3 -> "## API 3º Semestre";
            case 4 -> "## API 4º Semestre";
            case 5 -> "## API 5º Semestre";
            case 6 -> "## API 6º Semestre";
            default -> "## API";
        };
        String empresa = "", problema = "", solucao = "", repo = "", tecnologias = "", contrib = "", hard = "", soft = "";
        switch (semestre){
            case 1 -> { empresa = val(tfApi1Empresa); problema = val(taApi1Problema); solucao = val(taApi1Solucao);
                repo = val(tfApi1Repo); tecnologias = val(taApi1Tecnologias); contrib = val(taApi1Contrib);
                hard = val(taApi1Hard); soft = val(taApi1Soft); }
            case 2 -> { empresa = val(tfApi2Empresa); problema = val(taApi2Problema); solucao = val(taApi2Solucao);
                repo = val(tfApi2Repo); tecnologias = val(taApi2Tecnologias); contrib = val(taApi2Contrib);
                hard = val(taApi2Hard); soft = val(taApi2Soft); }
            case 3 -> { empresa = val(tfApi3Empresa); problema = val(taApi3Problema); solucao = val(taApi3Solucao);
                repo = val(tfApi3Repo); tecnologias = val(taApi3Tecnologias); contrib = val(taApi3Contrib);
                hard = val(taApi3Hard); soft = val(taApi3Soft); }
            case 4 -> { empresa = val(tfApi4Empresa); problema = val(taApi4Problema); solucao = val(taApi4Solucao);
                repo = val(tfApi4Repo); tecnologias = val(taApi4Tecnologias); contrib = val(taApi4Contrib);
                hard = val(taApi4Hard); soft = val(taApi4Soft); }
            case 5 -> { empresa = val(tfApi5Empresa); problema = val(taApi5Problema); solucao = val(taApi5Solucao);
                repo = val(tfApi5Repo); tecnologias = val(taApi5Tecnologias); contrib = val(taApi5Contrib);
                hard = val(taApi5Hard); soft = val(taApi5Soft); }
            case 6 -> { empresa = val(tfApi6Empresa); problema = val(taApi6Problema); solucao = val(taApi6Solucao);
                repo = val(tfApi6Repo); tecnologias = val(taApi6Tecnologias); contrib = val(taApi6Contrib);
                hard = val(taApi6Hard); soft = val(taApi6Soft); }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(titulo).append("\n\n");
        if (!empresa.isBlank()) sb.append("**Empresa Parceira:** ").append(empresa).append("\n\n");
        if (!problema.isBlank()) sb.append("### Problema\n").append(problema).append("\n\n");
        if (!solucao.isBlank()) sb.append("### Solução\n").append(solucao).append("\n\n");
        if (!repo.isBlank()) sb.append("**Repositório:** ").append(repo).append("\n\n");
        if (!tecnologias.isBlank()) sb.append("### Tecnologias\n").append(tecnologias).append("\n\n");
        if (!contrib.isBlank()) sb.append("### Contribuições Pessoais\n").append(contrib).append("\n\n");
        if (!hard.isBlank()) sb.append("### Hard Skills\n").append(hard).append("\n\n");
        if (!soft.isBlank()) sb.append("### Soft Skills\n").append(soft).append("\n\n");
        return sb.toString();
    }
    private String montarMdResumo(){
        String header = "## Tabela Resumo dos Projetos API\n\n"
                + "| Semestre | Empresa Parceira | Solução Desenvolvida |\n|---|---|---|\n";
        if (tfSem1 != null){
            String[][] rows = {
                    {val(tfSem1), val(tfEmp1), val(taSol1)},
                    {val(tfSem2), val(tfEmp2), val(taSol2)},
                    {val(tfSem3), val(tfEmp3), val(taSol3)},
                    {val(tfSem4), val(tfEmp4), val(taSol4)},
                    {val(tfSem5), val(tfEmp5), val(taSol5)},
                    {val(tfSem6), val(tfEmp6), val(taSol6)}
            };
            String corpo = Arrays.stream(rows)
                    .map(r -> "| " + safe(r[0]) + " | " + safe(r[1]) + " | " + safe(r[2]) + " |")
                    .collect(Collectors.joining("\n"));
            return header + corpo + "\n\n";
        }
        return header + "|  |  |  |\n|  |  |  |\n|  |  |  |\n|  |  |  |\n|  |  |  |\n|  |  |  |\n\n";
    }
    private String montarMdConclusoes(){
        return "## Considerações Finais\n\n" + val(taConclusoes) + "\n\n";
    }
    private String montarMarkdownCompleto(){
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 9; i++){
            sb.append(switch (i){
                case 1 -> montarMdAba1();
                case 2 -> montarMdProjeto(1);
                case 3 -> montarMdProjeto(2);
                case 4 -> montarMdProjeto(3);
                case 5 -> montarMdProjeto(4);
                case 6 -> montarMdProjeto(5);
                case 7 -> montarMdProjeto(6);
                case 8 -> montarMdResumo();
                case 9 -> montarMdConclusoes();
                default -> "";
            });
        }
        return sb.toString();
    }
    private void preencherResumoAPartirDoMarkdown(String md) {
        List<String[]> linhas = extrairLinhasTabela(md);
        TextField[] sems = {tfSem1, tfSem2, tfSem3, tfSem4, tfSem5, tfSem6};
        TextField[] emps = {tfEmp1, tfEmp2, tfEmp3, tfEmp4, tfEmp5, tfEmp6};
        TextArea[] sols = {taSol1, taSol2, taSol3, taSol4, taSol5, taSol6};
        for(int i=0; i<6; i++) {
            if(sems[i]!=null) sems[i].clear();
            if(emps[i]!=null) emps[i].clear();
            if(sols[i]!=null) sols[i].clear();
        }
        int linha = 0;
        for (String[] cols : linhas) {
            if (cols.length < 3) continue;
            if (linha < 6) {
                if (sems[linha]!=null) sems[linha].setText(cols[0]);
                if (emps[linha]!=null) emps[linha].setText(cols[1]);
                if (sols[linha]!=null) sols[linha].setText(cols[2]);
            }
            linha++;
        }
    }


    /**
     * ATUALIZADO: Carrega dados E status (Badges e Bordas) e aplica a trava de edição
     */
    public void onReady() {
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        try {
            this.trabalhoId = service.resolveTrabalhoIdDoAlunoLogado();
            // Pega a versão e status atual
            EditorAlunoService.TrabalhoInfo info = service.fetchTrabalhoInfo(trabalhoId)
                    .orElse(new EditorAlunoService.TrabalhoInfo("v1", "EM_ANDAMENTO"));

            this.versaoAtual = info.versao();
            this.statusAtualDoFluxo = info.status();

            service.carregarTudo(trabalhoId).ifPresent(d -> {

                // Aba 1 - Apresentação (com Badges)
                if (taInfoPessoais != null) { taInfoPessoais.setText(d.infoPessoais); atualizarStatusLabel(lblInfoPessoaisStatus, d.infoPessoaisStatus); }
                if (taHistoricoAcad != null) { taHistoricoAcad.setText(d.historicoAcad); atualizarStatusLabel(lblHistoricoAcadStatus, d.historicoAcadStatus); }
                if (taMotivacao != null) { taMotivacao.setText(d.motivacao); atualizarStatusLabel(lblMotivacaoStatus, d.motivacaoStatus); }
                if (taHistoricoProf != null) { taHistoricoProf.setText(d.historicoProf); atualizarStatusLabel(lblHistoricoProfStatus, d.historicoProfStatus); }
                if (taContatos != null) { taContatos.setText(d.contatos); atualizarStatusLabel(lblContatosStatus, d.contatosStatus); }
                if (taConhecimentos != null) { taConhecimentos.setText(d.conhecimentos); atualizarStatusLabel(lblConhecimentosStatus, d.conhecimentosStatus); }
                // Aba 9 (Considerações) usa borda
                if (taConclusoes != null) { taConclusoes.setText(d.consideracoes); atualizarEstiloCampo(taConclusoes, d.consideracoesStatus); }

                // Abas 2-7 - APIs (com Bordas)
                // (Cole aqui o código de preenchimento das APIs 1-6 que já corrigimos)
                // API 1
                if (tfApi1Empresa != null) { tfApi1Empresa.setText(d.api1Empresa); atualizarEstiloCampo(tfApi1Empresa, d.api1EmpresaStatus); }
                if (taApi1Problema != null) { taApi1Problema.setText(d.api1Problema); atualizarEstiloCampo(taApi1Problema, d.api1ProblemaStatus); }
                if (taApi1Solucao != null) { taApi1Solucao.setText(d.api1Solucao); atualizarEstiloCampo(taApi1Solucao, d.api1SolucaoStatus); }
                if (tfApi1Repo != null) { tfApi1Repo.setText(d.api1Repo); atualizarEstiloCampo(tfApi1Repo, d.api1RepoStatus); }
                if (taApi1Tecnologias != null) { taApi1Tecnologias.setText(d.api1Tecnologias); atualizarEstiloCampo(taApi1Tecnologias, d.api1TecnologiasStatus); }
                if (taApi1Contrib != null) { taApi1Contrib.setText(d.api1Contrib); atualizarEstiloCampo(taApi1Contrib, d.api1ContribStatus); }
                if (taApi1Hard != null) { taApi1Hard.setText(d.api1Hard); atualizarEstiloCampo(taApi1Hard, d.api1HardStatus); }
                if (taApi1Soft != null) { taApi1Soft.setText(d.api1Soft); atualizarEstiloCampo(taApi1Soft, d.api1SoftStatus); }
                // API 2
                if (tfApi2Empresa != null) { tfApi2Empresa.setText(d.api2Empresa); atualizarEstiloCampo(tfApi2Empresa, d.api2EmpresaStatus); }
                if (taApi2Problema != null) { taApi2Problema.setText(d.api2Problema); atualizarEstiloCampo(taApi2Problema, d.api2ProblemaStatus); }
                if (taApi2Solucao != null) { taApi2Solucao.setText(d.api2Solucao); atualizarEstiloCampo(taApi2Solucao, d.api2SolucaoStatus); }
                if (tfApi2Repo != null) { tfApi2Repo.setText(d.api2Repo); atualizarEstiloCampo(tfApi2Repo, d.api2RepoStatus); }
                if (taApi2Tecnologias != null) { taApi2Tecnologias.setText(d.api2Tecnologias); atualizarEstiloCampo(taApi2Tecnologias, d.api2TecnologiasStatus); }
                if (taApi2Contrib != null) { taApi2Contrib.setText(d.api2Contrib); atualizarEstiloCampo(taApi2Contrib, d.api2ContribStatus); }
                if (taApi2Hard != null) { taApi2Hard.setText(d.api2Hard); atualizarEstiloCampo(taApi2Hard, d.api2HardStatus); }
                if (taApi2Soft != null) { taApi2Soft.setText(d.api2Soft); atualizarEstiloCampo(taApi2Soft, d.api2SoftStatus); }
                // API 3
                if (tfApi3Empresa != null) { tfApi3Empresa.setText(d.api3Empresa); atualizarEstiloCampo(tfApi3Empresa, d.api3EmpresaStatus); }
                if (taApi3Problema != null) { taApi3Problema.setText(d.api3Problema); atualizarEstiloCampo(taApi3Problema, d.api3ProblemaStatus); }
                if (taApi3Solucao != null) { taApi3Solucao.setText(d.api3Solucao); atualizarEstiloCampo(taApi3Solucao, d.api3SolucaoStatus); }
                if (tfApi3Repo != null) { tfApi3Repo.setText(d.api3Repo); atualizarEstiloCampo(tfApi3Repo, d.api3RepoStatus); }
                if (taApi3Tecnologias != null) { taApi3Tecnologias.setText(d.api3Tecnologias); atualizarEstiloCampo(taApi3Tecnologias, d.api3TecnologiasStatus); }
                if (taApi3Contrib != null) { taApi3Contrib.setText(d.api3Contrib); atualizarEstiloCampo(taApi3Contrib, d.api3ContribStatus); }
                if (taApi3Hard != null) { taApi3Hard.setText(d.api3Hard); atualizarEstiloCampo(taApi3Hard, d.api3HardStatus); }
                if (taApi3Soft != null) { taApi3Soft.setText(d.api3Soft); atualizarEstiloCampo(taApi3Soft, d.api3SoftStatus); }
                // API 4
                if (tfApi4Empresa != null) { tfApi4Empresa.setText(d.api4Empresa); atualizarEstiloCampo(tfApi4Empresa, d.api4EmpresaStatus); }
                if (taApi4Problema != null) { taApi4Problema.setText(d.api4Problema); atualizarEstiloCampo(taApi4Problema, d.api4ProblemaStatus); }
                if (taApi4Solucao != null) { taApi4Solucao.setText(d.api4Solucao); atualizarEstiloCampo(taApi4Solucao, d.api4SolucaoStatus); }
                if (tfApi4Repo != null) { tfApi4Repo.setText(d.api4Repo); atualizarEstiloCampo(tfApi4Repo, d.api4RepoStatus); }
                if (taApi4Tecnologias != null) { taApi4Tecnologias.setText(d.api4Tecnologias); atualizarEstiloCampo(taApi4Tecnologias, d.api4TecnologiasStatus); }
                if (taApi4Contrib != null) { taApi4Contrib.setText(d.api4Contrib); atualizarEstiloCampo(taApi4Contrib, d.api4ContribStatus); }
                if (taApi4Hard != null) { taApi4Hard.setText(d.api4Hard); atualizarEstiloCampo(taApi4Hard, d.api4HardStatus); }
                if (taApi4Soft != null) { taApi4Soft.setText(d.api4Soft); atualizarEstiloCampo(taApi4Soft, d.api4SoftStatus); }
                // API 5
                if (tfApi5Empresa != null) { tfApi5Empresa.setText(d.api5Empresa); atualizarEstiloCampo(tfApi5Empresa, d.api5EmpresaStatus); }
                if (taApi5Problema != null) { taApi5Problema.setText(d.api5Problema); atualizarEstiloCampo(taApi5Problema, d.api5ProblemaStatus); }
                if (taApi5Solucao != null) { taApi5Solucao.setText(d.api5Solucao); atualizarEstiloCampo(taApi5Solucao, d.api5SolucaoStatus); }
                if (tfApi5Repo != null) { tfApi5Repo.setText(d.api5Repo); atualizarEstiloCampo(tfApi5Repo, d.api5RepoStatus); }
                if (taApi5Tecnologias != null) { taApi5Tecnologias.setText(d.api5Tecnologias); atualizarEstiloCampo(taApi5Tecnologias, d.api5TecnologiasStatus); }
                if (taApi5Contrib != null) { taApi5Contrib.setText(d.api5Contrib); atualizarEstiloCampo(taApi5Contrib, d.api5ContribStatus); }
                if (taApi5Hard != null) { taApi5Hard.setText(d.api5Hard); atualizarEstiloCampo(taApi5Hard, d.api5HardStatus); }
                if (taApi5Soft != null) { taApi5Soft.setText(d.api5Soft); atualizarEstiloCampo(taApi5Soft, d.api5SoftStatus); }
                // API 6
                if (tfApi6Empresa != null) { tfApi6Empresa.setText(d.api6Empresa); atualizarEstiloCampo(tfApi6Empresa, d.api6EmpresaStatus); }
                if (taApi6Problema != null) { taApi6Problema.setText(d.api6Problema); atualizarEstiloCampo(taApi6Problema, d.api6ProblemaStatus); }
                if (taApi6Solucao != null) { taApi6Solucao.setText(d.api6Solucao); atualizarEstiloCampo(taApi6Solucao, d.api6SolucaoStatus); }
                if (tfApi6Repo != null) { tfApi6Repo.setText(d.api6Repo); atualizarEstiloCampo(tfApi6Repo, d.api6RepoStatus); }
                if (taApi6Tecnologias != null) { taApi6Tecnologias.setText(d.api6Tecnologias); atualizarEstiloCampo(taApi6Tecnologias, d.api6TecnologiasStatus); }
                if (taApi6Contrib != null) { taApi6Contrib.setText(d.api6Contrib); atualizarEstiloCampo(taApi6Contrib, d.api6ContribStatus); }
                if (taApi6Hard != null) { taApi6Hard.setText(d.api6Hard); atualizarEstiloCampo(taApi6Hard, d.api6HardStatus); }
                if (taApi6Soft != null) { taApi6Soft.setText(d.api6Soft); atualizarEstiloCampo(taApi6Soft, d.api6SoftStatus); }

                // Aba 8: Tabela Resumo
                if (d.resumoMd != null && !d.resumoMd.isBlank()) {
                    preencherResumoAPartirDoMarkdown(d.resumoMd);
                    aplicarEstiloTabelaResumo(d.resumoMdStatus);
                } else {
                    preencherResumoAPartirDoMarkdown("");
                    aplicarEstiloTabelaResumo(0); // Pendente
                }
            });
        } catch (Exception e) {
            alertWarn("Falha ao carregar a última versão: " + e.getMessage());
        }

        // ATUALIZADO: Aplica a trava DEPOIS de carregar os dados
        atualizarTravaEdicao(this.statusAtualDoFluxo);
        refreshTabStatus(tabPane.getSelectionModel().getSelectedIndex());
    }


    // ====== NOVOS MÉTODOS HELPER PARA STATUS ======
    private void atualizarEstiloCampo(Node campo, int status) {
        // ... (seu método original está OK) ...
        if (campo == null) return;
        campo.getStyleClass().removeAll("status-aprovado", "status-reprovado", "status-pendente");
        switch (status) {
            case 1 -> campo.getStyleClass().add("status-aprovado");
            case 2 -> campo.getStyleClass().add("status-reprovado");
            default -> campo.getStyleClass().add("status-pendente");
        }
    }
    private void atualizarStatusLabel(Label label, int status) {
        // ... (seu método original está OK) ...
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
    private void aplicarEstiloTabelaResumo(int status) {
        // ... (seu método original está OK) ...
        TextInputControl[] camposResumo = {
                tfSem1, tfEmp1, taSol1, tfSem2, tfEmp2, taSol2,
                tfSem3, tfEmp3, taSol3, tfSem4, tfEmp4, taSol4,
                tfSem5, tfEmp5, taSol5, tfSem6, tfEmp6, taSol6
        };
        for (TextInputControl campo : camposResumo) {
            atualizarEstiloCampo(campo, status);
        }
    }
    // =======================================================
    private List<String[]> extrairLinhasTabela(String md) {
        // ... (seu método original está OK) ...
        List<String[]> out = new ArrayList<>();
        boolean dentro = false;
        boolean headerVisto = false;
        for (String raw : md.split("\\R")) {
            String line = raw.trim();
            if (!line.startsWith("|")) {
                if (dentro) break;
                continue;
            }
            if (line.matches("\\|\\s*-+\\s*\\|.*")) { dentro = true; continue; }
            if (!headerVisto) { headerVisto = true; continue; }
            if (!dentro) continue;
            String inner = line;
            if (inner.startsWith("|")) inner = inner.substring(1);
            if (inner.endsWith("|")) inner = inner.substring(0, inner.length()-1);
            String[] cols = Arrays.stream(inner.split("\\|"))
                    .map(String::trim)
                    .toArray(String[]::new);
            out.add(cols);
        }
        return out;
    }
    private static String val(TextInputControl c){ return (c == null || c.getText()==null) ? "" : c.getText().trim(); }
    private static String safe(String s){ return s == null ? "" : s.trim().replace("\n"," "); }
    private void alertWarn(String msg){ Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void appendIfNotEmpty(StringBuilder sb, String titulo, TextArea ta){
        String t = val(ta);
        if (!t.isBlank()){
            sb.append(titulo).append("\n").append(t).append("\n\n");
        }
    }
}