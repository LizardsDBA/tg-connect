package br.edu.fatec.api.nav;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.function.Consumer;

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

    /** Navegação padrão (sem inicialização explícita do controller). */
    public static void go(String fxmlPath){
        try {
            String path = normalizePath(fxmlPath); // "aluno/Editor.fxml" -> "/views/aluno/Editor.fxml"
            URL url = SceneManager.class.getResource(path);
            if (url == null) throw new IllegalArgumentException("FXML não encontrado: " + path);

            Parent root = FXMLLoader.load(url);

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, defaultW, defaultH);
                applyCss(scene);
                stage.setScene(scene);
            } else {
                Scene scene = stage.getScene();
                scene.setRoot(root);
                applyCss(scene);
                if (stage.isMaximized()) stage.setMaximized(true);
            }
        } catch (Exception e){
            throw new RuntimeException("Falha ao abrir FXML: " + fxmlPath, e);
        }
    }

    /**
     * Nova sobrecarga: permite acessar o controller para injetar contexto
     * (ex.: setAlunoContext) e chamar métodos como onReady() ANTES de exibir a cena.
     */
    public static void go(String fxmlPath, Consumer<Object> initController){
        try {
            String path = normalizePath(fxmlPath);
            URL url = SceneManager.class.getResource(path);
            if (url == null) throw new IllegalArgumentException("FXML não encontrado: " + path);

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Object controller = loader.getController();
            if (initController != null) {
                initController.accept(controller);
            }

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, defaultW, defaultH);
                applyCss(scene);
                stage.setScene(scene);
            } else {
                Scene scene = stage.getScene();
                scene.setRoot(root);
                applyCss(scene);
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
