package br.edu.fatec.api.controller.coordenacao;

import br.edu.fatec.api.nav.SceneManager;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import br.edu.fatec.api.controller.BaseController;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import br.edu.fatec.api.dao.JdbcPainelOrientadorDao;
import br.edu.fatec.api.dto.PainelOrientadorRow;
import java.util.ArrayList;
import java.util.List;

public class MapaCoordController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnMapa;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private ChoiceBox<String> cbStatus;

    // Tabela
    @FXML private TableView<MapaVM> tblMapa;
    @FXML private TableColumn<MapaVM, String> colProfessor, colAluno, colTema, colStatus;

    private FilteredList<MapaVM> filtered;
    private final ObservableList<MapaVM> base = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Ativa rota na sidebar
        if (btnMapa != null && !btnMapa.getStyleClass().contains("active")) {
            btnMapa.getStyleClass().add("active");
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Colunas
        colProfessor.setCellValueFactory(d -> d.getValue().professor);
        colAluno.setCellValueFactory(d -> d.getValue().aluno);
        colTema.setCellValueFactory(d -> d.getValue().tema);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        // Dados mock (trocar por DAO/Service)
        // Define o placeholder inicial
        tblMapa.setPlaceholder(new Label("Carregando dados..."));

        // ===== Carga assíncrona (DAO) =====
        Task<List<MapaVM>> task = new Task<>() {
            @Override
            protected List<MapaVM> call() throws Exception {
                // Reutiliza o DAO que já criamos
                JdbcPainelOrientadorDao painelDao = new JdbcPainelOrientadorDao();
                List<PainelOrientadorRow> rows = painelDao.listarParaCoordenador();

                List<MapaVM> list = new ArrayList<>();

                // Converte o DTO (Row) para a VM (da tela)
                for (PainelOrientadorRow r : rows) {
                    list.add(new MapaVM(
                            r.getOrientador(),
                            r.getAluno(),
                            r.getTema(),
                            // DECISÃO: Usando Opção A (o status que já vem do DAO)
                            r.getStatus()
                    ));
                }
                return list;
            }
        };

        // O que fazer quando a tarefa (call()) terminar com SUCESSO
        task.setOnSucceeded(ev -> {
            base.setAll(task.getValue()); // Popula a lista base

            // Configura o filtro (apontando para a base)
            filtered = new FilteredList<>(base, r -> true);
            tblMapa.setItems(filtered);

            // Configura os filtros (agora que os dados chegaram)
            // Adicionamos os status do DAO aos que já existiam no mock
            cbStatus.getItems().setAll("Todos", "Em andamento", "Atrasado", "Entregue", "Concluído", "Não avaliado");
            cbStatus.getSelectionModel().selectFirst();

            // Liga os listeners
            txtBusca.textProperty().addListener((obs, o, n) -> aplicarFiltros());
            cbStatus.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> aplicarFiltros());

            // Define o placeholder para caso a lista esteja vazia
            tblMapa.setPlaceholder(new Label("Sem registros para exibir."));
        });

        // O que fazer se a tarefa (call()) FALHAR
        task.setOnFailed(ev -> {
            tblMapa.setPlaceholder(new Label("Erro ao carregar dados."));
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

    private void aplicarFiltros() {
        final String q = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        final String s = cbStatus.getValue() == null ? "Todos" : cbStatus.getValue();

        filtered.setPredicate(vm -> {
            boolean matchStatus = "Todos".equals(s) || vm.status.get().equalsIgnoreCase(s);
            boolean matchTexto =
                    vm.professor.get().toLowerCase().contains(q) ||
                            vm.aluno.get().toLowerCase().contains(q) ||
                            vm.tema.get().toLowerCase().contains(q);
            return matchStatus && matchTexto;
        });
    }

    public void limparFiltros() {
        txtBusca.clear();
        cbStatus.getSelectionModel().selectFirst();
    }

    /* ===== Navegação ===== */
    public void goHomeOrient(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goMapa(){ SceneManager.go("coordenacao/Mapa.fxml"); }
    public void goAndamento(){ SceneManager.go("coordenacao/Andamento.fxml"); }
    public void goHistorico() { SceneManager.go("coordenacao/HistoricoCoord.fxml"); }


    /* ===== VM ===== */
    public static class MapaVM {
        final SimpleStringProperty professor = new SimpleStringProperty();
        final SimpleStringProperty aluno = new SimpleStringProperty();
        final SimpleStringProperty tema = new SimpleStringProperty();
        final SimpleStringProperty status = new SimpleStringProperty();
        public MapaVM(String p, String a, String t, String s){ professor.set(p); aluno.set(a); tema.set(t); status.set(s); }
    }
}
