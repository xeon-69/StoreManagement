package com.pos.system.controllers;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.models.Category;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

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

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    public void setCategoryToEdit(Category category) {
        this.categoryToEdit = category;
        if (category != null) {
            titleLabel.setText("Edit Category"); // Fallback text when no resource bundle key logic explicitly replaces
                                                 // it, though FXML takes precedence initially. We can keep it simple.
            nameField.setText(category.getName());
            descriptionArea.setText(category.getDescription());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput())
            return;

        try (CategoryDAO dao = new CategoryDAO()) {
            int id = (categoryToEdit != null) ? categoryToEdit.getId() : 0;
            Category category = new Category(id, nameField.getText().trim(), descriptionArea.getText().trim());

            if (categoryToEdit != null) {
                dao.updateCategory(category);
                messageLabel.setText("Category updated!");
            } else {
                dao.addCategory(category);
                messageLabel.setText("Category saved!");
            }

            messageLabel.setStyle("-fx-text-fill: green;");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();

        } catch (SQLException e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                messageLabel.setText("Error: Category name already exists.");
            } else {
                messageLabel.setText("Database error: " + e.getMessage());
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
            messageLabel.setText("Please enter a category name.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }
}
