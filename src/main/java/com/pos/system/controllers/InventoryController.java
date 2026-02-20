package com.pos.system.controllers;

import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class InventoryController {

    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, byte[]> imageCol; // Changed to byte[]
    @FXML
    private TableColumn<Product, String> barcodeCol;
    @FXML
    private TableColumn<Product, String> nameCol;
    @FXML
    private TableColumn<Product, String> categoryCol;
    @FXML
    private TableColumn<Product, Double> costCol;
    @FXML
    private TableColumn<Product, Double> priceCol;
    @FXML
    private TableColumn<Product, Integer> stockCol;
    @FXML
    private TextField searchField;

    @FXML
    public void initialize() {
        // Image Column Setup
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imageData"));
        imageCol.setCellFactory(col -> new javafx.scene.control.TableCell<Product, byte[]>() {
            private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(byte[] imageData, boolean empty) {
                super.updateItem(imageData, empty);
                if (empty || imageData == null || imageData.length == 0) {
                    setGraphic(null);
                } else {
                    try {
                        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageData);
                        imageView.setImage(new javafx.scene.image.Image(bis));
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });

        // Bind columns
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        costCol.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Auto-resize columns
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        loadProducts();
    }

    @FXML
    private void handleDeleteProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Selection Error");
            alert.setHeaderText(null);
            alert.setContentText("Please select a product to delete.");
            alert.showAndWait();
            return;
        }

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Product");
        confirm.setHeaderText("Delete " + selectedProduct.getName() + "?");
        confirm.setContentText("Are you sure you want to delete this product?");

        if (confirm.showAndWait()
                .orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) {
            try (ProductDAO productDAO = new ProductDAO()) {
                productDAO.deleteProduct(selectedProduct.getId());
                loadProducts();
            } catch (SQLException e) {
                e.printStackTrace();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to delete product: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void loadProducts() {
        try (ProductDAO productDAO = new ProductDAO()) {
            List<Product> products = productDAO.getAllProducts();
            ObservableList<Product> observableProducts = FXCollections.observableArrayList(products);
            productTable.setItems(observableProducts);
        } catch (SQLException e) {
            e.printStackTrace();
            // In a real app, show an Alert
        }
    }

    @FXML
    private void handleSearch() {
        // String query = searchField.getText().toLowerCase(); // Unused for now
        // Simple search: Re-query DB or filter current list.
        // For simplicity, let's just refresh for now or implement filter later.
        loadProducts(); // placeholder
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
    }

    @FXML
    private void handleEditProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            // Show warning
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Selection Error");
            alert.setHeaderText(null);
            alert.setContentText("Please select a product to edit.");
            alert.showAndWait();
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/add_product.fxml"));
            javafx.scene.Parent root = loader.load();

            AddProductController controller = loader.getController();
            controller.setProductToEdit(selectedProduct); // Pre-fill data
            controller.setOnSaveCallback(this::loadProducts);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Product");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddProduct() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/add_product.fxml"));
            javafx.scene.Parent root = loader.load();

            AddProductController controller = loader.getController();
            controller.setOnSaveCallback(this::loadProducts);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add Product");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
