package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcSolicitacaoOrientacaoDao;
import br.edu.fatec.api.model.SolicitacaoOrientacao;
import br.edu.fatec.api.model.SolicitacaoOrientacao.StatusSolicitacao;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SolicitacoesOrientadorController extends BaseController {

    @FXML private TableView<SolicitacaoOrientacao> tblSolicitacoes;
    @FXML private TableColumn<SolicitacaoOrientacao, String> colAluno;
    @FXML private TableColumn<SolicitacaoOrientacao, String> colData;
    @FXML private TableColumn<SolicitacaoOrientacao, String> colStatus;

    @FXML private VBox containerDetalhes;
    @FXML private Label lblNomeAluno;
    @FXML private Label lblDataSolicitacao;
    @FXML private Label lblStatusAtual;
    @FXML private TextArea txtJustificativa;

    // NOVOS LABELS
    @FXML private Label lblTituloTg;
    @FXML private Label lblTemaTg;

    @FXML private Button btnAprovar;
    @FXML private Button btnRecusar;
    @FXML private Button btnSouCoordenador;
    @FXML private ComboBox<String> cmbFiltroStatus;

    private final JdbcSolicitacaoOrientacaoDao dao = new JdbcSolicitacaoOrientacaoDao();
    private final ObservableList<SolicitacaoOrientacao> solicitacoes = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long orientadorId;
    private SolicitacaoOrientacao solicitacaoSelecionada;

    @FXML
    private void initialize() {
        User user = Session.getUser();
        if (user == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        this.orientadorId = user.getId();

        // Esconder botão "Sou Coordenador" se não for coordenador
        if (btnSouCoordenador != null) {
            boolean isCoordenador = user.getRole() == Role.COORDENADOR;
            btnSouCoordenador.setVisible(isCoordenador);
            btnSouCoordenador.setManaged(isCoordenador);
        }

        colAluno.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeAluno()));
        colData.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDataSolicitacao().format(formatter)));
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(traduzirStatus(data.getValue().getStatus())));

        tblSolicitacoes.setItems(solicitacoes);

        tblSolicitacoes.getSelectionModel().selectedItemProperty().addListener((obs, old, nova) -> {
            if (nova != null) {
                exibirDetalhes(nova);
            }
        });

        cmbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todas", "Pendentes", "Aprovadas", "Recusadas"
        ));
        cmbFiltroStatus.setValue("Pendentes");
        cmbFiltroStatus.setOnAction(e -> carregarSolicitacoes());

        btnAprovar.setOnAction(e -> aprovarSolicitacao());
        btnRecusar.setOnAction(e -> recusarSolicitacao());

        containerDetalhes.setVisible(false);
        containerDetalhes.setManaged(false);

        carregarSolicitacoes();
    }

    private void carregarSolicitacoes() {
        try {
            String filtro = cmbFiltroStatus.getValue();
            StatusSolicitacao status = switch (filtro) {
                case "Pendentes" -> StatusSolicitacao.PENDENTE;
                case "Aprovadas" -> StatusSolicitacao.APROVADA;
                case "Recusadas" -> StatusSolicitacao.RECUSADA;
                default -> null;
            };

            List<SolicitacaoOrientacao> lista = dao.listarSolicitacoesPorOrientador(orientadorId, status);
            solicitacoes.clear();
            solicitacoes.addAll(lista);

            containerDetalhes.setVisible(false);
            containerDetalhes.setManaged(false);

        } catch (SQLException e) {
            mostrarErro("Erro ao carregar solicitações", e.getMessage());
        }
    }

    private void exibirDetalhes(SolicitacaoOrientacao sol) {
        this.solicitacaoSelecionada = sol;

        lblNomeAluno.setText(sol.getNomeAluno());

        // ATUALIZADO: Preenche título e tema (com fallback se for nulo)
        if (lblTituloTg != null) lblTituloTg.setText(sol.getTituloTg() != null ? sol.getTituloTg() : "Não informado");
        if (lblTemaTg != null) lblTemaTg.setText(sol.getTemaTg() != null ? sol.getTemaTg() : "Não informado");

        lblDataSolicitacao.setText(sol.getDataSolicitacao().format(formatter));
        lblStatusAtual.setText(traduzirStatus(sol.getStatus()));

        boolean isPendente = sol.getStatus() == StatusSolicitacao.PENDENTE;
        btnAprovar.setVisible(isPendente);
        btnAprovar.setManaged(isPendente);
        btnRecusar.setVisible(isPendente);
        btnRecusar.setManaged(isPendente);
        txtJustificativa.setVisible(isPendente);
        txtJustificativa.setManaged(isPendente);

        if (!isPendente && sol.getJustificativa() != null) {
            txtJustificativa.setText(sol.getJustificativa());
            txtJustificativa.setEditable(false);
            txtJustificativa.setVisible(true);
            txtJustificativa.setManaged(true);
        } else {
            txtJustificativa.clear();
            txtJustificativa.setEditable(true);
        }

        containerDetalhes.setVisible(true);
        containerDetalhes.setManaged(true);
    }

    private void aprovarSolicitacao() {
        if (solicitacaoSelecionada == null) return;

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Aprovação");
        confirmacao.setHeaderText("Deseja aprovar a solicitação de:");
        confirmacao.setContentText(solicitacaoSelecionada.getNomeAluno());

        confirmacao.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    dao.aprovarSolicitacao(solicitacaoSelecionada.getId());
                    mostrarSucesso("Solicitação aprovada",
                            "O aluno foi adicionado à sua lista de orientandos");
                    carregarSolicitacoes();
                } catch (SQLException e) {
                    mostrarErro("Erro ao aprovar solicitação", e.getMessage());
                }
            }
        });
    }

    private void recusarSolicitacao() {
        if (solicitacaoSelecionada == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Recusar Solicitação");
        dialog.setHeaderText("Recusar solicitação de: " + solicitacaoSelecionada.getNomeAluno());

        ButtonType btnConfirmar = new ButtonType("Recusar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label lblJust = new Label("Justificativa:");
        TextArea txtJust = new TextArea();
        txtJust.setPromptText("Digite a justificativa da recusa (obrigatório)");
        txtJust.setPrefRowCount(4);
        txtJust.setWrapText(true);

        grid.add(lblJust, 0, 0);
        grid.add(txtJust, 0, 1);

        dialog.getDialogPane().setContent(grid);

        Button btnConfirmarNode = (Button) dialog.getDialogPane().lookupButton(btnConfirmar);
        btnConfirmarNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtJust.getText().trim().isEmpty()) {
                mostrarAviso("Justificativa obrigatória",
                        "É necessário informar uma justificativa para recusar a solicitação");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmar) {
                return txtJust.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(justificativa -> {
            try {
                dao.recusarSolicitacao(solicitacaoSelecionada.getId(), justificativa);
                mostrarSucesso("Solicitação recusada",
                        "O aluno foi notificado sobre a recusa");
                carregarSolicitacoes();
            } catch (SQLException e) {
                mostrarErro("Erro ao recusar solicitação", e.getMessage());
            }
        });
    }

    private String traduzirStatus(StatusSolicitacao status) {
        return switch (status) {
            case PENDENTE -> "Pendente";
            case APROVADA -> "Aprovada";
            case RECUSADA -> "Recusada";
        };
    }

    // Métodos auxiliares para alertas
    private void mostrarErro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void mostrarAviso(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void mostrarSucesso(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sucesso");
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    // ============ MÉTODOS DE NAVEGAÇÃO ============

    public void goHome() {
        SceneManager.go("orientador/VisaoGeral.fxml");
    }

    public void goHomeCoord() {
        SceneManager.go("coordenacao/VisaoGeral.fxml");
    }

    public void logout() {
        SceneManager.go("login/Login.fxml");
    }

    public void goVisaoGeral() {
        SceneManager.go("orientador/VisaoGeral.fxml");
    }

    public void goPainel() {
        SceneManager.go("orientador/Painel.fxml");
    }

    public void goSolicitacoes() {
        SceneManager.go("orientador/SolicitacoesOrientador.fxml");
    }

    public void goEditor() {
        SceneManager.go("orientador/Editor.fxml");
    }

    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }

    public void goHistorico() {
        SceneManager.go("orientador/Historico.fxml", c -> {
            HistoricoOrientadorController ctrl = (HistoricoOrientadorController) c;
            ctrl.onRefreshData();
        });
    }
}