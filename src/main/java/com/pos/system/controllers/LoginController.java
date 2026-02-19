package com.pos.system.controllers;

import com.pos.system.dao.UserDAO;
import com.pos.system.models.User;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        try (UserDAO userDAO = new UserDAO()) {
            User user = userDAO.login(username, password);

            if (user != null) {
                // Successful Login
                SessionManager.getInstance().setCurrentUser(user);
                // Switch to Main View
                try {
                    com.pos.system.App.setRoot("dashboard");
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    errorLabel.setText("Error loading POS.");
                }
            } else {
                errorLabel.setText("Invalid username or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }
}
