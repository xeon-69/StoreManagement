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

    @FXML
    private Label titleLabel; // Added reference

    private Product productToEdit; // To track edit mode
    private byte[] selectedImageData = null; // Changed from String path to byte[]
    private Runnable onSaveCallback;

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    public void initialize() {
        loadCategories();
    }

    // New method to initialize controller in "Edit Mode"
    public void setProductToEdit(Product product) {
        this.productToEdit = product;
        if (product != null) {
            titleLabel.setText("Edit Product");
            barcodeField.setText(product.getBarcode());
            nameField.setText(product.getName());

            // Set Category
            if (product.getCategory() != null) {
                for (com.pos.system.models.Category c : categoryComboBox.getItems()) {
                    if (c.getName().equalsIgnoreCase(product.getCategory())) {
                        categoryComboBox.setValue(c);
                        break;
                    }
                }
            }

            costPriceField.setText(String.valueOf(product.getCostPrice()));
            sellingPriceField.setText(String.valueOf(product.getSellingPrice()));
            stockField.setText(String.valueOf(product.getStock()));

            selectedImageData = product.getImageData();
            if (selectedImageData != null && selectedImageData.length > 0) {
                try {
                    java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(selectedImageData);
                    productImageView.setImage(new javafx.scene.image.Image(bis));
                } catch (Exception e) {
                    // Ignore image load error
                }
            }
        }
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
            try {
                selectedImageData = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(selectedImageData);
                productImageView.setImage(new javafx.scene.image.Image(bis));
            } catch (java.io.IOException e) {
                messageLabel.setText("Failed to load image: " + e.getMessage());
            }
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

            // If editing, keep ID, else 0 (auto-generated)
            int id = (productToEdit != null) ? productToEdit.getId() : 0;

            Product product = new Product(
                    id,
                    barcodeField.getText().trim(),
                    nameField.getText().trim(),
                    categoryName,
                    Double.parseDouble(costPriceField.getText().trim()),
                    Double.parseDouble(sellingPriceField.getText().trim()),
                    Integer.parseInt(stockField.getText().trim()),
                    selectedImageData);

            if (productToEdit != null) {
                productDAO.updateProduct(product); // Update
                messageLabel.setText("Product updated!");
            } else {
                productDAO.addProduct(product); // Add
                messageLabel.setText("Product saved!");
            }

            messageLabel.setStyle("-fx-text-fill: green;");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();

        } catch (NumberFormatException e) {
            messageLabel.setText("Invalid number format for price or stock.");
            messageLabel.setStyle("-fx-text-fill: red;");
        } catch (SQLException e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                messageLabel.setText("Error: Barcode already exists.");
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
        if (barcodeField.getText().isEmpty() || nameField.getText().isEmpty() ||
                sellingPriceField.getText().isEmpty()) {
            messageLabel.setText("Please fill all required fields.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }
}
