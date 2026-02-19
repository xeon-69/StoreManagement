package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class DashboardController {

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label currentUserLabel;

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().isLoggedIn()) {
            currentUserLabel.setText("User: " + SessionManager.getInstance().getCurrentUser().getUsername());
        } else {
            // Handle case where user is not logged in (redirect to login or exit)
            // For safety in dev, we might verify this.
        }
    }

    @FXML
    private void showPOS() {
        loadView("pos");
    }

    @FXML
    private void showInventory() {
        loadView("inventory");
    }

    @FXML
    private void showCategories() {
        loadView("categories");
    }

    @FXML
    private void showFinance() {
        loadView("finance");
    }

    @FXML
    private void showReports() {
        loadView("reports");
    }

    @FXML
    private void logout() throws IOException {
        SessionManager.getInstance().logout();
        App.setRoot("login");
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
            Parent view = loader.load();
            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            // Show error in center if view missing
            Label errorLabel = new Label("Failed to load view: " + fxml);
            mainPane.setCenter(errorLabel);
        }
    }
}
