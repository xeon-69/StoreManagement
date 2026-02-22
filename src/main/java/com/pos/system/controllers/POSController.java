package com.pos.system.controllers;

import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import com.pos.system.models.SaleItem;
import com.pos.system.utils.NotificationUtils;
import com.pos.system.viewmodels.ProductCatalogViewModel;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import org.controlsfx.control.GridView;
import org.controlsfx.control.GridCell;

public class POSController {

    @FXML
    private TextField searchField;
    @FXML
    private GridView<Product> productGridView;

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
    private ProductCatalogViewModel catalogViewModel;
    private PauseTransition searchDebounce;

    // For Dependency Injection in Tests
    public void setCatalogViewModel(ProductCatalogViewModel catalogViewModel) {
        this.catalogViewModel = catalogViewModel;
    }

    @FXML
    public void initialize() {
        // Initialize ViewModel if not injected
        if (catalogViewModel == null) {
            catalogViewModel = new ProductCatalogViewModel();
        }

        // Debounced search — waits 300ms after last keystroke before filtering
        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(event -> {
            String query = searchField.getText();
            if (query != null) {
                query = query.replace("\n", "").replace("\r", "").trim();
            }
            // Only trigger filter if query >= 2 chars or empty (reset)
            if (query == null || query.isEmpty() || query.length() >= 2) {
                catalogViewModel.search(query);
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.playFromStart();
        });

        // Enter key = Barcode scanner input → look up product by barcode and add to
        // cart
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String barcode = searchField.getText();
                if (barcode != null) {
                    barcode = barcode.replace("\n", "").replace("\r", "").trim();
                }
                if (barcode != null && !barcode.isEmpty()) {
                    handleBarcodeInput(barcode);
                }
                event.consume();
            }
        });

        // Bind List Updates
        productGridView.setItems(catalogViewModel.getFilteredProducts());

        // Setup Virtualized Cell Factory
        productGridView.setCellFactory(gridView -> new GridCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createProductTile(item));
                }
            }
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

        // Auto-focus search field on load
        Platform.runLater(() -> searchField.requestFocus());
    }

    /**
     * Handles barcode scanner input: looks up product by barcode,
     * adds it to cart (or increments qty), clears field, and re-focuses.
     */
    private void handleBarcodeInput(String barcode) {
        try (ProductDAO productDAO = new ProductDAO()) {
            Product product = productDAO.getProductByBarcode(barcode);
            if (product != null) {
                updateCartQuantity(product, 1);
            } else {
                NotificationUtils.showWarning(
                        com.pos.system.App.getBundle().getString("dialog.warning"),
                        "No product found for barcode: " + barcode);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            NotificationUtils.showError(
                    com.pos.system.App.getBundle().getString("dialog.error"),
                    "Failed to look up barcode.");
        }
        // Clear and re-focus for next scan
        searchField.clear();
        searchField.requestFocus();
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

        Label priceLbl = new Label(String.format("%,.0f MMK", p.getSellingPrice()));
        priceLbl.getStyleClass().add("product-price");

        Label stockLbl = new Label(com.pos.system.App.getBundle().getString("pos.stockLabel") + p.getStock());
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
        plusBtn.setId("add-btn-" + p.getId());
        plusBtn.getStyleClass().addAll("stepper-btn", "stepper-btn-plus");

        minusBtn.setOnAction(e -> updateCartQuantity(p, -1));
        plusBtn.setOnAction(e -> updateCartQuantity(p, 1));

        if (p.getStock() <= 0) {
            plusBtn.setDisable(true);
            stockLbl.setText("Out of Stock");
        }

        stepper.getChildren().addAll(plusBtn, qtyLbl, minusBtn);

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
                    NotificationUtils.showWarning(com.pos.system.App.getBundle().getString("dialog.warning"),
                            "Not enough stock!");
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
                NotificationUtils.showWarning(com.pos.system.App.getBundle().getString("dialog.warning"),
                        "Out of stock!");
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
        String query = searchField.getText();
        if (query != null) {
            query = query.replace("\n", "").replace("\r", "").trim();
        }
        catalogViewModel.search(query);
        searchField.requestFocus();
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
            NotificationUtils.showWarning(com.pos.system.App.getBundle().getString("dialog.warning"), "Cart is empty!");
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/checkout_modal.fxml"));
            loader.setResources(com.pos.system.App.getBundle());
            javafx.scene.Parent root = loader.load();

            CheckoutController controller = loader.getController();
            controller.setCheckoutData(cartItems, () -> {
                Platform.runLater(() -> {
                    NotificationUtils.showSuccess(com.pos.system.App.getBundle().getString("dialog.success"),
                            "Transaction Completed!");
                    cartItems.clear();
                    updateTotal();
                    catalogViewModel.loadProducts();
                });
            });

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(totalLabel.getScene().getWindow());
            stage.setTitle("Checkout");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            // Re-focus search field after checkout modal closes
            Platform.runLater(() -> searchField.requestFocus());

        } catch (java.io.IOException e) {
            e.printStackTrace();
            NotificationUtils.showError(com.pos.system.App.getBundle().getString("dialog.error"),
                    "Failed to open checkout modal.");
        }
    }

    private void updateTotal() {
        double total = cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
        totalLabel.setText(String.format("%,.2f MMK", total));
    }

    // Removed showAlert in favor of NotificationUtils
}
