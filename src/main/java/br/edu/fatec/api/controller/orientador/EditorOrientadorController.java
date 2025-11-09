package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ApresentacaoCamposDTO; // Necessário para o método carregarStatusAluno
import br.edu.fatec.api.dao.JdbcFeedbackDao.OrientandoDTO;
import br.edu.fatec.api.controller.orientador.ModalPreviewController;
import br.edu.fatec.api.dao.JdbcTrabalhosGraduacaoDao;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller da tela principal de Feedback.
 * Responsável por:
 * 1. Listar os alunos orientandos.
 * 2. Abrir o modal de feedback por campo.
 * 3. Enviar comentários gerais (via Chat).
 */
public class EditorOrientadorController extends BaseController {

    // ===== Header/Sidebar comuns =====
    @FXML private Button btnToggleSidebar;
    @FXML private Button btnSouCoordenador;

    // ===== Lista de alunos =====
    @FXML private TextField txtBuscaAluno;
    @FXML private TableView<OrientandoTableItem> tblAlunos;
    @FXML private TableColumn<OrientandoTableItem, String> colNome;

    // ===== Status e Botão do Modal =====
    @FXML private Label lblAluno, lblVersao, lblStatus;
    @FXML private Button btnAbrirModalFeedback;
    @FXML private Button btnAbrirModalPreview;

    // ===== Comentário (Chat) =====
    @FXML private TextArea txtComentario;
    @FXML private Button btnEnviarComentario;

    // ===== Estado =====
    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private final ObservableList<OrientandoTableItem> alunos = FXCollections.observableArrayList();
    @FXML private TableColumn<OrientandoTableItem, String> colStatus;

    private Long professorId;
    private Long alunoSelecionadoId;
    private Long trabalhoIdSelecionado;
    private String versaoAtual;

    @FXML
    public void initialize() {
        initUserAndLoad();
        initTabelaAlunos();
        initBuscaFiltro();
        initBotoes();
    }

    private void initUserAndLoad() {
        User user = Session.getUser();
        if (user != null) {
            this.professorId = user.getId();
            carregarOrientandos();
            boolean isCoord = (user.getRole() == Role.COORDENADOR);
            if (btnSouCoordenador != null) {
                btnSouCoordenador.setVisible(isCoord);
                btnSouCoordenador.setManaged(isCoord);
            }
        }
        if (btnToggleSidebar != null) btnToggleSidebar.setText("☰");
    }

    private void initTabelaAlunos() {
        colNome.setCellValueFactory(c -> c.getValue().nomeProperty);
        tblAlunos.setItems(alunos);

        if (colStatus != null) {
            colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setGraphic(null);
                        getStyleClass().removeAll("badge-ok", "badge-pendente", "badge-reprovado");
                    } else {
                        String texto;
                        String styleClass;

                        switch (status) {
                            case "ENTREGUE" -> {
                                texto = "Aguardando Revisão";
                                styleClass = "badge-pendente"; // (Estilo Laranja/Amarelo)
                            }
                            case "APROVADO" -> {
                                texto = "Concluído";
                                styleClass = "badge-ok"; // (Verde)
                            }
                            case "REPROVADO" -> {
                                texto = "Revisado (Pendências)";
                                styleClass = "badge-reprovado"; // (Vermelho)
                            }
                            default -> { // EM_ANDAMENTO
                                texto = "Em Andamento";
                                styleClass = ""; // (Sem cor)
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
        }

        tblAlunos.setRowFactory(tv -> {
            TableRow<OrientandoTableItem> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty()) selecionarAluno(row.getItem());
            });
            return row;
        });
        tblAlunos.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                OrientandoTableItem it = tblAlunos.getSelectionModel().getSelectedItem();
                if (it != null) selecionarAluno(it);
            }
        });
    }

    private void initBuscaFiltro() {
        txtBuscaAluno.textProperty().addListener((obs, o, v) -> filtrarAlunos(v));
    }

    private void initBotoes() {
        btnEnviarComentario.setOnAction(e -> enviarComentario());
        btnEnviarComentario.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> alunoSelecionadoId == null
                                || trabalhoIdSelecionado == null
                                || txtComentario.getText().isBlank(),
                        txtComentario.textProperty()
                )
        );

        if (btnAbrirModalFeedback != null) {
            btnAbrirModalFeedback.disableProperty().bind(
                    tblAlunos.getSelectionModel().selectedItemProperty().isNull()
                            .or(lblStatus.textProperty().isNotEqualTo("ENTREGUE")) // <-- LÓGICA DA TRAVA
            );
        }

        // (O botão de preview pode ficar habilitado)
        if (btnAbrirModalPreview != null) {
            btnAbrirModalPreview.disableProperty().bind(
                    tblAlunos.getSelectionModel().selectedItemProperty().isNull()
            );
        }
    }

    private void enviarComentario() {
        // ... (lógica para enviar comentário)
        info("Comentário (stub): " + txtComentario.getText());
    }

    private void carregarOrientandos() {
        alunos.clear();
        if (professorId == null) return;
        try {
            var lista = dao.listarOrientandos(professorId).stream()
                    .map(OrientandoTableItem::from)
                    .collect(Collectors.toList());
            alunos.addAll(lista);
        } catch (SQLException e) {
            erro("Falha ao listar orientandos.", e);
        }
    }

    private void selecionarAluno(OrientandoTableItem it) {
        if (it == null) return;

        this.alunoSelecionadoId = it.alunoId.get();
        lblAluno.setText(it.nomeProperty.get());
        try {
            this.trabalhoIdSelecionado = dao.obterTrabalhoIdPorAluno(alunoSelecionadoId);
            // Atualiza o status geral do aluno selecionado
            carregarStatusAluno(trabalhoIdSelecionado);

        } catch (SQLException e) {
            erro("Não foi possível localizar o Trabalho de Graduação.", e);
            // Limpa os labels se o aluno não tiver TG
            lblVersao.setText("—");
            lblStatus.setText("—");
            this.trabalhoIdSelecionado = null;
        }
    }

    /**
     * Carrega o status geral (versão e se está concluído) do aluno.
     */
    private void carregarStatusAluno(Long trabalhoId) {
        if (trabalhoId == null) return;
        try {
            // Busca a versão atual
            ApresentacaoCamposDTO dto = dao.carregarCamposApresentacao(trabalhoId);
            this.versaoAtual = dto.versao();
            lblVersao.setText(dto.versao());

            // Busca o status do fluxo (ex: "ENTREGUE")
            JdbcTrabalhosGraduacaoDao tgDao = new JdbcTrabalhosGraduacaoDao();
            String statusFluxo = tgDao.findStatusById(trabalhoId).orElse("EM_ANDAMENTO");

            // Passa os dois dados para o método de UI
            atualizarStatusVisual(statusFluxo, dto.concluida());

        } catch (SQLException e) {
            erro("Falha ao carregar status do aluno.", e);
        }
    }

    /**
     * Atualiza o status principal da versão (Concluída / Pendente)
     */
    private void atualizarStatusVisual(String statusFluxo, boolean concluida) {
        if (lblStatus == null) return;

        // Salva o status real (EM_ANDAMENTO, ENTREGUE, etc.)
        lblStatus.setText(statusFluxo);

        // (Podemos usar o 'concluida' para o label de Versão, se quisermos)
        // lblVersao.setText(versaoAtual + (concluida ? " (Concluída)" : ""));
    }
    /**
     * Abre o modal de feedback por campo.
     */
    @FXML
    private void abrirModalFeedback() {
        if (trabalhoIdSelecionado == null) {
            erro("Nenhum aluno selecionado.", null);
            return;
        }

        try {
            // Cria uma nova janela (Stage) para o modal
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL); // Trava a janela principal
            modalStage.setTitle("Feedback por Campo - " + lblAluno.getText());

            // Carrega o FXML do modal
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/views/orientador/ModalFeedback.fxml"));
            Parent root = loader.load();

            // Passa os dados para o controller do modal ANTES de exibi-lo
            ModalFeedbackController controller = loader.getController();
            controller.initData(trabalhoIdSelecionado, versaoAtual); // Passamos o ID e a versão

            Scene scene = new Scene(root);
            modalStage.setScene(scene);
            modalStage.showAndWait(); // Exibe e espera o modal ser fechado

            // Recarrega o status geral na tela principal
            carregarStatusAluno(trabalhoIdSelecionado);

        } catch (Exception e) {
            erro("Falha ao abrir o modal de feedback.", e);
        }
    }

    @FXML
    private void abrirModalPreview() {
        if (trabalhoIdSelecionado == null) {
            erro("Nenhum aluno selecionado.", null);
            return;
        }

        if (versaoAtual == null || versaoAtual.equals("—")) {
            erro("Nenhuma versão carregada para este aluno.", null);
            return;
        }

        try {
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Preview Completo - " + lblAluno.getText());

            // Carrega o NOVO FXML de preview
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/views/orientador/ModalPreview.fxml"));
            Parent root = loader.load();

            // Passa os dados para o NOVO controller de preview
            ModalPreviewController controller = loader.getController();
            controller.initData(trabalhoIdSelecionado, versaoAtual); // Passa o ID e a versão

            Scene scene = new Scene(root);
            modalStage.setScene(scene);
            modalStage.showAndWait();

        } catch (Exception e) {
            erro("Falha ao abrir o modal de preview.", e);
        }
    }

    private void filtrarAlunos(String filtro) {
        if (filtro == null || filtro.isBlank()) { tblAlunos.setItems(alunos); return; }
        final String f = filtro.toLowerCase(Locale.ROOT).trim();
        var filtrados = alunos.stream()
                .filter(a -> a.nomeProperty.get().toLowerCase(Locale.ROOT).contains(f))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        tblAlunos.setItems(filtrados);
    }

    private void erro(String msg, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Ops!");
        a.setContentText(msg + (e != null ? "\n\n" + e.getMessage() : ""));
        a.showAndWait();
        if (e != null) e.printStackTrace(); // Ajuda a debugar
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ===== Item da Tabela de Alunos =====
    public static class OrientandoTableItem {
        private final SimpleLongProperty alunoId = new SimpleLongProperty();
        private final SimpleStringProperty nomeProperty = new SimpleStringProperty();
        private final SimpleStringProperty statusProperty = new SimpleStringProperty(); // NOVO

        public static OrientandoTableItem from(OrientandoDTO d) {
            OrientandoTableItem it = new OrientandoTableItem();
            it.alunoId.set(Objects.requireNonNullElse(d.alunoId(), 0L));
            it.nomeProperty.set(Objects.requireNonNullElse(d.nome(), "—"));
            it.statusProperty.set(Objects.requireNonNullElse(d.status(), "—")); // NOVO
            return it;
        }

        // Getter para o status
        public SimpleStringProperty statusProperty() {
            return statusProperty;
        }
    }

    // ===== Navegação =====
    public void goHomeCoord(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goChat() {
        SceneManager.go("orientador/Chat.fxml", c -> {
            ChatOrientadorController ctrl = (ChatOrientadorController) c;
            ctrl.onReady();
        });
    }
}