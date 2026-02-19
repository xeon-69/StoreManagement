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
import javafx.geometry.Insets;
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

        // Cart Table Setup
        cNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        // Custom Cell Factory for Quantity with +/- buttons
        cQtyCol.setCellFactory(param -> new TableCell<>() {
            private final Button minusBtn = new Button("-");
            private final Button plusBtn = new Button("+");
            private final Label qtyLabel = new Label();
            private final HBox pane = new HBox(5, minusBtn, qtyLabel, plusBtn);

            {
                pane.setAlignment(Pos.CENTER);
                minusBtn.setOnAction(event -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    updateCartItemQuantity(item, -1);
                });
                plusBtn.setOnAction(event -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    updateCartItemQuantity(item, 1);
                });
                // Small buttons
                minusBtn.setPrefWidth(25);
                plusBtn.setPrefWidth(25);
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SaleItem saleItem = getTableView().getItems().get(getIndex());
                    qtyLabel.setText(String.valueOf(saleItem.getQuantity()));
                    setGraphic(pane);
                }
            }
        });

        cTotalCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotal()).asObject());

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
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
        card.setPrefWidth(150);
        card.setPrefHeight(200); // Increased height for image
        card.setAlignment(Pos.CENTER);

        // Image Handling
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try {
                // Try treating as URL or File Path
                String imagePath = p.getImagePath();
                if (!imagePath.startsWith("http") && !imagePath.startsWith("file:")) {
                    imagePath = "file:///" + imagePath;
                }
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(imagePath);
                imageView.setFitHeight(80);
                imageView.setFitWidth(80);
                imageView.setPreserveRatio(true);
                card.getChildren().add(imageView);
            } catch (Exception e) {
                // Fallback text if image fails
                Label imgPlaceholder = new Label("No Image");
                imgPlaceholder.setStyle("-fx-text-fill: gray;");
                card.getChildren().add(imgPlaceholder);
            }
        } else {
            Label imgPlaceholder = new Label("No Image");
            imgPlaceholder.setStyle(
                    "-fx-text-fill: #bdc3c7; -fx-border-color: #bdc3c7; -fx-border-style: dashed; -fx-padding: 20;");
            card.getChildren().add(imgPlaceholder);
        }

        Label nameLbl = new Label(p.getName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        nameLbl.setWrapText(true);

        Label priceLbl = new Label(String.format("$%.2f", p.getSellingPrice()));
        priceLbl.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");

        Label stockLbl = new Label("Stock: " + p.getStock());
        if (p.getStock() < 10) {
            stockLbl.setStyle("-fx-text-fill: red;");
        } else {
            stockLbl.setStyle("-fx-text-fill: gray;");
        }

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("accent"); // AtlantaFX accent style
        addBtn.setOnAction(e -> addToCart(p));

        if (p.getStock() <= 0) {
            addBtn.setDisable(true);
            addBtn.setText("Out of Stock");
        }

        card.getChildren().addAll(nameLbl, priceLbl, stockLbl, addBtn);
        return card;
    }

    private void updateCartItemQuantity(SaleItem item, int change) {
        int newQty = item.getQuantity() + change;
        if (newQty <= 0) {
            cartItems.remove(item);
        } else {
            // Check Stock
            // We need to look up the product stock again to be safe, or trust the catalog
            // ViewModel
            // For now, let's find the product in our loaded list
            Product product = catalogViewModel.getFilteredProducts().stream()
                    .filter(p -> p.getId() == item.getProductId())
                    .findFirst()
                    .orElse(null);

            if (product != null) {
                if (newQty > product.getStock()) {
                    showAlert("Error", "Not enough stock!");
                    return;
                }
            }

            item.setQuantity(newQty);
            // Force refresh of table
            cartTable.refresh();
        }
        updateTotal();
    }

    @FXML
    private void handleSearch() {
        catalogViewModel.search(searchField.getText());
    }

    private void addToCart(Product selected) {
        if (selected.getStock() <= 0) {
            showAlert("Error", "Product out of stock!");
            return;
        }

        // Check if already in cart
        for (SaleItem item : cartItems) {
            if (item.getProductId() == selected.getId()) {
                updateCartItemQuantity(item, 1);
                return;
            }
        }

        // Add to cart (Default Qty 1)
        SaleItem item = new SaleItem(
                0, 0, selected.getId(), selected.getName(), 1,
                selected.getSellingPrice(), selected.getCostPrice());
        cartItems.add(item);
        updateTotal();
    }

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
        totalLabel.setText(String.format("$%.2f", total));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
