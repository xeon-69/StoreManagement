package com.pos.system.controllers;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.models.Category;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import com.pos.system.services.SecurityService;
import com.pos.system.utils.SessionManager;

public class AddCategoryController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label messageLabel;
    @FXML
    private Label titleLabel; // Added reference

    private Category categoryToEdit; // To track edit mode
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        // No persistent services as they hold DB connections
    }

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    public void setCategoryToEdit(Category category) {
        this.categoryToEdit = category;
        if (category != null) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            titleLabel.setText(b.getString("category.edit"));
            nameField.setText(category.getName());
            descriptionArea.setText(category.getDescription());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput())
            return;

        try (CategoryDAO dao = new CategoryDAO();
                SecurityService securityService = new SecurityService()) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            int id = (categoryToEdit != null) ? categoryToEdit.getId() : 0;
            Category category = new Category(id, nameField.getText().trim(), descriptionArea.getText().trim());

            if (categoryToEdit != null) {
                dao.updateCategory(category);
                messageLabel.setText(b.getString("category.update.success"));

                securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                        "UPDATE_CATEGORY", "Category", String.valueOf(id),
                        "Name: " + category.getName());
            } else {
                dao.addCategory(category);
                messageLabel.setText(b.getString("category.add.success"));

                securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                        "CREATE_CATEGORY", "Category", "N/A",
                        "Name: " + category.getName());
            }

            messageLabel.setStyle("-fx-text-fill: green;");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();

        } catch (SQLException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                messageLabel.setText(b.getString("category.name.exists"));
            } else {
                messageLabel.setText(b.getString("dialog.dbError") + ": " + e.getMessage());
            }
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) messageLabel.getScene().getWindow();
        stage.close();
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("category.name.empty"));
            messageLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }
}
