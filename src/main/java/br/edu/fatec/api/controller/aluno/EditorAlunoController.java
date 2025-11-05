package br.edu.fatec.api.controller.aluno;

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

public class EditorAlunoController extends BaseController {

    // ====== UI base (sidebar etc.)
    @FXML private VBox commentsSection;

    // ====== Toolbar e TabPane
    @FXML private TabPane tabPane;
    @FXML private Label lbStatusAba; // <--- ADIÇÃO: label já inserido no FXML

    // ====== ABA 1 - Apresentação
    @FXML private TextArea taInfoPessoais;
    @FXML private TextArea taHistoricoAcad;
    @FXML private TextArea taMotivacao;
    @FXML private TextArea taHistoricoProf;
    @FXML private TextArea taContatos;
    @FXML private TextArea taConhecimentos;

    // ====== ABAS 2..7 - Projetos API (1º..6º)
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

    // ====== ABA 8 - Tabela Resumo
    @FXML private TextField tfSem1, tfSem2, tfSem3, tfSem4, tfSem5, tfSem6;
    @FXML private TextField tfEmp1, tfEmp2, tfEmp3, tfEmp4, tfEmp5, tfEmp6;
    @FXML private TextArea taSol1, taSol2, taSol3, taSol4, taSol5, taSol6;

    // ====== ABA 9 - Considerações
    @FXML private TextArea taConclusoes;

    // ====== Estado
    private TextInputControl focusedTextInput; // usado pela toolbar

    // ====== Botão salvar tudo + service
    @FXML private Button btnSalvarTudo; // (se tiver fx:id no FXML)
    private final EditorAlunoService service = new EditorAlunoService();

    // ====== Navegação (inalterada)
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
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }

    // ====== Ciclo de vida
    @FXML
    public void initialize() {
        // Se não estiver logado, volta ao login
        User u = Session.getUser();
        if (u == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Pré-carrega a última versão nos campos
        try {
            long trabalhoId = service.resolveTrabalhoIdDoAlunoLogado();
            service.carregarTudo(trabalhoId).ifPresent(d -> {
                // Aba 1
                if (taInfoPessoais != null)   taInfoPessoais.setText(d.infoPessoais);
                if (taHistoricoAcad != null)  taHistoricoAcad.setText(d.historicoAcad);
                if (taMotivacao != null)      taMotivacao.setText(d.motivacao);
                if (taHistoricoProf != null)  taHistoricoProf.setText(d.historicoProf);
                if (taContatos != null)       taContatos.setText(d.contatos);
                if (taConhecimentos != null)  taConhecimentos.setText(d.conhecimentos);
                if (taConclusoes != null)     taConclusoes.setText(d.consideracoes); // Aba 9

                // APIs 1..6
                if (tfApi1Empresa != null) tfApi1Empresa.setText(d.api1Empresa);
                if (taApi1Problema != null) taApi1Problema.setText(d.api1Problema);
                if (taApi1Solucao != null) taApi1Solucao.setText(d.api1Solucao);
                if (tfApi1Repo != null) tfApi1Repo.setText(d.api1Repo);
                if (taApi1Tecnologias != null) taApi1Tecnologias.setText(d.api1Tecnologias);
                if (taApi1Contrib != null) taApi1Contrib.setText(d.api1Contrib);
                if (taApi1Hard != null) taApi1Hard.setText(d.api1Hard);
                if (taApi1Soft != null) taApi1Soft.setText(d.api1Soft);

                if (tfApi2Empresa != null) tfApi2Empresa.setText(d.api2Empresa);
                if (taApi2Problema != null) taApi2Problema.setText(d.api2Problema);
                if (taApi2Solucao != null) taApi2Solucao.setText(d.api2Solucao);
                if (tfApi2Repo != null) tfApi2Repo.setText(d.api2Repo);
                if (taApi2Tecnologias != null) taApi2Tecnologias.setText(d.api2Tecnologias);
                if (taApi2Contrib != null) taApi2Contrib.setText(d.api2Contrib);
                if (taApi2Hard != null) taApi2Hard.setText(d.api2Hard);
                if (taApi2Soft != null) taApi2Soft.setText(d.api2Soft);

                if (tfApi3Empresa != null) tfApi3Empresa.setText(d.api3Empresa);
                if (taApi3Problema != null) taApi3Problema.setText(d.api3Problema);
                if (taApi3Solucao != null) taApi3Solucao.setText(d.api3Solucao);
                if (tfApi3Repo != null) tfApi3Repo.setText(d.api3Repo);
                if (taApi3Tecnologias != null) taApi3Tecnologias.setText(d.api3Tecnologias);
                if (taApi3Contrib != null) taApi3Contrib.setText(d.api3Contrib);
                if (taApi3Hard != null) taApi3Hard.setText(d.api3Hard);
                if (taApi3Soft != null) taApi3Soft.setText(d.api3Soft);

                if (tfApi4Empresa != null) tfApi4Empresa.setText(d.api4Empresa);
                if (taApi4Problema != null) taApi4Problema.setText(d.api4Problema);
                if (taApi4Solucao != null) taApi4Solucao.setText(d.api4Solucao);
                if (tfApi4Repo != null) tfApi4Repo.setText(d.api4Repo);
                if (taApi4Tecnologias != null) taApi4Tecnologias.setText(d.api4Tecnologias);
                if (taApi4Contrib != null) taApi4Contrib.setText(d.api4Contrib);
                if (taApi4Hard != null) taApi4Hard.setText(d.api4Hard);
                if (taApi4Soft != null) taApi4Soft.setText(d.api4Soft);

                if (tfApi5Empresa != null) tfApi5Empresa.setText(d.api5Empresa);
                if (taApi5Problema != null) taApi5Problema.setText(d.api5Problema);
                if (taApi5Solucao != null) taApi5Solucao.setText(d.api5Solucao);
                if (tfApi5Repo != null) tfApi5Repo.setText(d.api5Repo);
                if (taApi5Tecnologias != null) taApi5Tecnologias.setText(d.api5Tecnologias);
                if (taApi5Contrib != null) taApi5Contrib.setText(d.api5Contrib);
                if (taApi5Hard != null) taApi5Hard.setText(d.api5Hard);
                if (taApi5Soft != null) taApi5Soft.setText(d.api5Soft);

                if (tfApi6Empresa != null) tfApi6Empresa.setText(d.api6Empresa);
                if (taApi6Problema != null) taApi6Problema.setText(d.api6Problema);
                if (taApi6Solucao != null) taApi6Solucao.setText(d.api6Solucao);
                if (tfApi6Repo != null) tfApi6Repo.setText(d.api6Repo);
                if (taApi6Tecnologias != null) taApi6Tecnologias.setText(d.api6Tecnologias);
                if (taApi6Contrib != null) taApi6Contrib.setText(d.api6Contrib);
                if (taApi6Hard != null) taApi6Hard.setText(d.api6Hard);
                if (taApi6Soft != null) taApi6Soft.setText(d.api6Soft);

                // Aba 8: preencher campos a partir do markdown do resumo
                if (d.resumoMd != null && !d.resumoMd.isBlank()) {
                    preencherResumoAPartirDoMarkdown(d.resumoMd);
                }
            });
        } catch (Exception e) {
            alertWarn("Falha ao carregar a última versão: " + e.getMessage());
        }

        hookFocusHandlers();
        applyTips();
        wireTabStatus(); // <--- ADIÇÃO: inicializa o indicador dinâmico
    }

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
            long trabalhoId = service.resolveTrabalhoIdDoAlunoLogado();
            boolean validada = service.isAbaValidada(trabalhoId, abaNumero);
            statusTxt = validada ? "Concluída" : "Pendente Validação";
        } catch (Exception ignore){ /* mantém default */ }
        lbStatusAba.setText("Aba " + abaNumero + " - Status: " + statusTxt);

        // opcional: classes CSS
        lbStatusAba.getStyleClass().removeAll("badge-ok","badge-pendente");
        lbStatusAba.getStyleClass().add(statusTxt.equals("Concluída") ? "badge-ok" : "badge-pendente");
    }

    private void hookFocusHandlers() {
        tabPane.addEventFilter(MouseEvent.MOUSE_PRESSED, evt -> {
            Node n = evt.getPickResult().getIntersectedNode();
            while (n != null) {
                if (n instanceof TextInputControl) {
                    focusedTextInput = (TextInputControl) n;
                    break;
                }
                n = n.getParent();
            }
        });
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

    // ====== Toolbar – Markdown sobre o campo focado
    public void toggleBold(){ insertAroundBold("**"); }
    public void toggleItalic(){ insertAroundItalic("*"); }
    public void toggleUnderline(){ insertAroundUnderline("__"); }
    public void insertH1(){ insertAtCaret("\n# Título\n"); }
    public void insertLink(){ insertAtCaret("[Texto](https://)"); }

    public void preview(){
        String md = montarMarkdownCompleto();
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Pré-visualização (trecho)");
        a.setContentText(md.substring(0, Math.min(500, md.length())) + (md.length() > 500 ? "\n...\n" : ""));
        a.showAndWait();
    }

    // ====== Salvar TUDO (único handler chamado pelo botão da toolbar)
    public void salvarTudo() {
        try {
            long trabalhoId = service.resolveTrabalhoIdDoAlunoLogado();

            EditorAlunoService.DadosEditor d = new EditorAlunoService.DadosEditor();

            // Aba 1:
            d.infoPessoais   = val(taInfoPessoais);
            d.historicoAcad  = val(taHistoricoAcad);
            d.motivacao      = val(taMotivacao);
            d.historicoProf  = val(taHistoricoProf);
            d.contatos       = val(taContatos);
            d.conhecimentos  = val(taConhecimentos);

            // Abas 2..7 (API 1..6):
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

            // Aba 8:
            d.resumoMd = montarMdResumo();

            // Aba 9:
            d.consideracoesFinais = val(taConclusoes);

            // MD consolidado (preview):
            d.mdCompleto = montarMarkdownCompleto();

            String novaVersao = service.salvarTudo(trabalhoId, d);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Salvo com sucesso!");
            ok.setContentText("Nova versão: " + novaVersao);
            ok.showAndWait();

            // Atualiza o badge de status após salvar (a versão nova já copia validações)
            refreshTabStatus(tabPane.getSelectionModel().getSelectedIndex());

        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText("Falha ao salvar");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    // ====== Montagem de Markdown (por seção) — mantém seu modelo atual
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

    // ====== Parse do resumo_md para os campos da Aba 8
    private void preencherResumoAPartirDoMarkdown(String md) {
        List<String[]> linhas = extrairLinhasTabela(md);
        int linha = 0;
        for (String[] cols : linhas) {
            if (cols.length < 3) continue;
            linha++;
            switch (linha) {
                case 1 -> { if (tfSem1!=null) tfSem1.setText(cols[0]); if (tfEmp1!=null) tfEmp1.setText(cols[1]); if (taSol1!=null) taSol1.setText(cols[2]); }
                case 2 -> { if (tfSem2!=null) tfSem2.setText(cols[0]); if (tfEmp2!=null) tfEmp2.setText(cols[1]); if (taSol2!=null) taSol2.setText(cols[2]); }
                case 3 -> { if (tfSem3!=null) tfSem3.setText(cols[0]); if (tfEmp3!=null) tfEmp3.setText(cols[1]); if (taSol3!=null) taSol3.setText(cols[2]); }
                case 4 -> { if (tfSem4!=null) tfSem4.setText(cols[0]); if (tfEmp4!=null) tfEmp4.setText(cols[1]); if (taSol4!=null) taSol4.setText(cols[2]); }
                case 5 -> { if (tfSem5!=null) tfSem5.setText(cols[0]); if (tfEmp5!=null) tfEmp5.setText(cols[1]); if (taSol5!=null) taSol5.setText(cols[2]); }
                case 6 -> { if (tfSem6!=null) tfSem6.setText(cols[0]); if (tfEmp6!=null) tfEmp6.setText(cols[1]); if (taSol6!=null) taSol6.setText(cols[2]); }
                default -> { /* ignora linhas extras */ }
            }
            if (linha >= 6) break;
        }
    }

    private List<String[]> extrairLinhasTabela(String md) {
        List<String[]> out = new ArrayList<>();
        boolean dentro = false;
        boolean headerVisto = false;
        for (String raw : md.split("\\R")) {
            String line = raw.trim();
            if (!line.startsWith("|")) {
                if (dentro) break; // saiu da tabela
                continue;
            }
            // linha de separadores --- (ativa modo tabela)
            if (line.matches("\\|\\s*-+\\s*\\|.*")) { dentro = true; continue; }
            // primeira linha com '|' deve ser o header; marca e segue
            if (!headerVisto) { headerVisto = true; continue; }
            if (!dentro) continue;

            // linha de dados: remove bordas e separa por '|'
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

    // ====== utils
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
