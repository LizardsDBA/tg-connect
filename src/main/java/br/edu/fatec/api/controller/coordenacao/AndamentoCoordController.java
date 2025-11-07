package br.edu.fatec.api.controller.coordenacao;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.nav.SceneManager;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.concurrent.Task;
import br.edu.fatec.api.dao.JdbcPainelOrientadorDao;
import br.edu.fatec.api.dao.JdbcKpiDao;
import br.edu.fatec.api.dto.PainelOrientadorRow;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;

public class AndamentoCoordController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnAndamento;

    // KPIs
    @FXML private Label lblAlunosAtivos, lblEmDia, lblAtrasados;

    // Filtros
    @FXML private ChoiceBox<String> cbCurso, cbStatus;
    @FXML private TextField txtBusca;
    @FXML private ProgressIndicator loader;
    @FXML private Label lblCarrega;

    // Tabela
    @FXML private TableView<AndamentoVM> tblAndamento;
    @FXML private TableColumn<AndamentoVM, String> colAluno, colOrientador, colTema, colStatus, colAtualizado;
    @FXML private TableColumn<AndamentoVM, Number> colPercentual;

    private final ObservableList<AndamentoVM> base = FXCollections.observableArrayList();
    private FilteredList<AndamentoVM> filtered;

    @FXML
    private void initialize() {
        // Rota ativa
        if (btnAndamento != null && !btnAndamento.getStyleClass().contains("active")) {
            btnAndamento.getStyleClass().add("active");
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Bind colunas
        colAluno.setCellValueFactory(c -> c.getValue().aluno);
        colOrientador.setCellValueFactory(c -> c.getValue().orientador);
        colTema.setCellValueFactory(c -> c.getValue().tema);
        colStatus.setCellValueFactory(c -> c.getValue().status);
        colAtualizado.setCellValueFactory(c -> c.getValue().atualizadoEmStr);
        colPercentual.setCellValueFactory(c -> c.getValue().percentual);

        // ProgressBar + label na célula
        colPercentual.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label lbl = new Label();
            private final HBox box = new HBox(8, bar, lbl);
            {
                bar.setMaxWidth(Double.MAX_VALUE);
                HBox.setMargin(bar, new Insets(2, 0, 2, 0));
            }
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    double p = Math.max(0, Math.min(1, value.doubleValue()/100.0));
                    bar.setProgress(p);
                    lbl.setText(value.intValue() + "%");
                    setGraphic(box);
                }
            }
        });

        // ===== Carga assíncrona (DAO) =====
        Task<List<AndamentoVM>> task = new Task<>() {
            @Override
            protected List<AndamentoVM> call() throws Exception {
                JdbcPainelOrientadorDao painelDao = new JdbcPainelOrientadorDao();
                JdbcKpiDao kpiDao = new JdbcKpiDao();
                List<PainelOrientadorRow> rows = painelDao.listarParaCoordenador();

                List<AndamentoVM> list = new ArrayList<>();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                for (PainelOrientadorRow r : rows) {
                    // Status conforme combinado: pendências == 0 → "Em dia"
                    int pendencias = r.getPendencias();
                    String status = (pendencias == 0) ? "Em dia" : "Atrasado";

                    // Percentual (DTO já traz double 0..100). Arredondamos para int p/ a barra.
                    int pct = (int) Math.round(r.getPercentual());
                    pct = Math.max(0, Math.min(100, pct));


                    // Atualizado: se o DTO original não tem data, deixamos "-"
                    String atualizado = "-";

                    // Curso será ignorado por ora ("—")
                    list.add(new AndamentoVM(
                            "—",                 // curso (ignorado agora)
                            r.getAluno(),        // aluno
                            r.getOrientador(),   // orientador
                            r.getTema(),         // tema
                            status,              // status
                            pct,                 // percentual
                            atualizado           // atualizado em
                    ));
                }
                return list;
            }
        };

        task.setOnSucceeded(ev -> {
            base.setAll(task.getValue());

            // Liga tabela aos dados filtráveis
            filtered = new FilteredList<>(base, a -> true);
            tblAndamento.setItems(filtered);


            // Prepara filtros (curso deixamos estático por enquanto)
            cbCurso.getItems().setAll("Todos", "ADS", "GTI");
            // cbCurso.getSelectionModel().selectFirst();
            cbCurso.setDisable(true);


            cbStatus.getItems().setAll("Todos", "Em dia", "Atrasado");
            cbStatus.getSelectionModel().selectFirst();

            txtBusca.textProperty().addListener((o,old,v) -> aplicarFiltros());
            cbCurso.getSelectionModel().selectedItemProperty().addListener((o,old,v) -> aplicarFiltros());
            cbStatus.getSelectionModel().selectedItemProperty().addListener((o,old,v) -> aplicarFiltros());

            // KPIs
            atualizarKPIs();

            // Placeholder caso não haja linhas
            if (loader != null) loader.setVisible(false);
            if (lblCarrega != null) lblCarrega.setVisible(false);
            tblAndamento.setPlaceholder(new Label("Sem registros."));
        });

        task.setOnFailed(ev -> {
            if (loader != null) loader.setVisible(false);
            if (lblCarrega != null) lblCarrega.setVisible(false);
            tblAndamento.setPlaceholder(new Label("Erro ao carregar dados."));
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Falha ao carregar Andamento");
            Throwable ex = task.getException();
            a.setContentText(ex != null ? ex.getMessage() : "Erro inesperado.");
            a.showAndWait();
        });



        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

// ===== fim carga assíncrona =====


        // Filtros
        filtered = new FilteredList<>(base, a -> true);
        tblAndamento.setItems(filtered);
        tblAndamento.setPlaceholder(new Label("Carregando dados..."));

        cbCurso.getItems().setAll("Todos", "ADS", "GTI");
        cbCurso.getSelectionModel().selectFirst();

        cbStatus.getSelectionModel().selectFirst();

        txtBusca.textProperty().addListener((o,old,v) -> aplicarFiltros());
        cbCurso.getSelectionModel().selectedItemProperty().addListener((o,old,v) -> aplicarFiltros());
        cbStatus.getSelectionModel().selectedItemProperty().addListener((o,old,v) -> aplicarFiltros());

        // KPIs
        atualizarKPIs();
    }

    private void aplicarFiltros() {
        final String busca = (txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim());
        final String curso = cbCurso.getValue() == null ? "Todos" : cbCurso.getValue();
        final String status = cbStatus.getValue() == null ? "Todos" : cbStatus.getValue();

        filtered.setPredicate(vm -> {
            boolean okCurso  = "Todos".equals(curso)  || vm.curso.get().equalsIgnoreCase(curso);
            boolean okStatus = "Todos".equals(status) || vm.status.get().equalsIgnoreCase(status);
            boolean okBusca  =
                    vm.aluno.get().toLowerCase().contains(busca) ||
                            vm.orientador.get().toLowerCase().contains(busca) ||
                            vm.tema.get().toLowerCase().contains(busca);
            return okCurso && okStatus && okBusca;
        });

        atualizarKPIs();
    }

    public void limparFiltros() {
        cbCurso.getSelectionModel().selectFirst();
        cbStatus.getSelectionModel().selectFirst();
        txtBusca.clear();
    }

    private void atualizarKPIs() {
        int ativos = filtered.size();
        int emDia = (int) filtered.stream().filter(v -> "Em dia".equalsIgnoreCase(v.status.get())).count();
        int atras = (int) filtered.stream().filter(v -> "Atrasado".equalsIgnoreCase(v.status.get())).count();
        lblAlunosAtivos.setText(Integer.toString(ativos));
        lblEmDia.setText(Integer.toString(emDia));
        lblAtrasados.setText(Integer.toString(atras));
    }

    private static String nowMinus(int days) {
        return LocalDateTime.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /* ===== VM ===== */
    public static class AndamentoVM {
        final StringProperty curso = new SimpleStringProperty();
        final StringProperty aluno = new SimpleStringProperty();
        final StringProperty orientador = new SimpleStringProperty();
        final StringProperty tema = new SimpleStringProperty();
        final StringProperty status = new SimpleStringProperty();
        final IntegerProperty percentual = new SimpleIntegerProperty();
        final StringProperty atualizadoEmStr = new SimpleStringProperty();
        public AndamentoVM(String c, String a, String o, String t, String s, int pct, String atual) {
            curso.set(c); aluno.set(a); orientador.set(o); tema.set(t); status.set(s);
            percentual.set(pct); atualizadoEmStr.set(atual);
        }
    }

    /* ===== Navegação ===== */
    public void goHomeOrient(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goMapa(){ SceneManager.go("coordenacao/Mapa.fxml"); }
    public void goAndamento(){ SceneManager.go("coordenacao/Andamento.fxml"); }
}
