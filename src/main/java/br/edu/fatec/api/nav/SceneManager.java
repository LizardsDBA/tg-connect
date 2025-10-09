package br.edu.fatec.api.nav;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public final class SceneManager {
    private static Stage stage;
    private static double defaultW = 1200, defaultH = 720;
    private static String globalCss; // ex.: "/application.css"

    private SceneManager(){}

    public static void init(Stage s, double w, double h, String cssPath){
        stage = s;
        defaultW = w;
        defaultH = h;
        globalCss = cssPath;
    }

    public static void go(String fxmlPath){
        try {
            String path = normalizePath(fxmlPath);            // ex.: "aluno/Editor.fxml" -> "/views/aluno/Editor.fxml"
            URL url = SceneManager.class.getResource(path);
            if (url == null) throw new IllegalArgumentException("FXML não encontrado: " + path);

            Parent root = FXMLLoader.load(url);

            if (stage.getScene() == null) {
                // Primeira cena: cria com tamanho padrão
                Scene scene = new Scene(root, defaultW, defaultH);
                applyCss(scene);
                stage.setScene(scene);
            } else {
                // Demais navegações: preserva tamanho/estado (inclusive maximizado)
                Scene scene = stage.getScene();
                scene.setRoot(root);
                applyCss(scene);
                // Em alguns ambientes é bom reafirmar:
                if (stage.isMaximized()) stage.setMaximized(true);
            }
        } catch (Exception e){
            throw new RuntimeException("Falha ao abrir FXML: " + fxmlPath, e);
        }
    }

    private static void applyCss(Scene scene){
        if (globalCss == null || globalCss.isBlank()) return;
        URL cssUrl = SceneManager.class.getResource(globalCss);
        if (cssUrl == null) return;
        String css = cssUrl.toExternalForm();
        if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
    }

    private static String normalizePath(String fxmlPath){
        if (fxmlPath.startsWith("/")) return fxmlPath;
        if (fxmlPath.startsWith("views/")) return "/" + fxmlPath;
        return "/views/" + fxmlPath;
    }
}