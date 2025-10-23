package br.edu.fatec.api.controller.coordenacao;

import br.edu.fatec.api.nav.SceneManager;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MapaCoordController {

    // Sidebar – rota ativa
    @FXML private Button btnMapa;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private ChoiceBox<String> cbStatus;

    // Tabela
    @FXML private TableView<MapaVM> tblMapa;
    @FXML private TableColumn<MapaVM, String> colProfessor, colAluno, colTema, colStatus;

    private FilteredList<MapaVM> filtered;

    @FXML
    private void initialize() {
        // Ativa rota na sidebar
        if (btnMapa != null && !btnMapa.getStyleClass().contains("active")) {
            btnMapa.getStyleClass().add("active");
        }

        // Colunas
        colProfessor.setCellValueFactory(d -> d.getValue().professor);
        colAluno.setCellValueFactory(d -> d.getValue().aluno);
        colTema.setCellValueFactory(d -> d.getValue().tema);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        // Dados mock (trocar por DAO/Service)
        var data = FXCollections.observableArrayList(
                new MapaVM("Prof. Almeida", "Ana Souza", "Visão computacional em JavaFX", "Em andamento"),
                new MapaVM("Prof. Almeida", "Bruno Lima", "Integração JDBC/SQLite", "Atrasado"),
                new MapaVM("Profa. Carla", "Carla Mendes", "UI/UX para JavaFX", "Entregue"),
                new MapaVM("Profa. Carla", "Diego Rocha", "Relatórios e KPIs", "Em andamento")
        );

        filtered = new FilteredList<>(data, r -> true);
        tblMapa.setItems(filtered);
        tblMapa.setPlaceholder(new Label("Sem registros para exibir."));

        // Filtro status
        cbStatus.getItems().setAll("Todos", "Em andamento", "Atrasado", "Entregue");
        cbStatus.getSelectionModel().selectFirst();

        // Reatividade de filtros
        txtBusca.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        cbStatus.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> aplicarFiltros());
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

    /* ===== VM ===== */
    public static class MapaVM {
        final SimpleStringProperty professor = new SimpleStringProperty();
        final SimpleStringProperty aluno = new SimpleStringProperty();
        final SimpleStringProperty tema = new SimpleStringProperty();
        final SimpleStringProperty status = new SimpleStringProperty();
        public MapaVM(String p, String a, String t, String s){ professor.set(p); aluno.set(a); tema.set(t); status.set(s); }
    }
}
