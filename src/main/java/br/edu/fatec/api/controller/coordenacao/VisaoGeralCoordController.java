package br.edu.fatec.api.controller.coordenacao;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcKpiDao;
import javafx.concurrent.Task;

public class VisaoGeralCoordController extends BaseController {


    // KPIs
    @FXML private Label lblAlunos; // Card do Meio
    @FXML private Label lblOrientadoresComAlunos; // Card da Direita
    @FXML private Label lblOrientadoresSemAlunos; // Card da Esquerda (antigo Cursos)

    @FXML
    private void initialize() {

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Define o texto inicial enquanto carrega
        lblAlunos.setText("...");
        lblOrientadoresComAlunos.setText("...");
        lblOrientadoresSemAlunos.setText("...");

        // ===== Carga assíncrona (DAO) =====
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() throws Exception {
                // 1. Chama o DAO
                JdbcKpiDao kpiDao = new JdbcKpiDao();

                // 2. Busca os 3 valores do banco
                int alunos = kpiDao.countAlunosAtivos();
                int comAlunos = kpiDao.countOrientadoresComAlunos();
                int semAlunos = kpiDao.countOrientadoresSemAlunos();

                // 3. Retorna os valores
                return new int[]{alunos, comAlunos, semAlunos};
            }
        };

        // O que fazer quando a tarefa (call()) terminar com SUCESSO
        task.setOnSucceeded(ev -> {
            int[] resultados = task.getValue();
            lblAlunos.setText(Integer.toString(resultados[0]));
            lblOrientadoresComAlunos.setText(Integer.toString(resultados[1]));
            lblOrientadoresSemAlunos.setText(Integer.toString(resultados[2]));
        });

        // O que fazer se a tarefa (call()) FALHAR
        task.setOnFailed(ev -> {
            lblAlunos.setText("Erro");
            lblOrientadoresComAlunos.setText("Erro");
            lblOrientadoresSemAlunos.setText("Erro");
            if (task.getException() != null) {
                // Imprime o erro no console para debug
                task.getException().printStackTrace();
            }
        });

        // Inicia a tarefa em uma nova Thread
        Thread t = new Thread(task);
        t.setDaemon(true); // Garante que a thread não impeça o app de fechar
        t.start();
    }

    // ===== Navegação =====
    public void goHomeOrient(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goMapa(){ SceneManager.go("coordenacao/Mapa.fxml"); }
    public void goAndamento(){ SceneManager.go("coordenacao/Andamento.fxml"); }
    public void goHistorico() { SceneManager.go("coordenacao/HistoricoCoord.fxml"); }
}
