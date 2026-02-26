package com.pos.system.controllers;

import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import com.pos.system.services.SecurityService;
import com.pos.system.utils.SessionManager;

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
    private SecurityService securityService;

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    public void initialize() {
        try {
            securityService = new SecurityService();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        loadCategories();
    }

    // New method to initialize controller in "Edit Mode"
    public void setProductToEdit(Product product) {
        this.productToEdit = product;
        if (product != null) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            titleLabel.setText(b.getString("inventory.editProduct.title"));
            barcodeField.setText(product.getBarcode());
            nameField.setText(product.getName());

            // Set Category
            if (product.getCategoryId() > 0) {
                for (com.pos.system.models.Category c : categoryComboBox.getItems()) {
                    if (c.getId() == product.getCategoryId()) {
                        categoryComboBox.setValue(c);
                        break;
                    }
                }
            }

            costPriceField.setText(String.valueOf(product.getCostPrice()));
            sellingPriceField.setText(String.valueOf(product.getSellingPrice()));
            stockField.setText(String.valueOf(product.getStock()));
            stockField.setDisable(true); // Disable manual stock overrides for Ledger integrity

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
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("inventory.addProduct.loadCategoriesFail"));
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
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                messageLabel.setText(String.format(b.getString("inventory.addProduct.loadImageFail"), e.getMessage()));
            }
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput())
            return;

        try (ProductDAO productDAO = new ProductDAO()) {
            String categoryName = "";
            int categoryId = 0;
            if (categoryComboBox.getValue() != null) {
                categoryName = categoryComboBox.getValue().getName();
                categoryId = categoryComboBox.getValue().getId();
            }

            // If editing, keep ID, else 0 (auto-generated)
            int id = (productToEdit != null) ? productToEdit.getId() : 0;

            int parsedStock = Integer.parseInt(stockField.getText().trim());
            Product product; // Added line

            if (productToEdit != null) {
                // Editing existing product: Force retain existing stock from cache
                product = new Product(id, barcodeField.getText().trim(), nameField.getText().trim(), categoryId,
                        categoryName, Double.parseDouble(costPriceField.getText().trim()),
                        Double.parseDouble(sellingPriceField.getText().trim()), productToEdit.getStock(),
                        selectedImageData);
                productDAO.updateProduct(product); // Update
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                messageLabel.setText(b.getString("inventory.editProduct.success"));

                if (securityService != null) {
                    securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                            "UPDATE_PRODUCT", "Product", String.valueOf(id),
                            "Name: " + nameField.getText().trim());
                }
            } else {
                // Adding an entirely new product
                product = new Product(id, barcodeField.getText().trim(), nameField.getText().trim(), categoryId,
                        categoryName, Double.parseDouble(costPriceField.getText().trim()),
                        Double.parseDouble(sellingPriceField.getText().trim()), 0, selectedImageData);
                productDAO.addProduct(product); // Add with 0 stock natively

                if (parsedStock > 0) {
                    Product newlyCreated = productDAO.getProductByBarcode(product.getBarcode());
                    try (java.sql.Connection conn = com.pos.system.database.DatabaseManager.getInstance()
                            .getConnection()) {
                        com.pos.system.services.InventoryService invService = new com.pos.system.services.InventoryService();
                        invService.addStock(conn, newlyCreated.getId(), parsedStock, newlyCreated.getCostPrice(), null,
                                "INITIAL-ADD", SessionManager.getInstance().getCurrentUser().getId());
                    }
                }

                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                messageLabel.setText(b.getString("inventory.addProduct.success"));

                if (securityService != null) {
                    securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                            "CREATE_PRODUCT", "Product", barcodeField.getText().trim(),
                            "Name: " + nameField.getText().trim());
                }
            }

            messageLabel.setStyle("-fx-text-fill: green;");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();

        } catch (NumberFormatException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("inventory.addProduct.invalidFormat"));
            messageLabel.setStyle("-fx-text-fill: red;");
        } catch (SQLException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                messageLabel.setText(b.getString("inventory.addProduct.barcodeExists"));
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
        if (barcodeField.getText().isEmpty() || nameField.getText().isEmpty() ||
                sellingPriceField.getText().isEmpty()) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("inventory.addProduct.fillRequired"));
            messageLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }
}
