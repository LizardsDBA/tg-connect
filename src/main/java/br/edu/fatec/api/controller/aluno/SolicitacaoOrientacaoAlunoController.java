package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcSolicitacaoOrientacaoDao;
import br.edu.fatec.api.dao.JdbcSolicitacaoOrientacaoDao.OrientadorDisponivel;
import br.edu.fatec.api.model.SolicitacaoOrientacao;
import br.edu.fatec.api.model.SolicitacaoOrientacao.StatusSolicitacao;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SolicitacaoOrientacaoAlunoController extends BaseController {

    @FXML private VBox containerSolicitacao;
    @FXML private VBox containerAguardando;
    @FXML private VBox containerRecusada;

    @FXML private TableView<OrientadorDisponivel> tblOrientadores;
    @FXML private TableColumn<OrientadorDisponivel, String> colNome;
    @FXML private TableColumn<OrientadorDisponivel, String> colEmail;
    @FXML private Button btnSolicitar;

    @FXML private Label lblStatusSolicitacao;
    @FXML private Label lblOrientadorSolicitado;
    @FXML private Label lblDataSolicitacao;

    @FXML private Label lblOrientadorRecusado;
    @FXML private Label lblJustificativa;
    @FXML private Button btnNovaSolicitacao;

    private final JdbcSolicitacaoOrientacaoDao dao = new JdbcSolicitacaoOrientacaoDao();
    private final ObservableList<OrientadorDisponivel> orientadores = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long alunoId;

    @FXML
    private void initialize() {
        User user = Session.getUser();
        if (user == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        this.alunoId = user.getId();

        // Desabilitar todos os botões da sidebar exceto Dashboard
        desabilitarBotoesSidebar();

        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nome()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email()));
        tblOrientadores.setItems(orientadores);

        btnSolicitar.setOnAction(e -> enviarSolicitacao());
        btnNovaSolicitacao.setOnAction(e -> novaSolicitacao());

        verificarStatus();
    }

    private void desabilitarBotoesSidebar() {
        if (sidebar != null) {
            sidebar.getChildren().forEach(node -> {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    String text = btn.getText();
                    if (!text.equals("Dashboard")) {
                        btn.setDisable(true);
                        btn.setStyle("-fx-opacity: 0.5;");
                    }
                }
            });
        }
    }

    private void verificarStatus() {
        try {
            if (dao.alunoTemOrientacaoAtiva(alunoId)) {
                SceneManager.go("aluno/Dashboard.fxml");
                return;
            }

            if (dao.alunoTemSolicitacaoPendente(alunoId)) {
                exibirStatusPendente();
                return;
            }

            dao.buscarUltimaSolicitacaoAluno(alunoId).ifPresentOrElse(
                    sol -> {
                        if (sol.getStatus() == StatusSolicitacao.RECUSADA) {
                            exibirStatusRecusada(sol);
                        } else {
                            exibirFormularioSolicitacao();
                        }
                    },
                    this::exibirFormularioSolicitacao
            );

        } catch (SQLException e) {
            mostrarErro("Erro ao verificar status", e.getMessage());
        }
    }

    private void exibirFormularioSolicitacao() {
        containerSolicitacao.setVisible(true);
        containerSolicitacao.setManaged(true);
        containerAguardando.setVisible(false);
        containerAguardando.setManaged(false);
        containerRecusada.setVisible(false);
        containerRecusada.setManaged(false);

        carregarOrientadores();
    }

    private void exibirStatusPendente() {
        containerSolicitacao.setVisible(false);
        containerSolicitacao.setManaged(false);
        containerAguardando.setVisible(true);
        containerAguardando.setManaged(true);
        containerRecusada.setVisible(false);
        containerRecusada.setManaged(false);

        try {
            dao.buscarUltimaSolicitacaoAluno(alunoId).ifPresent(sol -> {
                lblOrientadorSolicitado.setText(sol.getNomeOrientador());
                lblDataSolicitacao.setText(sol.getDataSolicitacao().format(formatter));
                lblStatusSolicitacao.setText("Aguardando aprovação do orientador");
            });
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar solicitação", e.getMessage());
        }
    }

    private void exibirStatusRecusada(SolicitacaoOrientacao sol) {
        containerSolicitacao.setVisible(false);
        containerSolicitacao.setManaged(false);
        containerAguardando.setVisible(false);
        containerAguardando.setManaged(false);
        containerRecusada.setVisible(true);
        containerRecusada.setManaged(true);

        lblOrientadorRecusado.setText(sol.getNomeOrientador());
        lblJustificativa.setText(sol.getJustificativa() != null ? sol.getJustificativa() : "Sem justificativa");
    }

    private void carregarOrientadores() {
        try {
            List<OrientadorDisponivel> lista = dao.listarOrientadoresDisponiveis();
            orientadores.clear();
            orientadores.addAll(lista);
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar orientadores", e.getMessage());
        }
    }

    private void enviarSolicitacao() {
        OrientadorDisponivel selecionado = tblOrientadores.getSelectionModel().getSelectedItem();

        if (selecionado == null) {
            mostrarAviso("Nenhum orientador selecionado", "Selecione um orientador da lista");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Solicitação");
        confirmacao.setHeaderText("Deseja solicitar orientação de:");
        confirmacao.setContentText(selecionado.nome());

        confirmacao.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    dao.criarSolicitacao(alunoId, selecionado.id());
                    mostrarSucesso("Solicitação enviada",
                            "Sua solicitação foi enviada para " + selecionado.nome());
                    verificarStatus();
                } catch (SQLException e) {
                    mostrarErro("Erro ao enviar solicitação", e.getMessage());
                }
            }
        });
    }

    private void novaSolicitacao() {
        exibirFormularioSolicitacao();
    }

    // Métodos auxiliares para exibir alertas
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
        SceneManager.go("aluno/Dashboard.fxml");
    }

    public void logout() {
        SceneManager.go("login/Login.fxml");
    }

    public void goDashboard() {
        SceneManager.go("aluno/Dashboard.fxml");
    }

    public void goInbox() {
        SceneManager.go("aluno/Inbox.fxml", c -> {
            InboxAlunoController ctrl = (InboxAlunoController) c;
            User u = Session.getUser();
            if (u == null) {
                SceneManager.go("login/Login.fxml");
                return;
            }
            ctrl.setAlunoContext(u.getId());
            ctrl.onReady();
        });
    }

    public void goEditor() {
        SceneManager.go("aluno/Editor.fxml", c -> {
            EditorAlunoController ctrl = (EditorAlunoController) c;
            ctrl.onReady();
        });
    }

    public void goComparar() {
        SceneManager.go("aluno/Comparar.fxml");
    }

    public void goConclusao() {
        SceneManager.go("aluno/Conclusao.fxml");
    }

    public void goHistorico() {
        SceneManager.go("aluno/Historico.fxml");
    }
}