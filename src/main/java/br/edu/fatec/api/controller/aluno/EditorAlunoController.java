package br.edu.fatec.api.controller.aluno;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import br.edu.fatec.api.nav.SceneManager;

public class EditorAlunoController {

    @FXML private TextArea markdownEditor;
    @FXML private VBox commentsSection;

    // Navegação (mesmos nomes do Dashboard)
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){ SceneManager.go("aluno/Inbox.fxml"); }
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }

    // Toolbar – colocações simples de markdown (mínimo viável)
    public void toggleBold(){ insertAround("**"); }
    public void toggleItalic(){ insertAround("*"); }
    public void toggleUnderline(){ insertAround("__"); }
    public void insertH1(){ insertAtCaret("\n# Título\n"); }
    public void insertLink(){ insertAtCaret("[texto](https://)"); }
    public void preview(){ /* TODO: abrir modal/aba com renderização */ }
    public void salvarVersao(){ /* TODO: chamar service de persistência */ }

    private void insertAround(String wrapper){
        if (markdownEditor == null) return;
        var sel = markdownEditor.getSelectedText();
        if (sel == null || sel.isEmpty()) sel = "texto";
        replaceSelection(wrapper + sel + wrapper);
    }
    private void insertAtCaret(String text){ replaceSelection(text); }
    private void replaceSelection(String text){
        var i = markdownEditor.getSelection();
        markdownEditor.replaceText(i.getStart(), i.getEnd(), text);
        markdownEditor.positionCaret(i.getStart() + text.length());
    }
}
