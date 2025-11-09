package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.dto.PainelOrientadorRow;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.dao.JdbcPainelOrientadorDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList; // NOVO IMPORT
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import br.edu.fatec.api.controller.BaseController;

import br.edu.fatec.api.model.auth.Role;

import java.util.function.Predicate; // NOVO IMPORT

public class PainelOrientadorController extends BaseController {

    // Sidebar – rota ativa
    @FXML private Button btnPainel;
    @FXML private Button btnSouCoordenador;

    // Filtros
    @FXML private TextField txtBuscaAluno;
    @FXML private CheckBox chkAguardandoRevisao; // NOVO

    // Tabela
    @FXML private TableView<PainelOrientadorRow> tblPendencias;
    @FXML private TableColumn<PainelOrientadorRow, String> colAluno, colTitulo, colTema, colVersao, colStatus;
    @FXML private TableColumn<PainelOrientadorRow, Number> colValidadas, colPendencias;
    @FXML private TableColumn<PainelOrientadorRow, PainelOrientadorRow> colProgresso;

    private final JdbcPainelOrientadorDao dao = new JdbcPainelOrientadorDao();

    // ATUALIZADO: Precisamos da lista original e da filtrada
    private ObservableList<PainelOrientadorRow> masterData = FXCollections.observableArrayList();
    private FilteredList<PainelOrientadorRow> filteredData;

    @FXML
    private void initialize() {
        if (btnPainel != null && !btnPainel.getStyleClass().contains("active")) {
            btnPainel.getStyleClass().add("active");
        }
        if (tblPendencias != null) {
            tblPendencias.setPlaceholder(new Label("Sem pendências no momento."));
        }
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Bindings das colunas (OK)
        colAluno.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getAluno));
        colTitulo.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getTitulo));
        colTema.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getTema));
        colVersao.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getVersaoAtual));
        colValidadas.setCellValueFactory(c -> javafx.beans.binding.Bindings.createIntegerBinding(c.getValue()::getTotalValidadas));
        colPendencias.setCellValueFactory(c -> javafx.beans.binding.Bindings.createIntegerBinding(c.getValue()::getPendencias));

        // ATUALIZADO: Status agora mostra o status do fluxo
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    getStyleClass().removeAll("badge-ok", "badge-pendente", "badge-reprovado");
                } else {
                    String texto;
                    String styleClass;

                    switch (status) {
                        case "ENTREGUE" -> {
                            texto = "Aguardando Revisão";
                            styleClass = "badge-pendente"; // Amarelo/Laranja (Precisa de ação)
                        }
                        case "APROVADO" -> {
                            texto = "Concluído";
                            styleClass = "badge-ok"; // Verde
                        }
                        case "REPROVADO" -> {
                            texto = "Revisado (Pendências)";
                            styleClass = "badge-reprovado"; // Vermelho
                        }
                        default -> { // EM_ANDAMENTO
                            texto = "Em Andamento";
                            styleClass = ""; // Sem cor
                        }
                    }
                    setText(texto);
                    getStyleClass().removeAll("badge-ok", "badge-pendente", "badge-reprovado");
                    if (!styleClass.isEmpty()) {
                        getStyleClass().add(styleClass);
                    }
                }
            }
        });
        colStatus.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getStatus));

        // Progresso (OK)
        colProgresso.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colProgresso.setCellFactory(col -> new TableCell<>() {
            // ... (Seu código original da barra de progresso está perfeito) ...
            private final ProgressBar bar = new ProgressBar(0);
            private final Label lbl = new Label("0%");
            private final HBox box = new HBox(8, bar, lbl);
            {
                box.setPadding(new Insets(2, 0, 2, 0));
                bar.setPrefWidth(120);
            }
            @Override protected void updateItem(PainelOrientadorRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                } else {
                    double pct = row.getPercentual();
                    double pct01 = Math.max(0, Math.min(1, pct / 100.0));
                    bar.setProgress(pct01);
                    lbl.setText(String.format("%.0f%%", pct));
                    bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("low"), false);
                    bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("medium"), false);
                    bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("high"), false);
                    if (pct < 30) {
                        bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("low"), true);
                    } else if (pct < 70) {
                        bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("medium"), true);
                    } else {
                        bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("high"), true);
                    }
                    setGraphic(box);
                }
            }
        });

        // Carregar dados
        User u = Session.getUser();
        if (u == null) { SceneManager.go("login/Login.fxml"); return; }

        // ATUALIZADO: Configura o FilteredList
        masterData.addAll(dao.listar(u.getId()));
        filteredData = new FilteredList<>(masterData, r -> true);
        tblPendencias.setItems(filteredData);

        // ATUALIZADO: Listeners para os filtros combinados
        txtBuscaAluno.textProperty().addListener((obs, old, val) -> aplicarFiltros());
        chkAguardandoRevisao.selectedProperty().addListener((obs, old, val) -> aplicarFiltros());

        // ... (Seu código do btnSouCoordenador está OK) ...
        boolean isCoord = (u != null && u.getRole() == Role.COORDENADOR);
        if (btnSouCoordenador != null) {
            btnSouCoordenador.setVisible(isCoord);
            btnSouCoordenador.setManaged(isCoord);
        }
    }

    private void aplicarFiltros() {
        String busca = txtBuscaAluno.getText() == null ? "" : txtBuscaAluno.getText().trim().toLowerCase();
        boolean somenteEntregues = chkAguardandoRevisao.isSelected();

        Predicate<PainelOrientadorRow> filtroTexto = row ->
                busca.isEmpty() || row.getAluno().toLowerCase().contains(busca);

        Predicate<PainelOrientadorRow> filtroStatus = row ->
                !somenteEntregues || row.getStatus().equals("ENTREGUE");

        filteredData.setPredicate(filtroTexto.and(filtroStatus));
    }

    // ===== Ações da tabela/toolbar =====
    public void limparFiltros() {
        if (txtBuscaAluno != null) txtBuscaAluno.clear();
        if (chkAguardandoRevisao != null) chkAguardandoRevisao.setSelected(false);
        // aplicarFiltros(); // O listener do checkbox já chama isso
    }

    public void abrirEditor() {
        SceneManager.go("orientador/Editor.fxml");
    }

    public void goHistorico() {
        SceneManager.go("orientador/Historico.fxml");
    }    // ... (Restante dos seus métodos de navegação) ...
    public void abrirChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }
    public void goHomeCoord(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }
}