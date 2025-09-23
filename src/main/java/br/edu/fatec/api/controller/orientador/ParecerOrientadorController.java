package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class ParecerOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnParecer;

    // Campos
    @FXML private TextArea txtDocumentoAluno;
    @FXML private TextArea txtParecer;

    @FXML
    private void initialize() {
        // Destaca rota ativa
        if (btnParecer != null && !btnParecer.getStyleClass().contains("active")) {
            btnParecer.getStyleClass().add("active");
        }

        // Placeholder (trocar por carga real do service)
        if (txtDocumentoAluno != null) {
            txtDocumentoAluno.setText("""
# Introdução
Trecho de exemplo do documento do aluno para revisão de parecer.

## Pontos observados
- Objetivos claros
- Metodologia consistente
- Resultados parciais

## Conclusão
Conforme leitura inicial, recomenda-se ajustes na seção de resultados.
""");
        }
    }

    // ===== Ação principal =====
    public void salvarParecer() {
        final String parecer = (txtParecer != null) ? txtParecer.getText() : null;
        if (parecer == null || parecer.isBlank()) {
            // opcional: mostrar alerta
            return;
        }

        // TODO: Persistir parecer (SQLite/JDBC)
        // Ex.: parecer(id, aluno_id, secao_id, professor_id, texto, criado_em)
        // parecerService.salvar(alunoId, secaoId, professorId, parecer);

        // Opcional: navegar de volta ao painel ou manter na página
        // SceneManager.go("orientador/Painel.fxml");
        txtParecer.clear();
    }

    // ===== Navegação =====
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    public void goChat(){ SceneManager.go("orientador/Chat.fxml"); }

    // Compatibilidade com seu handler antigo
    public void voltar(){ SceneManager.go("orientador/Painel.fxml"); }
}
