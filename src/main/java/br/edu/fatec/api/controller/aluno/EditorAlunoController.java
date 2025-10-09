package br.edu.fatec.api.controller.aluno;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import br.edu.fatec.api.nav.SceneManager;

import java.util.*;
import java.util.stream.Collectors;

public class EditorAlunoController {

    // ====== UI base (sidebar etc.)
    @FXML private VBox commentsSection;

    // ====== Toolbar e TabPane
    @FXML private TabPane tabPane;

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

    // ====== ABA 8 - Tabela Resumo (GridPane com TextFields/TextAreas)
    @FXML private TextField tfSem1, tfSem2, tfSem3, tfSem4, tfSem5, tfSem6;
    @FXML private TextField tfEmp1, tfEmp2, tfEmp3, tfEmp4, tfEmp5, tfEmp6;
    @FXML private TextArea taSol1, taSol2, taSol3, taSol4, taSol5, taSol6;

    // ====== ABA 9 - Considerações
    @FXML private TextArea taConclusoes;

    // ====== Estado
    private TextInputControl focusedTextInput; // usado pela toolbar

    // ====== Simulação de persistência em memória (sem BD)
    // chave = numero_aba (1..9) | valor = markdown da seção
    private final Map<Integer, String> secoesMem = new LinkedHashMap<>();

    // ====== Navegação (inalterada)
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){ SceneManager.go("aluno/Inbox.fxml"); }
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }

    // ====== Ciclo de vida
    @FXML
    public void initialize() {
        hookFocusHandlers();
        applyTips();
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
        // Adicione mais tooltips conforme necessário nas demais abas…
    }

    // ====== Toolbar – Markdown sobre o campo focado
    public void toggleBold(){ insertAround("**"); }
    public void toggleItalic(){ insertAround("*"); }
    public void toggleUnderline(){ insertAround("__"); }
    public void insertH1(){ insertAtCaret("\n# Título\n"); }
    public void insertLink(){ insertAtCaret("[texto](https://)"); }

    public void preview(){
        String md = montarMarkdownCompleto();
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Pré-visualização (trecho)");
        a.setContentText(md.substring(0, Math.min(500, md.length())) + (md.length() > 500 ? "\n...\n" : ""));
        a.showAndWait();
    }

    public void salvarVersao(){
        int idx = tabPane.getSelectionModel().getSelectedIndex();
        switch (idx) {
            case 0 -> salvarAba1();
            case 1 -> salvarAba2();
            case 2 -> salvarAba3();
            case 3 -> salvarAba4();
            case 4 -> salvarAba5();
            case 5 -> salvarAba6();
            case 6 -> salvarAba7();
            case 7 -> salvarAba8();
            case 8 -> salvarAba9();
            default -> {}
        }
    }

    private void insertAround(String wrapper){
        TextInputControl target = (focusedTextInput != null) ? focusedTextInput : getFirstVisibleTextInput();
        if (target == null) return;
        String sel = target.getSelectedText();
        if (sel == null || sel.isEmpty()) sel = "texto";
        replaceSelection(target, wrapper + sel + wrapper);
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

    // ====== Salvar por ABA (memória)
    public void salvarAba1(){ secoesMem.put(1, montarMdAba1()); feedbackOk(); }
    public void salvarAba2(){ secoesMem.put(2, montarMdProjeto(1)); feedbackOk(); }
    public void salvarAba3(){ secoesMem.put(3, montarMdProjeto(2)); feedbackOk(); }
    public void salvarAba4(){ secoesMem.put(4, montarMdProjeto(3)); feedbackOk(); }
    public void salvarAba5(){ secoesMem.put(5, montarMdProjeto(4)); feedbackOk(); }
    public void salvarAba6(){ secoesMem.put(6, montarMdProjeto(5)); feedbackOk(); }
    public void salvarAba7(){ secoesMem.put(7, montarMdProjeto(6)); feedbackOk(); }
    public void salvarAba8(){ secoesMem.put(8, montarMdResumo()); feedbackOk(); }
    public void salvarAba9(){ secoesMem.put(9, montarMdConclusoes()); feedbackOk(); }

    private void feedbackOk(){
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText("Seção salva (memória) com sucesso.");
        a.showAndWait();
    }

    // ====== Montagem de Markdown (por seção)
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

        /* ===== (VALIDAÇÃO OPCIONAL – DESCOMENTAR QUANDO FOR ATIVAR)
        if (linhas(problema) < 3) { warn("Problema deve ter ao menos 3 linhas."); }
        if (linhas(solucao) < 5) { warn("Solução deve ter ~5 linhas."); }
        if (!repo.isBlank() && !repo.matches("^https://github\\.com/.+/.+")) { warn("Repositório deve ser uma URL GitHub válida."); }
        */
        return sb.toString();
    }

    private String montarMdResumo(){
        String header = "## Tabela Resumo dos Projetos API\n\n"
                + "| Semestre | Empresa Parceira | Solução Desenvolvida |\n|---|---|---|\n";

        if (tfSem1 != null){ // se os campos da opção B existem
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

        // Fallback vazio (não deve ocorrer)
        return header + "|  |  |  |\n|  |  |  |\n|  |  |  |\n|  |  |  |\n|  |  |  |\n|  |  |  |\n\n";
    }

    private String montarMdConclusoes(){
        return "## Considerações Finais\n\n" + val(taConclusoes) + "\n\n";
    }

    private String montarMarkdownCompleto(){
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 9; i++){
            String md = secoesMem.get(i);
            if (md != null && !md.isBlank()) {
                sb.append(md);
                if (!md.endsWith("\n")) sb.append("\n");
            } else {
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
        }
        return sb.toString();
    }

    private static String val(TextInputControl c){ return (c == null || c.getText()==null) ? "" : c.getText().trim(); }
    private static String safe(String s){ return s == null ? "" : s.trim().replace("\n"," "); }
    private static int linhas(String s){
        if (s == null || s.isBlank()) return 0;
        return (int) Arrays.stream(s.split("\\R")).filter(t -> !t.isBlank()).count();
    }
    private void warn(String msg){
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText("Validação");
        a.setContentText(msg);
        a.showAndWait();
    }
    private void appendIfNotEmpty(StringBuilder sb, String titulo, TextArea ta){
        String t = val(ta);
        if (!t.isBlank()){
            sb.append(titulo).append("\n").append(t).append("\n\n");
        }
    }
}