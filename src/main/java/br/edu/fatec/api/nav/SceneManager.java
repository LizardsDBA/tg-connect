package br.edu.fatec.api.nav;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public final class SceneManager {
  private static Stage stage;
  private static double width, height;
  private static String css;
  private SceneManager() {}
  public static void init(Stage stg, double w, double h, String cssPath){
    stage = stg; width = w; height = h; css = cssPath;
  }
  public static void go(String fxmlPath){
    try{
      Parent root = FXMLLoader.load(Objects.requireNonNull(SceneManager.class.getResource("/views/" + fxmlPath)));
      Scene scene = new Scene(root, width, height);
      if(css != null) scene.getStylesheets().add(Objects.requireNonNull(SceneManager.class.getResource(css)).toExternalForm());
      stage.setScene(scene);
    }catch(Exception e){
      throw new RuntimeException("Falha ao abrir FXML: " + fxmlPath, e);
    }
  }
}
