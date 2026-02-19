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
        // Bind columns
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        costCol.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));

        loadProducts();
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
        String query = searchField.getText().toLowerCase();
        // Simple search: Re-query DB or filter current list.
        // For simplicity, let's just refresh for now or implement filter later.
        loadProducts(); // placeholder
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
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
