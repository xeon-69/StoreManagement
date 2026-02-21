package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import java.util.Locale;

import java.io.IOException;

public class DashboardController {

    private static String currentActiveView = null;

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label currentUserLabel;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().isLoggedIn()) {
            currentUserLabel.setText(
                    getString("nav.user", "User: ") + SessionManager.getInstance().getCurrentUser().getUsername());
        }

        languageComboBox.getItems().addAll("English", "မြန်မာစာ", "中文");

        Locale current = com.pos.system.App.getBundle().getLocale();
        if ("my".equals(current.getLanguage())) {
            languageComboBox.setValue("မြန်မာစာ");
        } else if ("zh".equals(current.getLanguage())) {
            languageComboBox.setValue("中文");
        } else {
            languageComboBox.setValue("English");
        }

        languageComboBox.setOnAction(e -> {
            String selected = languageComboBox.getValue();
            if ("English".equals(selected)) {
                com.pos.system.App.setLocale("en");
            } else if ("မြန်မာစာ".equals(selected)) {
                com.pos.system.App.setLocale("my");
            } else if ("中文".equals(selected)) {
                com.pos.system.App.setLocale("zh");
            }
        });

        if (currentActiveView != null) {
            loadView(currentActiveView);
        }
    }

    private String getString(String key, String defaultVal) {
        try {
            return com.pos.system.App.getBundle().getString(key);
        } catch (Exception e) {
            return defaultVal;
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
        currentActiveView = null; // Clear view on logout
        App.setRoot("login");
    }

    private void loadView(String fxml) {
        currentActiveView = fxml;
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
            loader.setResources(App.getBundle());
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
