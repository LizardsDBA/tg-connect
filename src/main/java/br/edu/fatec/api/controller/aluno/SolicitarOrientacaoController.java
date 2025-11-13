package br.edu.fatec.api.controller.aluno;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcSolicitacaoOrientacaoDao;
import br.edu.fatec.api.dto.OrientadorDisponivelDTO;
import br.edu.fatec.api.model.SolicitacaoOrientacao;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

public class SolicitarOrientacaoController extends BaseController {

    @FXML private TableView<OrientadorVM> tblOrientadores;
    @FXML private TableColumn<OrientadorVM, String> colNome;
    @FXML private TableColumn<OrientadorVM, String> colEmail;
    @FXML private TableColumn<OrientadorVM, Integer> colOrientandos;
    @FXML private TableColumn<OrientadorVM, String> colDisponivel;

    @FXML private TextArea txtMensagem;
    @FXML private Button btnSolicitar;
    @FXML private Label lblStatus;
    @FXML private VBox vboxForm;
    @FXML private VBox vboxAguardando;
    @FXML private Label lblStatusSolicitacao;
    @FXML private Label lblOrientadorSolicitado;
    @FXML private Label lblDataSolicitacao;
    @FXML private Button btnNovaSolicitacao;

    private final JdbcSolicitacaoOrientacaoDao dao = new JdbcSolicitacaoOrientacaoDao();
    private final ObservableList<OrientadorVM> orientadores = FXCollections.observableArrayList();
    private OrientadorVM orientadorSelecionado;
    private Long alunoId;

    @FXML
    private void initialize() {
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Verifica se o usuário está logado
        User user = Session.getUser();
        if (user == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        this.alunoId = user.getId();

        // Configura as colunas da tabela
        colNome.setCellValueFactory(d -> d.getValue().nome);
        colEmail.setCellValueFactory(d -> d.getValue().email);
        colOrientandos.setCellValueFactory(d -> d.getValue().orientandos.asObject());
        colDisponivel.setCellValueFactory(d -> d.getValue().disponivel);

        // Estilo para linha selecionada
        tblOrientadores.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
            orientadorSelecionado = novo;
            btnSolicitar.setDisable(novo == null);
        });

        // Carrega o status inicial
        verificarStatusAluno();
    }

    private void verificarStatusAluno() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Verifica se já tem orientador
                if (dao.alunoTemOrientador(alunoId)) {
                    return "TEM_ORIENTADOR";
                }

                // Verifica se tem solicitação pendente
                if (dao.alunoTemSolicitacaoPendente(alunoId)) {
                    return "TEM_PENDENTE";
                }

                // Verifica se tem solicitação recusada
                Optional<SolicitacaoOrientacao> ultima = dao.buscarUltimaSolicitacaoAluno(alunoId);
                if (ultima.isPresent() && ultima.get().getStatus() == SolicitacaoOrientacao.StatusSolicitacao.RECUSADA) {
                    return "RECUSADA";
                }

                return "PODE_SOLICITAR";
            }
        };

        task.setOnSucceeded(e -> {
            String status = task.getValue();

            switch (status) {
                case "TEM_ORIENTADOR" -> {
                    // Redireciona para o dashboard
                    SceneManager.go("aluno/Dashboard.fxml");
                }
                case "TEM_PENDENTE" -> {
                    // Mostra tela de aguardando resposta
                    mostrarTelaAguardando();
                }
                case "RECUSADA" -> {
                    // Mostra mensagem de recusa e permite nova solicitação
                    mostrarMensagemRecusa();
                    carregarOrientadores();
                }
                case "PODE_SOLICITAR" -> {
                    // Mostra formulário normal
                    vboxForm.setVisible(true);
                    vboxForm.setManaged(true);
                    vboxAguardando.setVisible(false);
                    vboxAguardando.setManaged(false);
                    carregarOrientadores();
                }
            }
        });

        task.setOnFailed(e -> {
            erro("Erro ao verificar status", (Exception) task.getException());
        });

        new Thread(task).start();
    }

    private void carregarOrientadores() {
        Task<List<OrientadorDisponivelDTO>> task = new Task<>() {
            @Override
            protected List<OrientadorDisponivelDTO> call() throws Exception {
                return dao.listarOrientadoresDisponiveis();
            }
        };

        task.setOnSucceeded(e -> {
            List<OrientadorDisponivelDTO> lista = task.getValue();
            orientadores.clear();

            for (OrientadorDisponivelDTO dto : lista) {
                orientadores.add(new OrientadorVM(
                        dto.id(),
                        dto.nome(),
                        dto.email(),
                        dto.totalOrientandos(),
                        dto.disponivel() ? "Disponível" : "Indisponível"
                ));
            }

            tblOrientadores.setItems(orientadores);

            if (orientadores.isEmpty()) {
                lblStatus.setText("Nenhum orientador disponível no momento.");
            } else {
                lblStatus.setText("Selecione um orientador e clique em 'Solicitar'");
            }
        });

        task.setOnFailed(e -> {
            erro("Erro ao carregar orientadores", (Exception) task.getException());
        });

        new Thread(task).start();
    }

    private void mostrarTelaAguardando() {
        vboxForm.setVisible(false);
        vboxForm.setManaged(false);
        vboxAguardando.setVisible(true);
        vboxAguardando.setManaged(true);

        Task<Optional<SolicitacaoOrientacao>> task = new Task<>() {
            @Override
            protected Optional<SolicitacaoOrientacao> call() throws Exception {
                return dao.buscarUltimaSolicitacaoAluno(alunoId);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<SolicitacaoOrientacao> opt = task.getValue();

            if (opt.isPresent()) {
                SolicitacaoOrientacao sol = opt.get();
                lblStatusSolicitacao.setText("Status: Aguardando resposta");
                lblOrientadorSolicitado.setText("Orientador: " + sol.getNomeOrientador());
                lblDataSolicitacao.setText("Data da solicitação: " +
                        sol.getDataSolicitacao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });

        new Thread(task).start();
    }

    private void mostrarMensagemRecusa() {
        Task<Optional<SolicitacaoOrientacao>> task = new Task<>() {
            @Override
            protected Optional<SolicitacaoOrientacao> call() throws Exception {
                return dao.buscarUltimaSolicitacaoAluno(alunoId);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<SolicitacaoOrientacao> opt = task.getValue();

            if (opt.isPresent()) {
                SolicitacaoOrientacao sol = opt.get();

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Solicitação Recusada");
                alert.setHeaderText("Sua solicitação foi recusada");
                alert.setContentText("Orientador: " + sol.getNomeOrientador() + "\n\n" +
                        "Justificativa:\n" + (sol.getJustificativaRecusa() != null ?
                        sol.getJustificativaRecusa() : "Sem justificativa"));
                alert.showAndWait();
            }
        });

        new Thread(task).start();
    }

    @FXML
    private void onSolicitar() {
        if (orientadorSelecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Atenção");
            alert.setHeaderText("Nenhum orientador selecionado");
            alert.setContentText("Por favor, selecione um orientador da lista.");
            alert.showAndWait();
            return;
        }

        String mensagem = txtMensagem.getText();

        // Confirmação
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Solicitação");
        confirm.setHeaderText("Deseja enviar solicitação para:");
        confirm.setContentText(orientadorSelecionado.nome.get());

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            enviarSolicitacao(orientadorSelecionado.id, mensagem);
        }
    }

    private void enviarSolicitacao(Long orientadorId, String mensagem) {
        btnSolicitar.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                dao.criarSolicitacao(alunoId, orientadorId, mensagem);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Sucesso");
            success.setHeaderText("Solicitação enviada!");
            success.setContentText("Aguarde a resposta do orientador.");
            success.showAndWait();

            // Mostra tela de aguardando
            mostrarTelaAguardando();
        });

        task.setOnFailed(e -> {
            erro("Erro ao enviar solicitação", (Exception) task.getException());
            btnSolicitar.setDisable(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void onAtualizarStatus() {
        verificarStatusAluno();
    }

    // Navegação
    public void goHome() { SceneManager.go("aluno/Dashboard.fxml"); }
    public void logout() { SceneManager.go("login/Login.fxml"); }

    private void erro(String mensagem, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(mensagem);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
        e.printStackTrace();
    }

    // View Model para a tabela
    public static class OrientadorVM {
        final Long id;
        final SimpleStringProperty nome = new SimpleStringProperty();
        final SimpleStringProperty email = new SimpleStringProperty();
        final SimpleIntegerProperty orientandos = new SimpleIntegerProperty();
        final SimpleStringProperty disponivel = new SimpleStringProperty();

        public OrientadorVM(Long id, String nome, String email, int orientandos, String disponivel) {
            this.id = id;
            this.nome.set(nome);
            this.email.set(email);
            this.orientandos.set(orientandos);
            this.disponivel.set(disponivel);
        }
    }
}