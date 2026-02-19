package com.pos.system.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class NotificationUtils {

    // If ControlsFX is available (I need to check pom.xml if I added it?
    // I added AtlantaFX, ReactFX, Ikonli. I did NOT add ControlsFX in dependencies!
    // The prompt suggested "ControlsFX (for advanced buttons/dialogs)".
    // I missed adding ControlsFX to pom.xml in the Dependency Management step!

    // I should add ControlsFX to pom.xml.

    // For now, I'll use standard JavaFX Alert or Custom Toast if ControlsFX is
    // missing.
    // Let's implement a simple JavaFX Toast to be safe without ControlsFX first,
    // or just standard Alerts.

    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    public static void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    // Simple Toast
    public static void showToast(Stage owner, String message) {
        Platform.runLater(() -> {
            Popup popup = new Popup();
            Label label = new Label(message);
            label.setStyle(
                    "-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5px;");
            popup.getContent().add(label);
            popup.setAutoHide(true);
            if (owner != null) {
                popup.show(owner);
                // Auto-hide using PauseTransition
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(3));
                delay.setOnFinished(e -> popup.hide());
                delay.play();
            }
        });
    }
}
