package br.edu.fatec.api;

import br.edu.fatec.api.nav.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
  @Override public void start(Stage stage) {
    SceneManager.init(stage, 1200, 720, "/application.css");
    SceneManager.go("login/Login.fxml");
    stage.setTitle("API/TG - Plataforma");
    stage.setMaximized(true);
    stage.show();
  }
  public static void main(String[] args){ launch(args); }
}
