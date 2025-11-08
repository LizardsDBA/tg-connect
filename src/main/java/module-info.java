module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;   // <— necessário para WebView
    requires java.sql;
    requires flexmark.ext.tables;
    requires flexmark;
    requires flexmark.util.data;

    // pacotes que o FXML precisa refletir:
    opens br.edu.fatec.api to javafx.graphics, javafx.fxml;
    opens br.edu.fatec.api.controller to javafx.fxml;
    opens br.edu.fatec.api.controller.aluno to javafx.fxml;
    opens br.edu.fatec.api.controller.orientador to javafx.fxml;
    opens br.edu.fatec.api.controller.coordenacao to javafx.fxml;

    // exports só do que sua app realmente expõe como API pública
    exports br.edu.fatec.api;
}
