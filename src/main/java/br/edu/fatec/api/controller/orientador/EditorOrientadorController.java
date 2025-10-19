package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class EditorOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnEditor;

    // Áreas da tela
    @FXML private TextArea txtAluno;      // texto do aluno (somente leitura)
    @FXML private TextArea txtMarkdown;   // comentário do orientador

    @FXML private ScrollPane commentsScroll;
    @FXML private VBox commentsSection;

    @FXML
    private void initialize() {
        // Destaca rota ativa
        if (btnEditor != null && !btnEditor.getStyleClass().contains("active")) {
            btnEditor.getStyleClass().add("active");
        }

        // Placeholder de conteúdo (trocar por carga real do service)
        if (txtAluno != null) {
            txtAluno.setText("""
# Introdução
Este é um exemplo de seção em Markdown do aluno.
Selecione um trecho aqui e adicione um comentário ao lado.
""");
        }

        // Placeholder inicial de comentário
        appendSystemNote("Selecione um trecho do texto à esquerda para vincular ao seu feedback.");
    }

    // === Ações ===
    public void comentar() {
        final String comentario = (txtMarkdown != null) ? txtMarkdown.getText() : null;
        if (comentario == null || comentario.isBlank()) return;

        // trecho selecionado no texto do aluno (se houver)
        String trecho = "";
        if (txtAluno != null) {
            String sel = txtAluno.getSelectedText();
            trecho = (sel == null || sel.isBlank()) ? "" : sel.trim();
        }

        appendComment(trecho, comentario.trim());
        txtMarkdown.clear();
        autoScroll();
        // TODO: persistir (SQLite) -> tabela feedback(secao_id, professor_id, trecho, comentario, criado_em)
    }

    public void voltar() { SceneManager.go("orientador/Painel.fxml"); }

    // Navegação da sidebar/header
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    // (ALTERADO POR MATHEUS - adicionado callback onReady)
    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    // === Helpers de UI (bubbles simples) ===
    private void appendSystemNote(String text) {
        Label bubble = new Label(text);
        bubble.getStyleClass().addAll("msg", "msg-peer");
        bubble.setWrapText(true);
        bubble.setMaxWidth(520);

        HBox line = new HBox(bubble);
        line.setAlignment(Pos.CENTER_LEFT);
        commentsSection.getChildren().add(line);
    }

    private void appendComment(String trecho, String comentario) {
        VBox bubbleBox = new VBox(6);

        if (trecho != null && !trecho.isBlank()) {
            Label trechoLbl = new Label("Trecho: \"" + safeTrim(trecho, 200) + "\"");
            trechoLbl.getStyleClass().add("pill");
            bubbleBox.getChildren().add(trechoLbl);
        }

        Label cmtLbl = new Label(comentario);
        cmtLbl.getStyleClass().addAll("msg", "msg-self");
        cmtLbl.setWrapText(true);
        cmtLbl.setMaxWidth(520);

        bubbleBox.getChildren().add(cmtLbl);

        HBox line = new HBox(bubbleBox);
        HBox.setHgrow(bubbleBox, Priority.NEVER);
        line.setAlignment(Pos.CENTER_RIGHT);
        commentsSection.getChildren().add(line);
    }

    private void autoScroll() {
        if (commentsScroll == null) return;
        commentsScroll.layout();
        commentsScroll.setVvalue(1.0);
    }

    private String safeTrim(String s, int max) {
        if (s == null) return "";
        String t = s.trim().replaceAll("\\s+", " ");
        return (t.length() <= max) ? t : (t.substring(0, Math.max(0, max - 3)) + "...");
    }
}
