package com.pos.system.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import com.pos.system.App;

import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationUtils {

    public enum DialogType {
        SUCCESS, ERROR, WARNING, INFO, CONFIRMATION
    }

    public static void showInfo(String title, String message) {
        showCustomDialog(title, message, DialogType.INFO);
    }

    public static void showWarning(String title, String message) {
        showCustomDialog(title, message, DialogType.WARNING);
    }

    public static void showSuccess(String title, String message) {
        showCustomDialog(title, message, DialogType.SUCCESS);
    }

    public static void showError(String title, String message) {
        showCustomDialog(title, message, DialogType.ERROR);
    }

    // returns true if OK clicked
    public static boolean showConfirmation(String title, String message) {
        return showCustomDialog(title, message, DialogType.CONFIRMATION);
    }

    private static boolean showCustomDialog(String title, String message, DialogType type) {
        AtomicBoolean result = new AtomicBoolean(false);

        if (Platform.isFxApplicationThread()) {
            return buildAndShow(title, message, type, result);
        } else {
            // Unlikely to block off-thread cleanly returning boolean via Platform.runLater
            // But for simple Info/Success calls off-thread, we don't need the return value
            // anyway.
            Platform.runLater(() -> buildAndShow(title, message, type, result));
            return false;
        }
    }

    private static boolean buildAndShow(String title, String message, DialogType type, AtomicBoolean result) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        // Make draggable
        final double[] xOffset = { 0 };
        final double[] yOffset = { 0 };

        VBox root = new VBox(20);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: #2c3e50; -fx-background-radius: 12px; -fx-border-radius: 12px;");

        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });

        // Clip the node to the same radius so the scene corners stay rounded (no pointy
        // bits)
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        FontIcon icon = new FontIcon();
        icon.setIconSize(56);
        String color = "#333";

        switch (type) {
            case SUCCESS:
                icon.setIconLiteral("fas-check-circle");
                color = "#2ecc71";
                break;
            case ERROR:
                icon.setIconLiteral("fas-times-circle");
                color = "#e74c3c";
                break;
            case WARNING:
                icon.setIconLiteral("fas-exclamation-triangle");
                color = "#f39c12";
                break;
            case INFO:
                icon.setIconLiteral("fas-info-circle");
                color = "#3498db";
                break;
            case CONFIRMATION:
                icon.setIconLiteral("fas-question-circle");
                color = "#3498db";
                break;
        }
        icon.setIconColor(Color.web(color));

        // Translation Support
        String translatedTitle = title;
        String translatedMessage = message;
        try {
            if (App.getBundle() != null) {
                if (App.getBundle().containsKey(title)) {
                    translatedTitle = App.getBundle().getString(title);
                }
                if (App.getBundle().containsKey(message)) {
                    translatedMessage = App.getBundle().getString(message);
                }
            }
        } catch (Exception ignored) {
        }

        Label titleLabel = new Label(translatedTitle);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label messageLabel = new Label(translatedMessage);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #ffffffff;");
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setMaxWidth(300);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        String okText = "OK";
        String cancelText = "Cancel";

        try {
            if (App.getBundle() != null && App.getBundle().containsKey("dialog.ok")) {
                okText = App.getBundle().getString("dialog.ok");
            }
            if (App.getBundle() != null && App.getBundle().containsKey("dialog.cancel")) {
                cancelText = App.getBundle().getString("dialog.cancel");
            }
        } catch (Exception ignored) {
        }

        final String finalColor = color;

        if (type == DialogType.CONFIRMATION) {
            Button okBtn = new Button(okText);
            okBtn.setStyle(
                    "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 6px; -fx-cursor: hand;");

            Button cancelBtn = new Button(cancelText);
            cancelBtn.setStyle(
                    "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 6px; -fx-cursor: hand;");

            // Hover effects
            okBtn.setOnMouseEntered(e -> okBtn.setStyle(
                    "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 6px; -fx-cursor: hand;"));
            okBtn.setOnMouseExited(e -> okBtn.setStyle(
                    "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 6px; -fx-cursor: hand;"));

            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
                    "-fx-background-color: #bdc3c7; -fx-text-fill: #2c3e50; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 6px; -fx-cursor: hand;"));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                    "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 6px; -fx-cursor: hand;"));

            okBtn.setOnAction(e -> {
                result.set(true);
                stage.close();
            });

            cancelBtn.setOnAction(e -> {
                result.set(false);
                stage.close();
            });

            btnBox.getChildren().addAll(cancelBtn, okBtn);
        } else {
            Button okBtn = new Button(okText);
            okBtn.setStyle("-fx-background-color: " + finalColor
                    + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 30px; -fx-background-radius: 6px; -fx-cursor: hand;");
            // Generic Hover
            okBtn.setOnMouseEntered(e -> okBtn.setStyle(
                    "-fx-background-color: #bdc3c7; -fx-text-fill: #2c3e50; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 30px; -fx-background-radius: 6px; -fx-cursor: hand;"));
            okBtn.setOnMouseExited(e -> okBtn.setStyle("-fx-background-color: " + finalColor
                    + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 30px; -fx-background-radius: 6px; -fx-cursor: hand;"));

            okBtn.setOnAction(e -> stage.close());
            btnBox.getChildren().add(okBtn);
        }

        root.getChildren().addAll(icon, titleLabel, messageLabel, btnBox);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Animations Setup (Scaling up from small, fading in)
        root.setOpacity(0);
        root.setScaleX(0.7);
        root.setScaleY(0.7);

        FadeTransition ft = new FadeTransition(Duration.millis(250), root);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(250), root);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, st);

        stage.setOnShown(e -> pt.play());

        stage.showAndWait();
        return result.get();
    }
}
