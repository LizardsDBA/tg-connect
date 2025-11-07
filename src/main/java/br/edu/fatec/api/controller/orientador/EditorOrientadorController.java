package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.controller.BaseController;
import br.edu.fatec.api.dao.JdbcFeedbackDao;
import br.edu.fatec.api.dao.JdbcFeedbackDao.ConteudoParteDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.OrientandoDTO;
import br.edu.fatec.api.dao.JdbcFeedbackDao.Parte;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;
import br.edu.fatec.api.nav.SceneManager;
import br.edu.fatec.api.nav.Session;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class EditorOrientadorController extends BaseController {

    // ===== Header/Sidebar comuns =====
    @FXML private Button btnToggleSidebar;
    @FXML private Button btnSouCoordenador;

    // ===== Lista de alunos =====
    @FXML private TextField txtBuscaAluno;
    @FXML private TableView<OrientandoTableItem> tblAlunos;
    @FXML private TableColumn<OrientandoTableItem, String> colNome;

    // ===== Abas/labels principais =====
    @FXML private TabPane tabPartes;
    @FXML private Tab tabApresentacao, tabApi1, tabApi2, tabApi3, tabApi4, tabApi5, tabApi6, tabResumo, tabFinais;

    @FXML private Label lblAluno, lblVersao, lblStatus;

    // ===== Campos Apresentação (exemplos; adicione os demais conforme for usar) =====
    @FXML private TextArea txtNomeCompleto;
    @FXML private WebView  webNomeCompleto;
    @FXML private Label    lblNomeCompletoStatus;

    @FXML private TextArea txtCurso;
    @FXML private WebView  webCurso;
    @FXML private Label    lblCursoStatus;

    @FXML private TextArea txtHistoricoAcademico;
    @FXML private WebView  webHistoricoAcademico;
    @FXML private Label    lblHistoricoAcademicoStatus;

    // ===== Resumo =====
    @FXML private TextArea txtResumoMd;
    @FXML private WebView  webResumo;
    @FXML private Label    lblResumoVersaoStatus;

    // ===== Comentário (Chat) =====
    @FXML private TextArea txtComentario;
    @FXML private Button btnEnviarComentario;

    // ===== Estado =====
    private final JdbcFeedbackDao dao = new JdbcFeedbackDao();
    private final ObservableList<OrientandoTableItem> alunos = FXCollections.observableArrayList();

    private Long professorId;
    private Long alunoSelecionadoId;
    private Long trabalhoIdSelecionado;

    // Última versão carregada por parte (rótulo)
    private String versaoAtual;

    // Markdown engine
    private Parser mdParser;
    private HtmlRenderer mdRenderer;

    @FXML
    public void initialize() {
        initMarkdown();
        initUserAndLoad();
        initTabelaAlunos();
        initBuscaFiltro();
        initAbas();
        initBotoes();
    }

    private void initMarkdown() {
        MutableDataSet opts = new MutableDataSet();
        opts.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        mdParser = Parser.builder(opts).build();
        mdRenderer = HtmlRenderer.builder(opts).build();
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

    private void initAbas() {
        tabPartes.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                carregarParte(parteFromTab(newT));
            }
        });
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
    }

    private void enviarComentario() {
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
        this.alunoSelecionadoId = it.alunoId.get();
        lblAluno.setText(it.nomeProperty.get());
        try {
            this.trabalhoIdSelecionado = dao.obterTrabalhoIdPorAluno(alunoSelecionadoId);
        } catch (SQLException e) {
            erro("Não foi possível localizar o Trabalho de Graduação.", e);
            return;
        }
        // Seleciona aba padrão
        tabPartes.getSelectionModel().select(tabApresentacao);
        carregarParte(Parte.APRESENTACAO);
    }

    private void carregarParte(Parte parte) {
        if (trabalhoIdSelecionado == null || parte == null) return;

        try {
            ConteudoParteDTO dto = dao.carregarUltimaVersao(trabalhoIdSelecionado, parte);

            // Versão e markdown (garantidos)
            this.versaoAtual = dto.versao();
            lblVersao.setText(versaoAtual != null ? versaoAtual : "—");

            String md = (dto.markdown() == null) ? "" : dto.markdown();

            // Atualiza badge/label de status da versão atual
            boolean validada = (versaoAtual != null) && dao.verificarConclusao(trabalhoIdSelecionado, parte, versaoAtual);
            atualizarStatusVisual(validada);

            switch (parte) {
                case APRESENTACAO -> {
                    // Mostramos o consolidado no card "Histórico acadêmico" (tem WebView disponível)
                    setTextArea(txtHistoricoAcademico, md);
                    renderMarkdown(webHistoricoAcademico, md);

                    // Limpa os demais cards até termos os campos individuais mapeados
                    setTextArea(txtNomeCompleto, "");
                    renderMarkdown(webNomeCompleto, "");
                    setTextArea(txtCurso, "");
                    renderMarkdown(webCurso, "");
                }

                case RESUMO -> {
                    setTextArea(txtResumoMd, md);
                    renderMarkdown(webResumo, md);
                }

                default -> {
                    // APIs 1..6 e FINAIS: layout por campo ainda não está no FXML desta versão
                    // (quando você inserir os cards por campo nas abas de API/Finais, basta renderizar como acima)
                    // Aqui só atualizamos o status visual.
                }
            }

        } catch (SQLException e) {
            erro("Falha ao carregar parte.", e);
        }
    }

    private void atualizarStatusVisual(boolean validada) {
        if (lblStatus == null) return;
        lblStatus.setText(validada ? "Concluída" : "Pendente Validação");
        lblStatus.getStyleClass().removeAll("badge-ok","badge-pendente");
        lblStatus.getStyleClass().add(validada ? "badge-ok" : "badge-pendente");
    }



    private void setTextArea(TextArea ta, String txt) {
        if (ta != null) ta.setText(txt != null ? txt : "");
    }

    private void renderMarkdown(WebView webView, String markdown) {
        if (webView == null) return;

        String md = (markdown == null) ? "" : markdown;
        String htmlBody = mdRenderer.render(mdParser.parse(md));

        String page =
                "<!doctype html><html><head>" +
                        "<meta charset='UTF-8'>" +
                        "<style>" +
                        "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; padding: 10px; }" +
                        "  pre, code { white-space: pre-wrap; }" +
                        "  table { border-collapse: collapse; width: 100%; }" +
                        "  th, td { border: 1px solid #ddd; padding: 6px; }" +
                        "  h1,h2,h3 { margin-top: 0.8em; }" +
                        "</style>" +
                        "</head><body>" +
                        htmlBody +
                        "</body></html>";

        webView.getEngine().loadContent(page);
    }

    // ===== Ações Aprovar/Reprovar (stubs visuais por enquanto) =====
    @FXML private void onAprovarCampo() { info("Aprovação por campo: em breve (depende do DAO com *_status)."); }
    @FXML private void onReprovarCampo() { info("Reprovação por campo: em breve (depende do DAO com *_status)."); }

    @FXML private void onAprovarResumoVersao() { info("Aprovar versão do Resumo: em breve (tri-state)."); }
    @FXML private void onReprovarResumoVersao() { info("Reprovar versão do Resumo: em breve (tri-state)."); }

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
        public static OrientandoTableItem from(OrientandoDTO d) {
            OrientandoTableItem it = new OrientandoTableItem();
            it.alunoId.set(Objects.requireNonNullElse(d.alunoId(), 0L));
            it.nomeProperty.set(Objects.requireNonNullElse(d.nome(), "—"));
            return it;
        }
    }

    // ===== Navegação =====
    public void goHomeCoord(){ SceneManager.go("coordenacao/VisaoGeral.fxml"); }
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }
    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goChat(){ SceneManager.go("orientador/Chat.fxml"); }
}
