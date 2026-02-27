package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.services.HardwareService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ResourceBundle;

public class HardwareController {

    @FXML
    private Circle scannerLight;
    @FXML
    private Label scannerStatusLabel;

    @FXML
    private Circle printerLight;
    @FXML
    private Label printerStatusLabel;

    @FXML
    private Circle drawerLight;
    @FXML
    private Label drawerStatusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    private final HardwareService hardwareService = new HardwareService();
    private ResourceBundle bundle;

    @FXML
    public void initialize() {
        bundle = App.getBundle();
        refresh();
    }

    @FXML
    private void refresh() {
        loadingIndicator.setVisible(true);

        hardwareService.checkStatus().thenAccept(status -> {
            Platform.runLater(() -> {
                updateUI(status);
                loadingIndicator.setVisible(false);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                // Handle error if needed
            });
            return null;
        });
    }

    private void updateUI(HardwareService.HardwareStatus status) {
        // Scanner
        if (status.scannerConnected) {
            scannerLight.setFill(Color.web("#2ecc71")); // Green
            scannerStatusLabel.setText(bundle.getString("hardware.connected"));
        } else {
            scannerLight.setFill(Color.web("#e74c3c")); // Red
            scannerStatusLabel.setText(bundle.getString("hardware.disconnected"));
        }

        // Printer
        if (status.printerConnected) {
            printerLight.setFill(Color.web("#2ecc71")); // Green
            printerStatusLabel.setText(bundle.getString("hardware.printer.ready"));
        } else {
            printerLight.setFill(Color.web("#e74c3c")); // Red
            printerStatusLabel.setText(bundle.getString("hardware.disconnected"));
        }

        // Drawer
        if (status.drawerLinked) {
            drawerLight.setFill(Color.web("#2ecc71")); // Green
            drawerStatusLabel.setText(bundle.getString("hardware.printer.linked"));
        } else {
            drawerLight.setFill(Color.web("#e74c3c")); // Red
            drawerStatusLabel.setText(bundle.getString("hardware.disconnected"));
        }
    }
}
