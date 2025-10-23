package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.dto.PainelOrientadorRow;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.dao.JdbcPainelOrientadorDao;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import br.edu.fatec.api.model.auth.Role;

public class PainelOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnPainel;
    @FXML private Button btnSouCoordenador;


    // Filtros
    @FXML private TextField txtBuscaAluno;

    // Tabela
    @FXML private TableView<PainelOrientadorRow> tblPendencias;
    @FXML private TableColumn<PainelOrientadorRow, String> colAluno, colTitulo, colTema, colVersao, colStatus;
    @FXML private TableColumn<PainelOrientadorRow, Number> colValidadas, colPendencias;
    @FXML private TableColumn<PainelOrientadorRow, PainelOrientadorRow> colProgresso;

    private final JdbcPainelOrientadorDao dao = new JdbcPainelOrientadorDao();
    private FilteredList<PainelOrientadorRow> filtered;

    @FXML
    private void initialize() {
        if (btnPainel != null && !btnPainel.getStyleClass().contains("active")) {
            btnPainel.getStyleClass().add("active");
        }
        if (tblPendencias != null) {
            tblPendencias.setPlaceholder(new Label("Sem pendências no momento."));
        }

        // Bindings das colunas simples
        colAluno.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getAluno));
        colTitulo.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getTitulo));
        colTema.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getTema));
        colVersao.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getVersaoAtual));
        colValidadas.setCellValueFactory(c -> javafx.beans.binding.Bindings.createIntegerBinding(c.getValue()::getTotalValidadas));
        colPendencias.setCellValueFactory(c -> javafx.beans.binding.Bindings.createIntegerBinding(c.getValue()::getPendencias));

        // Status com emoji (🔴🟡🟢)
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    String decorated = switch (status) {
                        case "Concluído"     -> "Concluído ";
                        case "Não avaliado"  -> "Não avaliado ";
                        default               -> "Em andamento";
                    };
                    setText(decorated);
                }
            }
        });
        colStatus.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(c.getValue()::getStatus));

        // Progresso: barra + label "XX%"
        colProgresso.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colProgresso.setCellFactory(col -> new TableCell<>() {
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

                    // Remove pseudo-classes anteriores
                    bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("low"), false);
                    bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("medium"), false);
                    bar.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("high"), false);

                    // Define cor conforme progresso
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

        var data = FXCollections.observableArrayList(dao.listar(u.getId()));
        filtered = new FilteredList<>(data, r -> true);
        tblPendencias.setItems(filtered);

        // Filtro por nome do aluno
        txtBuscaAluno.textProperty().addListener((obs, old, val) -> {
            String q = val == null ? "" : val.trim().toLowerCase();
            filtered.setPredicate(row ->
                    q.isEmpty() || row.getAluno().toLowerCase().contains(q)
            );
        });

        boolean isCoord = (u != null && u.getRole() == Role.COORDENADOR);
        if (btnSouCoordenador != null) {
            btnSouCoordenador.setVisible(isCoord);
            btnSouCoordenador.setManaged(isCoord); // evita “buraco” no layout quando oculto
        }
    }

    // ===== Ações da tabela/toolbar =====
    public void limparFiltros() {
        if (txtBuscaAluno != null) txtBuscaAluno.clear();
        if (filtered != null) filtered.setPredicate(r -> true);
    }

    public void abrirEditor() {
        SceneManager.go("orientador/Editor.fxml");
    }

    public void abrirChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    // ===== Navegação =====
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
