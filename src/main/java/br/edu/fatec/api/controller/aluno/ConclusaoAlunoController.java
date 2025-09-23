package br.edu.fatec.api.controller.aluno;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import br.edu.fatec.api.nav.SceneManager;

public class ConclusaoAlunoController {
    @FXML private Label lblConclusaoGeral, lblSecoesConcluidas, lblUltimaAtualizacao;
    @FXML private TableView<?> tblSecoes;
    @FXML private TableColumn<?,?> colSecao, colStatus, colPercentual, colAtualizadoEm;

    // Navegação
    public void goHome(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goDashboard(){ SceneManager.go("aluno/Dashboard.fxml"); }
    public void goInbox(){ SceneManager.go("aluno/Inbox.fxml"); }
    public void goEditor(){ SceneManager.go("aluno/Editor.fxml"); }
    public void goComparar(){ SceneManager.go("aluno/Comparar.fxml"); }
    public void goConclusao(){ SceneManager.go("aluno/Conclusao.fxml"); }
    public void goHistorico(){ SceneManager.go("aluno/Historico.fxml"); }

    // Ações
    public void recalcular(){ /* TODO: service para recálculo do % por seção */ }
    public void exportMd(){ /* TODO: gerar .md consolidado */ }
    public void exportPdf(){ /* TODO: gerar .pdf (via wkhtmltopdf/itext ou similar) */ }
}
