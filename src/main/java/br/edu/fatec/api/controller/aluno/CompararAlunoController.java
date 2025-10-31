package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.controller.aluno.InboxAlunoController;
import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;

public class CompararAlunoController extends BaseController {

    private void initialize(){
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }
    }

    public void voltar(){ SceneManager.go("aluno/Dashboard.fxml"); }
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
}
