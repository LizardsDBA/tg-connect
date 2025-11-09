module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires javafx.web;
    requires flexmark.ext.tables;
    requires flexmark;
    requires flexmark.util.data;

    // Abre pacotes para JavaFX FXML (reflexão)
    opens br.edu.fatec.api to javafx.graphics, javafx.fxml;
    opens br.edu.fatec.api.controller to javafx.fxml;
    opens br.edu.fatec.api.controller.aluno to javafx.fxml;
    opens br.edu.fatec.api.controller.orientador to javafx.fxml;
    opens br.edu.fatec.api.controller.coordenacao to javafx.fxml;

    // Exporta pacotes públicos
    exports br.edu.fatec.api.controller;
    exports br.edu.fatec.api.controller.aluno;
    exports br.edu.fatec.api.controller.orientador;
    exports br.edu.fatec.api.controller.coordenacao;
    exports br.edu.fatec.api;
}