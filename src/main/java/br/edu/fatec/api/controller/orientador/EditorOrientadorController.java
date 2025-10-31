package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ConteudoParteDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.OrientandoDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.Parte;
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
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.controller.BaseController;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Tela Editor do Orientador (Feedback/Parecer)
 * Padrão: Header + Sidebar + Footer (layout no FXML)
 * - Lista orientandos do professor (esquerda)
 * - Abas no topo para partes (Apresentação, API1..6, Resumo, Finais)
 * - Preview Markdown (somente leitura)
 * - Envio de comentário -> Mensagens (Chat externo)
 * - Marcar parte como Concluída (status por versão; não reabre)
 */
public class EditorOrientadorController extends BaseController {

    // ====== UI: Sidebar/Main ======
    @FXML private TextField txtBuscaAluno;
    @FXML private TableView<OrientandoTableItem> tblAlunos;
    @FXML private TableColumn<OrientandoTableItem, String> colNome; // somente nome

    @FXML private TabPane tabPartes;
    @FXML private Tab tabApresentacao, tabApi1, tabApi2, tabApi3, tabApi4, tabApi5, tabApi6, tabResumo, tabFinais;

    @FXML private Label lblAluno, lblVersao, lblStatus;
    @FXML private TextArea txtPreview;
    @FXML private TextArea txtComentario;
    @FXML private Button btnEnviarComentario, btnConcluirParte;
    @FXML private Button btnSouCoordenador;

    // ====== Estado da tela ======
    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private final ObservableList<OrientandoTableItem> alunos = FXCollections.observableArrayList();

    private Long professorId;               // injetado após login
    private Long alunoSelecionadoId;        // aluno atual
    private Long trabalhoIdSelecionado;     // TG do aluno atual

    // Versão corrente por parte (exibe em label e reusa no concluir/comentar)
    private String versaoApresentacao;
    private String versaoApi1, versaoApi2, versaoApi3, versaoApi4, versaoApi5, versaoApi6;
    private String versaoResumo;
    private String versaoFinais;

    // ====== Ciclo de vida ======
    @FXML
    public void initialize() {

        User user = Session.getUser();

        if (user != null) {
            Long professorId = user.getId(); // ✅ aqui você pega o ID do orientador logado
            this.professorId = professorId;
            carregarOrientandos(); // pode chamar direto se quiser
        } else {
            System.err.println("Nenhum usuário logado encontrado na sessão.");
        }

        if (btnToggleSidebar != null) {
            btnToggleSidebar.setText("☰");
        }

        // Tabela de alunos (apenas Nome)
        colNome.setCellValueFactory(c -> c.getValue().nomeProperty);
        tblAlunos.setItems(alunos);

        // Duplo clique / Enter para selecionar aluno
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

        // Filtro local (por nome)
        txtBuscaAluno.textProperty().addListener((obs, o, v) -> filtrarAlunos(v));

        // Troca de aba -> recarrega parte
        tabPartes.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {
                    if (newTab != null) {
                        Parte parte = parteFromTab(newTab);
                        carregarParte(parte);
                    }
                });

        // Botões (regras de disponibilidade)
        btnEnviarComentario.setOnAction(e -> enviarComentario());
        btnConcluirParte.setOnAction(e -> marcarConcluida());

        btnEnviarComentario.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> alunoSelecionadoId == null
                                || trabalhoIdSelecionado == null
                                || txtComentario.getText().isBlank(),
                        txtComentario.textProperty()
                )
        );
        btnConcluirParte.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> alunoSelecionadoId == null
                                || trabalhoIdSelecionado == null
                                || parteSelecionada() == null
                                || isBlank(lblVersao.getText())  // habilita quando existir versão carregada
                        ,
                        lblVersao.textProperty(),
                        tblAlunos.getSelectionModel().selectedItemProperty(),
                        tabPartes.getSelectionModel().selectedItemProperty()
                )
        );

        User u = Session.getUser();
        boolean isCoord = (u != null && u.getRole() == Role.COORDENADOR);
        if (btnSouCoordenador != null) {
            btnSouCoordenador.setVisible(isCoord);
            btnSouCoordenador.setManaged(isCoord); // evita “buraco” no layout quando oculto
        }


    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty() || "—".equals(s.trim());
    }

    // ====== Fluxos principais ======
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
        this.alunoSelecionadoId = it.alunoId.get();
        lblAluno.setText(it.nomeProperty.get());

        try {
            this.trabalhoIdSelecionado = dao.obterTrabalhoIdPorAluno(alunoSelecionadoId);
        } catch (SQLException e) {
            erro("Não foi possível localizar o Trabalho de Graduação para o aluno selecionado.", e);
            return;
        }

        // reset das versões conhecidas
        versaoApresentacao = versaoResumo = versaoFinais = null;
        versaoApi1 = versaoApi2 = versaoApi3 = versaoApi4 = versaoApi5 = versaoApi6 = null;

        // seleciona aba padrão e carrega
        tabPartes.getSelectionModel().select(tabApresentacao);
        carregarParte(Parte.APRESENTACAO);
    }

    private void carregarParte(Parte parte) {
        if (trabalhoIdSelecionado == null) return;

        try {
            ConteudoParteDTO dto = dao.carregarUltimaVersao(trabalhoIdSelecionado, parte);

            // Labels
            lblVersao.setText(dto.versao() != null ? dto.versao() : "—");

            // Preview
            txtPreview.setText(dto.markdown() != null ? dto.markdown() : "");

            // Guarda a versão corrente por parte
            if (dto.versao() != null) setVersaoAtual(parte, dto.versao());

            atualizarStatusAtual();

        } catch (SQLException e) {
            erro("Falha ao carregar a parte " + parte + ".", e);
        }
    }

    private void enviarComentario() {
        Parte parte = parteSelecionada();
        if (parte == null || trabalhoIdSelecionado == null || alunoSelecionadoId == null) return;

        String versao = versaoAtual(parte);
        String texto = "FEEDBACK\nReferente a: " + parte + "\n" + txtComentario.getText().trim();
        if (texto.isBlank()) return;

        Long destinatarioId = alunoSelecionadoId;
        Long remetenteId = professorId;

        try {
            dao.enviarComentario(trabalhoIdSelecionado, remetenteId, destinatarioId, parte, versao, texto);
            txtComentario.clear();
            info("Comentário enviado! O aluno verá na tela de Chat.");
        } catch (SQLException e) {
            erro("Falha ao enviar comentário.", e);
        }
    }

    private void marcarConcluida() {
        Parte parte = parteSelecionada();
        if (parte == null) return;

        String versao = versaoAtual(parte);
        if (versao == null || versao.isBlank()) {
            erro("Não há versão carregada para esta parte.", null);
            return;
        }

        try {
            boolean ok = dao.marcarComoConcluida(trabalhoIdSelecionado, parte, versao);
            if (ok) {
                lblStatus.setText("Concluída");
                atualizarStatusAtual();
                info("Parte marcada como concluída.");
            } else {
                erro("Nenhum registro atualizado. Verifique parte/versão.", null);
            }
        } catch (SQLException e) {
            erro("Erro ao marcar como concluída.", e);
        }
    }

    // ====== Util ======
    private Parte parteSelecionada() {
        Tab t = tabPartes.getSelectionModel().getSelectedItem();
        if (t == null) return null;
        return parteFromTab(t);
    }

    private Parte parteFromTab(Tab t) {
        if (t == tabApresentacao) return Parte.APRESENTACAO;
        if (t == tabApi1) return Parte.API1;
        if (t == tabApi2) return Parte.API2;
        if (t == tabApi3) return Parte.API3;
        if (t == tabApi4) return Parte.API4;
        if (t == tabApi5) return Parte.API5;
        if (t == tabApi6) return Parte.API6;
        if (t == tabResumo) return Parte.RESUMO;
        if (t == tabFinais) return Parte.FINAIS;
        return null;
    }

    private String labelParte(Parte p) {
        return switch (p) {
            case APRESENTACAO -> "Apresentação";
            case RESUMO -> "Resumo";
            case FINAIS -> "Considerações finais";
            default -> p.name().replace("API", "API ");
        };
    }

    private void filtrarAlunos(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            tblAlunos.setItems(alunos);
            return;
        }
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
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ====== Versões correntes ======
    private void setVersaoAtual(Parte p, String versao) {
        switch (p) {
            case APRESENTACAO -> versaoApresentacao = versao;
            case API1 -> versaoApi1 = versao;
            case API2 -> versaoApi2 = versao;
            case API3 -> versaoApi3 = versao;
            case API4 -> versaoApi4 = versao;
            case API5 -> versaoApi5 = versao;
            case API6 -> versaoApi6 = versao;
            case RESUMO -> versaoResumo = versao;
            case FINAIS -> versaoFinais = versao;
        }
    }

    private String versaoAtual(Parte p) {
        return switch (p) {
            case APRESENTACAO -> versaoApresentacao;
            case API1 -> versaoApi1;
            case API2 -> versaoApi2;
            case API3 -> versaoApi3;
            case API4 -> versaoApi4;
            case API5 -> versaoApi5;
            case API6 -> versaoApi6;
            case RESUMO -> versaoResumo;
            case FINAIS -> versaoFinais;
        };
    }

    // ====== Item da Tabela de Alunos (apenas Nome) ======
    public static class OrientandoTableItem {
        private final SimpleLongProperty alunoId = new SimpleLongProperty();
        private final SimpleStringProperty nomeProperty = new SimpleStringProperty();
        private Long trabalhoId;

        public static OrientandoTableItem from(OrientandoDTO d) {
            OrientandoTableItem it = new OrientandoTableItem();
            it.alunoId.set(Objects.requireNonNullElse(d.alunoId(), 0L));
            it.nomeProperty.set(Objects.requireNonNullElse(d.nome(), "—"));
            it.trabalhoId = d.trabalhoId();
            return it;
        }
    }

    private void atualizarStatusAtual() {
        if (lblStatus == null) return;
        Parte parte = parteSelecionada();

        String versao = versaoAtual(parte);

        try {
            boolean validada = dao.verificarConclusao(trabalhoIdSelecionado, parte, versao);
            String statusTxt = validada ? "Concluída" : "Pendente Validação";
            lblStatus.setText(statusTxt);
            lblStatus.getStyleClass().removeAll("badge-ok","badge-pendente");
            lblStatus.getStyleClass().add(validada ? "badge-ok" : "badge-pendente");
        } catch (SQLException e) {
            // Em caso de erro de banco, manter pendente
            lblStatus.setText("Pendente Validação");
            lblStatus.getStyleClass().removeAll("badge-ok","badge-pendente");
            lblStatus.getStyleClass().add("badge-pendente");
        }
    }


    // ===== Navegação =====
    public void goHomeCoord(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
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
