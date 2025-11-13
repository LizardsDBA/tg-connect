package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcSolicitacaoOrientacaoDao;
import br.edu.fatec.api.model.SolicitacaoOrientacao;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class NotificacoesSolicitacoesController extends BaseController {

    @FXML private Button btnNotificacoes;
    @FXML private TableView<SolicitacaoVM> tblSolicitacoes;
    @FXML private TableColumn<SolicitacaoVM, String> colAluno;
    @FXML private TableColumn<SolicitacaoVM, String> colEmail;
    @FXML private TableColumn<SolicitacaoVM, String> colData;
    @FXML private TableColumn<SolicitacaoVM, String> colStatus;
    @FXML private Label lblTotalPendentes;
    @FXML private CheckBox chkApenasPendentes;

    private final JdbcSolicitacaoOrientacaoDao dao = new JdbcSolicitacaoOrientacaoDao();
    private final ObservableList<SolicitacaoVM> solicitacoes = FXCollections.observableArrayList();
    private Long orientadorId;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        // Marca rota ativa
        if (btnNotificacoes != null && !btnNotificacoes.getStyleClass().contains("active")) {
            btnNotificacoes.getStyleClass().add("active");
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Verifica login
        User user = Session.getUser();
        if (user == null) {
            SceneManager.go("login/Login.fxml");
            return;
        }

        this.orientadorId = user.getId();

        // Configura colunas
        colAluno.setCellValueFactory(d -> d.getValue().nomeAluno);
        colEmail.setCellValueFactory(d -> d.getValue().emailAluno);
        colData.setCellValueFactory(d -> d.getValue().dataFormatada);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        // Estilo para a coluna de status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);

                    // Aplica estilos baseado no status
                    switch (item) {
                        case "PENDENTE" -> setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
                        case "APROVADA" -> setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                        case "RECUSADA" -> setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        // Adiciona coluna de ações
        TableColumn<SolicitacaoVM, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setPrefWidth(200);

        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnAprovar = new Button("Aprovar");
            private final Button btnRecusar = new Button("Recusar");
            private final Button btnVisualizar = new Button("Visualizar");
            private final HBox pane = new HBox(8);

            {
                btnAprovar.getStyleClass().add("btn-primary");
                btnRecusar.getStyleClass().add("btn-danger");

                btnAprovar.setOnAction(e -> {
                    SolicitacaoVM vm = getTableView().getItems().get(getIndex());
                    aprovarSolicitacao(vm);
                });

                btnRecusar.setOnAction(e -> {
                    SolicitacaoVM vm = getTableView().getItems().get(getIndex());
                    abrirModalRecusa(vm);
                });

                btnVisualizar.setOnAction(e -> {
                    SolicitacaoVM vm = getTableView().getItems().get(getIndex());
                    visualizarSolicitacao(vm);
                });

                pane.getChildren().addAll(btnAprovar, btnRecusar, btnVisualizar);
                pane.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    SolicitacaoVM vm = getTableView().getItems().get(getIndex());
                    boolean isPendente = "PENDENTE".equals(vm.status.get());

                    btnAprovar.setVisible(isPendente);
                    btnRecusar.setVisible(isPendente);
                    btnVisualizar.setVisible(!isPendente);

                    setGraphic(pane);
                }
            }
        });

        tblSolicitacoes.getColumns().add(colAcoes);

        // Listener para o checkbox
        if (chkApenasPendentes != null) {
            chkApenasPendentes.setSelected(true);
            chkApenasPendentes.selectedProperty().addListener((obs, old, novo) -> {
                carregarSolicitacoes();
            });
        }

        // Carrega dados
        carregarSolicitacoes();
    }

    private void carregarSolicitacoes() {
        boolean apenasPendentes = chkApenasPendentes != null && chkApenasPendentes.isSelected();

        Task<List<SolicitacaoOrientacao>> task = new Task<>() {
            @Override
            protected List<SolicitacaoOrientacao> call() throws Exception {
                if (apenasPendentes) {
                    return dao.listarSolicitacoesPendentes(orientadorId);
                } else {
                    return dao.listarTodasSolicitacoes(orientadorId);
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<SolicitacaoOrientacao> lista = task.getValue();
            solicitacoes.clear();

            int pendentes = 0;

            for (SolicitacaoOrientacao sol : lista) {
                if (sol.getStatus() == SolicitacaoOrientacao.StatusSolicitacao.PENDENTE) {
                    pendentes++;
                }

                solicitacoes.add(new SolicitacaoVM(
                        sol.getId(),
                        sol.getAlunoId(),
                        sol.getNomeAluno(),
                        sol.getEmailAluno(),
                        sol.getMensagemAluno(),
                        sol.getJustificativaRecusa(),
                        sol.getDataSolicitacao().format(formatter),
                        sol.getStatus().name()
                ));
            }

            tblSolicitacoes.setItems(solicitacoes);

            if (lblTotalPendentes != null) {
                lblTotalPendentes.setText(String.valueOf(pendentes));
            }
        });

        task.setOnFailed(e -> {
            erro("Erro ao carregar solicitações", (Exception) task.getException());
        });

        new Thread(task).start();
    }

    private void aprovarSolicitacao(SolicitacaoVM vm) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Aprovação");
        confirm.setHeaderText("Deseja aprovar a solicitação de:");
        confirm.setContentText(vm.nomeAluno.get());

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    dao.aprovarSolicitacao(vm.id);
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Sucesso");
                success.setHeaderText("Solicitação aprovada!");
                success.setContentText("O aluno " + vm.nomeAluno.get() + " foi adicionado como seu orientando.");
                success.showAndWait();

                carregarSolicitacoes();
            });

            task.setOnFailed(e -> {
                erro("Erro ao aprovar solicitação", (Exception) task.getException());
            });

            new Thread(task).start();
        }
    }

    private void abrirModalRecusa(SolicitacaoVM vm) {
        // Cria modal
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Recusar Solicitação");
        modal.setWidth(500);
        modal.setHeight(400);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());

        Label titulo = new Label("Recusar solicitação de: " + vm.nomeAluno.get());
        titulo.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Label subtitulo = new Label("Por favor, informe uma justificativa para a recusa:");

        // Checkboxes de justificativas pré-definidas
        VBox vboxOpcoes = new VBox(8);
        CheckBox chk1 = new CheckBox("No momento, minha carga de trabalho e compromissos acadêmicos não me permitem oferecer a dedicação necessária a uma nova orientação.");
        CheckBox chk2 = new CheckBox("No momento, já oriento o número máximo de alunos permitido, o que me impede de oferecer a atenção necessária a uma nova orientação.");
        CheckBox chk3 = new CheckBox("Estou temporariamente sem disponibilidade para novas orientações em razão de compromissos acadêmicos e institucionais já assumidos.");

        chk1.setWrapText(true);
        chk2.setWrapText(true);
        chk3.setWrapText(true);

        vboxOpcoes.getChildren().addAll(chk1, chk2, chk3);

        Label lblOu = new Label("Ou escreva uma justificativa personalizada:");

        TextArea txtJustificativa = new TextArea();
        txtJustificativa.setPromptText("Escreva aqui sua justificativa...");
        txtJustificativa.setPrefRowCount(5);
        txtJustificativa.setWrapText(true);
        VBox.setVgrow(txtJustificativa, Priority.ALWAYS);

        HBox botoes = new HBox(12);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnConfirmar = new Button("Confirmar Recusa");
        btnConfirmar.getStyleClass().add("btn-danger");
        btnConfirmar.setOnAction(e -> {
            StringBuilder justificativa = new StringBuilder();

            if (chk1.isSelected()) justificativa.append("• ").append(chk1.getText()).append("\n\n");
            if (chk2.isSelected()) justificativa.append("• ").append(chk2.getText()).append("\n\n");
            if (chk3.isSelected()) justificativa.append("• ").append(chk3.getText()).append("\n\n");

            String textoCustom = txtJustificativa.getText().trim();
            if (!textoCustom.isEmpty()) {
                justificativa.append(textoCustom);
            }

            if (justificativa.length() == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Atenção");
                alert.setHeaderText("Justificativa obrigatória");
                alert.setContentText("Por favor, selecione ao menos uma opção ou escreva uma justificativa.");
                alert.showAndWait();
                return;
            }

            recusarSolicitacao(vm, justificativa.toString());
            modal.close();
        });

        botoes.getChildren().addAll(btnCancelar, btnConfirmar);

        root.getChildren().addAll(titulo, subtitulo, vboxOpcoes, lblOu, txtJustificativa, botoes);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        modal.setScene(scene);
        modal.showAndWait();
    }

    private void recusarSolicitacao(SolicitacaoVM vm, String justificativa) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                dao.recusarSolicitacao(vm.id, justificativa);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Sucesso");
            success.setHeaderText("Solicitação recusada");
            success.setContentText("O aluno " + vm.nomeAluno.get() + " foi notificado sobre a recusa.");
            success.showAndWait();

            carregarSolicitacoes();
        });

        task.setOnFailed(e -> {
            erro("Erro ao recusar solicitação", (Exception) task.getException());
        });

        new Thread(task).start();
    }

    private void visualizarSolicitacao(SolicitacaoVM vm) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalhes da Solicitação");
        alert.setHeaderText("Solicitação de: " + vm.nomeAluno.get());

        StringBuilder conteudo = new StringBuilder();
        conteudo.append("Status: ").append(vm.status.get()).append("\n");
        conteudo.append("Data: ").append(vm.dataFormatada.get()).append("\n");
        conteudo.append("E-mail: ").append(vm.emailAluno.get()).append("\n\n");

        if (vm.mensagemAluno != null && !vm.mensagemAluno.isEmpty()) {
            conteudo.append("Mensagem do aluno:\n").append(vm.mensagemAluno).append("\n\n");
        }

        if (vm.justificativaRecusa != null && !vm.justificativaRecusa.isEmpty()) {
            conteudo.append("Justificativa da recusa:\n").append(vm.justificativaRecusa);
        }

        alert.setContentText(conteudo.toString());
        alert.showAndWait();
    }

    @FXML
    private void onAtualizar() {
        carregarSolicitacoes();
    }

    // Navegação (adapte conforme suas rotas)
    public void goHome() { SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout() { SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral() { SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel() { SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes() { SceneManager.go("orientador/NotificacoesSolicitacoes.fxml"); }

    public void goHomeCoord() {
        User u = Session.getUser();
        if (u != null && u.getRole() == br.edu.fatec.api.model.auth.Role.COORDENADOR) {
            SceneManager.go("coordenacao/VisaoGeral.fxml");
        }
    }

    private void erro(String mensagem, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(mensagem);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
        e.printStackTrace();
    }

    // View Model
    public static class SolicitacaoVM {
        final Long id;
        final Long alunoId;
        final SimpleStringProperty nomeAluno = new SimpleStringProperty();
        final SimpleStringProperty emailAluno = new SimpleStringProperty();
        final String mensagemAluno;
        final String justificativaRecusa;
        final SimpleStringProperty dataFormatada = new SimpleStringProperty();
        final SimpleStringProperty status = new SimpleStringProperty();

        public SolicitacaoVM(Long id, Long alunoId, String nomeAluno, String emailAluno,
                             String mensagemAluno, String justificativaRecusa,
                             String dataFormatada, String status) {
            this.id = id;
            this.alunoId = alunoId;
            this.nomeAluno.set(nomeAluno);
            this.emailAluno.set(emailAluno);
            this.mensagemAluno = mensagemAluno;
            this.justificativaRecusa = justificativaRecusa;
            this.dataFormatada.set(dataFormatada);
            this.status.set(status);
        }
    }
}