package com.pos.system.controllers;

import com.pos.system.models.Product;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.utils.SessionManager;
import com.pos.system.viewmodels.ProductCatalogViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class POSController {

    @FXML
    private TextField searchField;
    @FXML
    private TilePane productTilePane; // Added TilePane

    // Removed TableView and Columns for products

    @FXML
    private TableView<SaleItem> cartTable;
    @FXML
    private TableColumn<SaleItem, String> cNameCol;
    @FXML
    private TableColumn<SaleItem, Integer> cQtyCol;
    @FXML
    private TableColumn<SaleItem, Double> cTotalCol;

    @FXML
    private Label totalLabel;

    // DAOs will be instantiated as needed to manage connections properly
    // private final ProductDAO productDAO = new ProductDAO();
    // private final SaleDAO saleDAO = new SaleDAO();

    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private ProductCatalogViewModel catalogViewModel; // Added ViewModel

    @FXML
    public void initialize() {
        // Initialize ViewModel
        catalogViewModel = new ProductCatalogViewModel();

        // Bind Search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> catalogViewModel.search(newVal));

        // Bind List Updates
        catalogViewModel.getFilteredProducts()
                .addListener((javafx.collections.ListChangeListener.Change<? extends Product> c) -> {
                    renderProductTiles(catalogViewModel.getFilteredProducts());
                });

        // Cart Table Setup - Receipt Style
        cNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        cQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cTotalCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotal()).asObject());

        // Receipt Styling preference
        cartTable.setPlaceholder(new Label("Cart is empty"));

        cartTable.setItems(cartItems);
        // Ensure catalog loads
        catalogViewModel.loadProducts();
    }

    private void renderProductTiles(List<Product> products) {
        Platform.runLater(() -> {
            productTilePane.getChildren().clear();
            for (Product p : products) {
                productTilePane.getChildren().add(createProductTile(p));
            }
        });
    }

    private VBox createProductTile(Product p) {
        VBox card = new VBox(5);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(150);
        card.setPrefHeight(230);
        card.setAlignment(Pos.CENTER);

        // Image Handling
        if (p.getImageData() != null && p.getImageData().length > 0) {
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(p.getImageData());
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(bis));
                imageView.setFitHeight(80);
                imageView.setFitWidth(80);
                imageView.setPreserveRatio(true);
                card.getChildren().add(imageView);
            } catch (Exception e) {
                Label imgPlaceholder = new Label("No Image");
                imgPlaceholder.setStyle("-fx-text-fill: gray;"); // Keep simple or add class
                card.getChildren().add(imgPlaceholder);
            }
        } else {
            Label imgPlaceholder = new Label("No Image");
            imgPlaceholder.setStyle(
                    "-fx-text-fill: #bdc3c7; -fx-border-color: #bdc3c7; -fx-border-style: dashed; -fx-padding: 20;");
            card.getChildren().add(imgPlaceholder);
        }

        Label nameLbl = new Label(p.getName());
        nameLbl.getStyleClass().add("product-name");
        nameLbl.setWrapText(true);

        Label priceLbl = new Label(String.format("%.0f MMK", p.getSellingPrice()));
        priceLbl.getStyleClass().add("product-price");

        Label stockLbl = new Label("Stock: " + p.getStock());
        stockLbl.getStyleClass().add("stock-label");
        if (p.getStock() < 10) {
            stockLbl.getStyleClass().add("stock-low");
        }

        // Stepper Control
        HBox stepper = new HBox(10);
        stepper.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().addAll("stepper-btn", "stepper-btn-minus");

        Label qtyLbl = new Label("0");
        qtyLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); // Could move to CSS

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().addAll("stepper-btn", "stepper-btn-plus");

        minusBtn.setOnAction(e -> updateCartQuantity(p, -1));
        plusBtn.setOnAction(e -> updateCartQuantity(p, 1));

        if (p.getStock() <= 0) {
            plusBtn.setDisable(true);
            stockLbl.setText("Out of Stock");
        }

        stepper.getChildren().addAll(minusBtn, qtyLbl, plusBtn);

        // Sync Logic: Update qtyLbl when cart changes
        Runnable updateLabel = () -> {
            int currentQty = 0;
            for (SaleItem item : cartItems) {
                if (item.getProductId() == p.getId()) {
                    currentQty = item.getQuantity();
                    break;
                }
            }
            qtyLbl.setText(String.valueOf(currentQty));

            if (currentQty >= p.getStock()) {
                plusBtn.setDisable(true);
            } else {
                plusBtn.setDisable(false);
            }
        };

        // Initial update
        updateLabel.run();

        // Listen for global cart changes
        cartItems.addListener((javafx.collections.ListChangeListener<SaleItem>) c -> {
            Platform.runLater(updateLabel);
        });

        card.getChildren().addAll(nameLbl, priceLbl, stockLbl, stepper);
        return card;
    }

    private void updateCartQuantity(Product p, int change) {
        // Find if item exists
        SaleItem existingItem = null;
        int index = -1;
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getProductId() == p.getId()) {
                existingItem = cartItems.get(i);
                index = i;
                break;
            }
        }

        if (existingItem != null) {
            int newQty = existingItem.getQuantity() + change;
            if (newQty <= 0) {
                cartItems.remove(existingItem);
            } else {
                if (newQty > p.getStock()) {
                    showAlert("Error", "Not enough stock!");
                    return;
                }
                existingItem.setQuantity(newQty);
                // Trigger list update event by replacing the item
                cartItems.set(index, existingItem);

                cartTable.refresh();
            }
        } else if (change > 0) {
            // Add new
            if (p.getStock() <= 0) {
                showAlert("Error", "Out of stock!");
                return;
            }
            SaleItem item = new SaleItem(
                    0, 0, p.getId(), p.getName(), 1,
                    p.getSellingPrice(), p.getCostPrice());
            cartItems.add(item);
        }

        updateTotal();
    }

    @FXML
    private void handleSearch() {
        catalogViewModel.search(searchField.getText());
    }

    // addToCart removed, replaced by updateCartQuantity

    @FXML
    private void handleClearCart() {
        cartItems.clear();
        updateTotal();
    }

    @FXML
    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            showAlert("Error", "Cart is empty!");
            return;
        }

        double totalAmount = cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
        double totalCost = cartItems.stream().mapToDouble(i -> i.getCostAtSale() * i.getQuantity()).sum();
        double totalProfit = totalAmount - totalCost;

        int userId = SessionManager.getInstance().getCurrentUser().getId();

        Sale sale = new Sale(0, userId, totalAmount, totalProfit, LocalDateTime.now());

        try {
            com.pos.system.services.CheckoutService checkoutService = new com.pos.system.services.CheckoutService();
            checkoutService.processCheckout(sale, new ArrayList<>(cartItems));

            showAlert("Success", "Transaction Completed!");
            cartItems.clear();
            updateTotal();

            // Refresh stock via ViewModel
            catalogViewModel.loadProducts();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Checkout Failed: " + e.getMessage());
        }
    }

    private void updateTotal() {
        double total = cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
        totalLabel.setText(String.format("%.2f MMK", total));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
