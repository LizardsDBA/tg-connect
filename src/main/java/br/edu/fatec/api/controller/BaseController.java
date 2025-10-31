package br.edu.fatec.api.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public abstract class BaseController {

    @FXML protected VBox sidebar;         // precisa existir no FXML
    @FXML protected HBox mainLayout;      // precisa existir no FXML
    @FXML protected Button btnToggleSidebar; // botão ☰ no header

    private boolean sidebarVisible = true;
    private double expandedWidth = -1;    // largura “aberta” real da sidebar
    private boolean animating = false;

    @FXML
    public void toggleSidebar() {
        if (sidebar == null || mainLayout == null) return;
        if (animating) return;

        // descobre largura alvo (uma única vez)
        if (expandedWidth <= 0) {
            double w = sidebar.getWidth();
            if (w <= 0) w = sidebar.getPrefWidth();
            if (w <= 0) w = 240; // fallback
            expandedWidth = w;
        }

        animating = true;
        if (btnToggleSidebar != null) btnToggleSidebar.setDisable(true);

        // parâmetros da animação
        Duration d = Duration.millis(220);
        Interpolator ease = Interpolator.EASE_BOTH;

        if (sidebarVisible) {
            // FECHAR: largura expandedWidth -> 0
            Timeline close = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sidebar.prefWidthProperty(), sidebar.getWidth() > 0 ? sidebar.getWidth() : expandedWidth, ease),
                            new KeyValue(sidebar.opacityProperty(), 1.0, ease)
                    ),
                    new KeyFrame(d,
                            new KeyValue(sidebar.prefWidthProperty(), 0.0, ease),
                            new KeyValue(sidebar.opacityProperty(), 0.0, ease)
                    )
            );
            close.setOnFinished(e -> {
                sidebar.setVisible(false);
                sidebar.setManaged(false);   // não ocupa espaço após fechar
                sidebarVisible = false;
                if (btnToggleSidebar != null) btnToggleSidebar.setText("☷");
                animating = false;
                if (btnToggleSidebar != null) btnToggleSidebar.setDisable(false);
            });
            close.play();

        } else {
            // ABRIR: largura 0 -> expandedWidth
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            sidebar.setOpacity(0.0);
            sidebar.setPrefWidth(0.0);

            Timeline open = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sidebar.prefWidthProperty(), 0.0, ease),
                            new KeyValue(sidebar.opacityProperty(), 0.0, ease)
                    ),
                    new KeyFrame(d,
                            new KeyValue(sidebar.prefWidthProperty(), expandedWidth, ease),
                            new KeyValue(sidebar.opacityProperty(), 1.0, ease)
                    )
            );
            open.setOnFinished(e -> {
                // garante a largura final “aberta”
                sidebar.setPrefWidth(expandedWidth);
                sidebarVisible = true;
                if (btnToggleSidebar != null) btnToggleSidebar.setText("☰");
                animating = false;
                if (btnToggleSidebar != null) btnToggleSidebar.setDisable(false);
            });
            open.play();
        }
    }
}
