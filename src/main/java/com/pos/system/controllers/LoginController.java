package com.pos.system.controllers;

import com.pos.system.dao.UserDAO;
import com.pos.system.models.User;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import java.util.Locale;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    public void initialize() {
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
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText(getString("error.emptyFields", "Please enter both username and password."));
            return;
        }

        try (UserDAO userDAO = new UserDAO()) {
            User user = userDAO.getUserByUsername(username);

            com.pos.system.services.SecurityService securityService = new com.pos.system.services.SecurityService();

            if (user != null && securityService.verifyPassword(password, user.getPassword())) {
                // Successful Login
                SessionManager.getInstance().setCurrentUser(user);
                securityService.logAction(user.getId(), "LOGIN", "USER", String.valueOf(user.getId()),
                        "Successful login");

                try {
                    // Start Background Services
                    com.pos.system.services.StoreMonitorService monitorService = new com.pos.system.services.StoreMonitorService();
                    monitorService.start();

                    com.pos.system.App.setRoot("dashboard");
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    errorLabel.setText("Error loading next view.");
                }
            } else {
                errorLabel.setText(getString("error.invalidLogin", "Invalid username or password."));
                if (user != null) {
                    securityService.logAction(user.getId(), "LOGIN_FAILED", "USER", String.valueOf(user.getId()),
                            "Failed login attempt");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText(getString("error.database", "Database error: ") + e.getMessage());
        }
    }

    private String getString(String key, String defaultVal) {
        try {
            return com.pos.system.App.getBundle().getString(key);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
