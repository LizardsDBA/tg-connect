package br.edu.fatec.api.controller.orientador;

import br.edu.fatec.api.nav.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImportarOrientadorController {

    // Sidebar – rota ativa
    @FXML private Button btnImportar;

    // Upload
    @FXML private Button btnSelecionar, btnImportarAcao;
    @FXML private Label lblStatus;

    // Prévia
    @FXML private TableView<PreviewRow> tblPreview;
    @FXML private TableColumn<PreviewRow, String> colPrev1, colPrev2, colPrev3;

    // Histórico
    @FXML private TableView<HistoricoRow> tblHistorico;
    @FXML private TableColumn<HistoricoRow, String> colHistData, colHistNome, colHistStatus;

    private File arquivoSelecionado;

    @FXML
    private void initialize() {
        // Rota ativa
        if (btnImportar != null && !btnImportar.getStyleClass().contains("active")) {
            btnImportar.getStyleClass().add("active");
        }

        // Bind das colunas de prévia
        if (colPrev1 != null) colPrev1.setCellValueFactory(c -> c.getValue().c1);
        if (colPrev2 != null) colPrev2.setCellValueFactory(c -> c.getValue().c2);
        if (colPrev3 != null) colPrev3.setCellValueFactory(c -> c.getValue().c3);
        if (tblPreview != null) {
            tblPreview.setPlaceholder(new Label("Selecione um arquivo para visualizar a prévia."));
        }

        // Bind das colunas do histórico
        if (colHistData != null) colHistData.setCellValueFactory(c -> c.getValue().data);
        if (colHistNome != null) colHistNome.setCellValueFactory(c -> c.getValue().nome);
        if (colHistStatus != null) colHistStatus.setCellValueFactory(c -> c.getValue().status);
        if (tblHistorico != null) {
            tblHistorico.setItems(FXCollections.observableArrayList());
            tblHistorico.setPlaceholder(new Label("Sem histórico ainda."));
        }

        setStatus("Nenhum arquivo selecionado");
        if (btnImportarAcao != null) btnImportarAcao.setDisable(true);
    }

    // === Upload ===
    public void selecionarArquivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Arquivo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Markdown", "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter("Texto", "*.txt"),
                new FileChooser.ExtensionFilter("Todos", "*.*")
        );

        Window owner = (lblStatus != null && lblStatus.getScene() != null) ? lblStatus.getScene().getWindow() : null;
        File f = fc.showOpenDialog(owner);
        if (f == null) return;

        this.arquivoSelecionado = f;
        setStatus("Selecionado: " + f.getName());
        if (btnImportarAcao != null) btnImportarAcao.setDisable(false);

        // Carrega prévia
        try {
            ObservableList<PreviewRow> rows = loadPreview(f, 50);
            tblPreview.setItems(rows);
        } catch (IOException e) {
            setStatus("Erro ao ler: " + e.getMessage());
            if (tblPreview != null) tblPreview.setItems(FXCollections.observableArrayList());
        }
    }

    public void importarArquivo() {
        if (arquivoSelecionado == null) {
            setStatus("Selecione um arquivo antes de importar.");
            return;
        }

        // TODO: persistir no SQLite (por ex.: inserir em tabelas correspondentes)
        addHistorico(arquivoSelecionado.getName(), "Importado");
        setStatus("Importação concluída: " + arquivoSelecionado.getName());

        // Opcional: desabilitar botão até nova seleção
        // btnImportarAcao.setDisable(true);
    }

    // Prévia simples: CSV → divide por ; ou , | MD/TXT → linha inteira na primeira coluna
    private ObservableList<PreviewRow> loadPreview(File f, int maxLines) throws IOException {
        ObservableList<PreviewRow> list = FXCollections.observableArrayList();
        try (BufferedReader br = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            String name = f.getName().toLowerCase();
            boolean isCsv = name.endsWith(".csv");
            boolean isMd  = name.endsWith(".md") || name.endsWith(".markdown");
            String line; int count = 0;

            while ((line = br.readLine()) != null && count < maxLines) {
                count++;
                if (isCsv) {
                    String[] parts = line.split("[;|,]", -1);
                    list.add(new PreviewRow(safe(parts,0), safe(parts,1), safe(parts,2)));
                } else if (isMd) {
                    // Simples: joga a linha na Coluna 1, útil para conferência visual
                    list.add(new PreviewRow(line, "", ""));
                } else {
                    // TXT genérico
                    list.add(new PreviewRow(line, "", ""));
                }
            }
        }
        return list;
    }

    private String safe(String[] arr, int i) { return (i < arr.length) ? arr[i] : ""; }

    private void addHistorico(String nomeArquivo, String status) {
        if (tblHistorico == null) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String data = LocalDateTime.now().format(fmt);
        tblHistorico.getItems().add(new HistoricoRow(data, nomeArquivo, status));
    }

    private void setStatus(String s) { if (lblStatus != null) lblStatus.setText(s); }

    // ===== Navegação =====
    public void goHome(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void logout(){ SceneManager.go("login/Login.fxml"); }

    public void goVisaoGeral(){ SceneManager.go("orientador/VisaoGeral.fxml"); }
    public void goPainel(){ SceneManager.go("orientador/Painel.fxml"); }
    public void goNotificacoes(){ SceneManager.go("orientador/Notificacoes.fxml"); }
    public void goEditor(){ SceneManager.go("orientador/Editor.fxml"); }
    public void goParecer(){ SceneManager.go("orientador/Parecer.fxml"); }
    public void goImportar(){ SceneManager.go("orientador/Importar.fxml"); }
    public void goChat(){ SceneManager.go("orientador/Chat.fxml"); }

    // ===== View Models =====
    public static class PreviewRow {
        final SimpleStringProperty c1 = new SimpleStringProperty();
        final SimpleStringProperty c2 = new SimpleStringProperty();
        final SimpleStringProperty c3 = new SimpleStringProperty();
        public PreviewRow(String c1, String c2, String c3){ this.c1.set(c1); this.c2.set(c2); this.c3.set(c3); }
        public String getC1(){ return c1.get(); } public String getC2(){ return c2.get(); } public String getC3(){ return c3.get(); }
    }

    public static class HistoricoRow {
        final SimpleStringProperty data = new SimpleStringProperty();
        final SimpleStringProperty nome = new SimpleStringProperty();
        final SimpleStringProperty status = new SimpleStringProperty();
        public HistoricoRow(String data, String nome, String status){ this.data.set(data); this.nome.set(nome); this.status.set(status); }
        public String getData(){ return data.get(); } public String getNome(){ return nome.get(); } public String getStatus(){ return status.get(); }
    }
}
