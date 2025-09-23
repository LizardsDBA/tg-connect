package br.edu.fatec.api.controller.aluno;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import br.edu.fatec.api.nav.SceneManager;

public class HistoricoAlunoController {
    @FXML private ChoiceBox<String> cbSecao, cbStatus;
    @FXML private TableView<?> tblVersoes;
    @FXML private TableColumn<?,?> colVersao, colCriadoEm, colSecao, colAlteracoes, colStatus, colRevisor;

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
    public void limparFiltros(){ /* TODO: resetar filtros e recarregar tabela */ }
    public void verSelecionada(){ /* TODO: abrir viewer de versão */ }
    public void compararComAtual(){ /* TODO: abrir Comparar.fxml com versão atual */ }
    public void restaurarVersao(){ /* TODO: service para sobrescrever conteúdo atual */ }
}
