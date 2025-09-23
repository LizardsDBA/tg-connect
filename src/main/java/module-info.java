module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens br.edu.fatec.api to javafx.graphics, javafx.fxml;
    opens br.edu.fatec.api.controller to javafx.fxml;
    opens br.edu.fatec.api.controller.aluno to javafx.fxml;
    opens br.edu.fatec.api.controller.orientador to javafx.fxml;
    opens br.edu.fatec.api.controller.coordenacao to javafx.fxml;


    exports br.edu.fatec.api;
}
