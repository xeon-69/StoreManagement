package com.pos.system.controllers;

import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AddProductController {

    @FXML
    private TextField barcodeField;
    @FXML
    private TextField nameField;
    @FXML
    private javafx.scene.control.ComboBox<com.pos.system.models.Category> categoryComboBox;
    @FXML
    private TextField costPriceField;
    @FXML
    private TextField sellingPriceField;
    @FXML
    private TextField stockField;
    @FXML
    private Label messageLabel;
    @FXML
    private javafx.scene.image.ImageView productImageView;

    private String selectedImagePath = null;
    private Runnable onSaveCallback;

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    public void initialize() {
        loadCategories();
    }

    private void loadCategories() {
        try (com.pos.system.dao.CategoryDAO dao = new com.pos.system.dao.CategoryDAO()) {
            categoryComboBox.setItems(javafx.collections.FXCollections.observableArrayList(dao.getAllCategories()));
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Failed to load categories.");
        }
    }

    @FXML
    private void handleBrowseImage() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        java.io.File selectedFile = fileChooser.showOpenDialog(messageLabel.getScene().getWindow());
        if (selectedFile != null) {
            selectedImagePath = selectedFile.toURI().toString(); // Store as URI string for portability/loading
            productImageView.setImage(new javafx.scene.image.Image(selectedImagePath));
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput())
            return;

        try (ProductDAO productDAO = new ProductDAO()) {
            String categoryName = "";
            if (categoryComboBox.getValue() != null) {
                categoryName = categoryComboBox.getValue().getName();
            }

            Product product = new Product(
                    0, // ID auto-generated
                    barcodeField.getText().trim(),
                    nameField.getText().trim(),
                    categoryName, // Use name from category object
                    Double.parseDouble(costPriceField.getText().trim()),
                    Double.parseDouble(sellingPriceField.getText().trim()),
                    Integer.parseInt(stockField.getText().trim()),
                    selectedImagePath); // Pass image path

            productDAO.addProduct(product);

            messageLabel.setText("Product saved!");
            messageLabel.setStyle("-fx-text-fill: green;");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();

        } catch (NumberFormatException e) {
            messageLabel.setText("Invalid number format for price or stock.");
            messageLabel.setStyle("-fx-text-fill: red;");
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
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
        if (barcodeField.getText().isEmpty() || nameField.getText().isEmpty() ||
                sellingPriceField.getText().isEmpty()) {
            messageLabel.setText("Please fill all required fields.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }
}
