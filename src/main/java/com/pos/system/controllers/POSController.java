package com.pos.system.controllers;

import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import com.pos.system.models.SaleItem;
import com.pos.system.utils.NotificationUtils;
import com.pos.system.viewmodels.ProductCatalogViewModel;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.geometry.Bounds;

import org.controlsfx.control.GridView;
import org.controlsfx.control.GridCell;

import com.pos.system.services.ImageCacheService;

public class POSController {

    @FXML
    private TextField searchField;
    @FXML
    private GridView<Product> productGridView;
    @FXML
    private ComboBox<com.pos.system.models.Category> categoryFilter;
    @FXML
    private Pagination pagination;

    private static final int ITEMS_PER_PAGE = 9;

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

    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private ProductCatalogViewModel catalogViewModel;
    private String currencySymbol;
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
        currencySymbol = com.pos.system.utils.SettingsManager.getInstance().getCurrencySymbol();

        setupCategoryFilter();

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

        // Setup Pagination and Bind List Updates
        setupPagination();

        // Setup Virtualized Cell Factory
        productGridView.setCellFactory(gridView -> new GridCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setOnMouseClicked(null);
                    getStyleClass().remove("product-card-active");
                } else {
                    VBox tile = createProductTile(item);
                    setGraphic(tile);

                    // Use standard Action click handling
                    setOnMouseClicked(e -> {
                        if (item != null && e.getClickCount() == 1) {
                            handleProductClick(item, tile);
                        }
                    });
                }
            }
        });

        setupCartTable();
        // Ensure catalog loads
        catalogViewModel.loadProducts();

        // Auto-focus search field on load
        Platform.runLater(() -> searchField.requestFocus());

        // Refresh grid when cart changes to update active styles
        cartItems.addListener((ListChangeListener<SaleItem>) c -> {
            // Re-render the current page of the product grid to update active styles
            showPage(pagination.getCurrentPageIndex());
        });
    }

    private void setupCartTable() {
        // Cart Table Setup - Receipt Style
        cartTable.getStyleClass().add("cart-table");
        cNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        setupQuantityColumn();
        cQtyCol.setMinWidth(120);
        cQtyCol.setPrefWidth(120);

        cTotalCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotal()).asObject());

        // Receipt Styling preference
        cartTable.setPlaceholder(new Label(com.pos.system.App.getBundle().getString("pos.cart.placeholder")));

        cartTable.setItems(cartItems);
    }

    private void handleProductClick(Product p, Node tileNode) {
        if (p == null || tileNode == null)
            return;
        if (isProductInCart(p)) {
            playFlyAnimation(tileNode, false);
            removeFromCart(p);
        } else {
            playFlyAnimation(tileNode, true);
            updateCartQuantity(p, 1);
        }
    }

    /**
     * Handles barcode scanner input: looks up product by barcode,
     * adds it to cart (or increments qty), clears field, and re-focuses.
     */
    private void handleBarcodeInput(String barcode) {
        java.util.ResourceBundle b = com.pos.system.App.getBundle();

        Task<Product> task = new Task<>() {
            @Override
            protected Product call() throws Exception {
                try (ProductDAO productDAO = new ProductDAO()) {
                    return productDAO.getProductByBarcode(barcode);
                }
            }
        };

        task.setOnSucceeded(e -> {
            Product product = task.getValue();
            if (product != null) {
                updateCartQuantity(product, 1);
            } else {
                NotificationUtils.showWarning(b.getString("dialog.warning"),
                        String.format(b.getString("pos.barcode.notFound"), barcode));
            }
            // Clear and re-focus for next scan
            searchField.clear();
            searchField.requestFocus();
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            NotificationUtils.showError(b.getString("dialog.error"), b.getString("pos.barcode.error"));
            // Clear and re-focus for next scan
            searchField.clear();
            searchField.requestFocus();
        });

        new Thread(task).start();
    }

    private VBox createProductTile(Product p) {
        VBox card = new VBox(5);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(150);
        card.setPrefHeight(230);
        card.setAlignment(Pos.CENTER);

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(80, 80);

        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        Label placeholder = new Label(b.getString("pos.noImage"));
        placeholder.setStyle(
                "-fx-text-fill: #bdc3c7; -fx-border-color: #bdc3c7; -fx-border-style: dashed; -fx-padding: 20;");
        imageContainer.getChildren().add(placeholder);

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setFitHeight(80);
        imageView.setFitWidth(80);
        imageView.setPreserveRatio(true);

        if (p.getImageData() != null && p.getImageData().length > 0) {
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(p.getImageData());
                imageView.setImage(new javafx.scene.image.Image(bis));
                imageContainer.getChildren().add(imageView);
                placeholder.setVisible(false);
            } catch (Exception e) {
                // Ignore image load error, placeholder remains
            }
        } else {
            // Lazy load via ImageCacheService
            Image cached = ImageCacheService.getInstance().getCachedImage(p.getId());
            if (cached != null) {
                imageView.setImage(cached);
                imageContainer.getChildren().add(imageView);
                placeholder.setVisible(false);
            } else {
                ImageCacheService.getInstance().loadImage(p.getId(), img -> {
                    imageView.setImage(img);
                    if (!imageContainer.getChildren().contains(imageView)) {
                        imageContainer.getChildren().add(imageView);
                        placeholder.setVisible(false);
                    }
                });
            }
        }

        card.getChildren().add(imageContainer);

        Label nameLbl = new Label(p.getName());
        nameLbl.getStyleClass().add("product-name");
        nameLbl.setWrapText(true);

        Label priceLbl = new Label(String.format("%,.0f %s", p.getSellingPrice(), currencySymbol));
        priceLbl.getStyleClass().add("product-price");

        Label stockLbl = new Label(com.pos.system.App.getBundle().getString("pos.stockLabel") + p.getStock());
        stockLbl.getStyleClass().add("stock-label");
        if (p.getStock() < 10) {
            stockLbl.getStyleClass().add("stock-low");
        }

        if (isProductInCart(p)) {
            card.getStyleClass().add("product-card-active");
        }

        card.getChildren().addAll(nameLbl, priceLbl, stockLbl);
        return card;
    }

    private boolean isProductInCart(Product p) {
        return cartItems.stream().anyMatch(item -> item.getProductId() == p.getId());
    }

    private void removeFromCart(Product p) {
        cartItems.removeIf(item -> item.getProductId() == p.getId());
        updateTotal();
    }

    private void playFlyAnimation(Node startNode, boolean toCart) {
        try {
            if (startNode.getScene() == null)
                return;

            Pane root = (Pane) startNode.getScene().getRoot();
            Bounds startBounds = startNode.localToScene(startNode.getBoundsInLocal());
            Bounds targetBounds = cartTable.localToScene(cartTable.getBoundsInLocal());

            // Create flyer silhouette (rectangle matching card size)
            Rectangle flyer = new Rectangle(startBounds.getWidth(), startBounds.getHeight());
            flyer.getStyleClass().add("flyer-node");
            flyer.setArcWidth(20); // 2 * radius = 20
            flyer.setArcHeight(20);
            flyer.setManaged(false);
            root.getChildren().add(flyer);

            // Final destination in the middle of the cart
            double targetX = targetBounds.getMinX() + targetBounds.getWidth() / 2 - flyer.getWidth() / 2;
            double targetY = targetBounds.getMinY() + targetBounds.getHeight() / 2 - flyer.getHeight() / 2;

            if (toCart) {
                flyer.setTranslateX(startBounds.getMinX());
                flyer.setTranslateY(startBounds.getMinY());
            } else {
                flyer.setTranslateX(targetX);
                flyer.setTranslateY(targetY);
                // Inverse start state
                flyer.setScaleX(0.2);
                flyer.setScaleY(0.2);
                flyer.setOpacity(0);
            }

            // Shrinking and flying transition
            TranslateTransition translate = new TranslateTransition(Duration.millis(500), flyer);
            translate.setToX(toCart ? targetX : startBounds.getMinX());
            translate.setToY(toCart ? targetY : startBounds.getMinY());

            ScaleTransition scale = new ScaleTransition(Duration.millis(500), flyer);
            scale.setToX(toCart ? 0.2 : 1.0);
            scale.setToY(toCart ? 0.2 : 1.0);

            FadeTransition fade = new FadeTransition(Duration.millis(500), flyer);
            fade.setFromValue(toCart ? 0.6 : 0.0);
            fade.setToValue(toCart ? 0.0 : 0.6);

            ParallelTransition pt = new ParallelTransition(translate, scale, fade);
            pt.setOnFinished(e -> root.getChildren().remove(flyer));
            pt.play();

        } catch (Exception e) {
            System.err.println("Animation failed: " + e.getMessage());
        }
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
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                if (newQty > p.getStock()) {
                    NotificationUtils.showWarning(b.getString("dialog.warning"),
                            b.getString("pos.notEnoughStock"));
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
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                NotificationUtils.showWarning(b.getString("dialog.warning"),
                        b.getString("pos.outOfStock"));
                return;
            }
            SaleItem item = new SaleItem(
                    0, 0, p.getId(), p.getName(), 1,
                    p.getSellingPrice(), p.getCostPrice());
            cartItems.add(item);
        }

        updateTotal();
    }

    private void setupQuantityColumn() {
        cQtyCol.setCellFactory(col -> new TableCell<SaleItem, Integer>() {
            private final Button minusBtn = new Button("-");
            private final Button plusBtn = new Button("+");
            private final TextField qtyField = new TextField();
            private final HBox container = new HBox(5, plusBtn, qtyField, minusBtn);

            {
                minusBtn.getStyleClass().addAll("stepper-btn", "stepper-btn-minus");
                plusBtn.getStyleClass().addAll("stepper-btn", "stepper-btn-plus");
                qtyField.getStyleClass().add("cart-qty-field");
                container.getStyleClass().add("cart-stepper");
                container.setAlignment(Pos.CENTER);

                minusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    Product p = findProductById(item.getProductId());
                    if (p != null)
                        updateCartQuantity(p, -1);
                });

                plusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    Product p = findProductById(item.getProductId());
                    if (p != null)
                        updateCartQuantity(p, 1);
                });

                qtyField.setOnAction(e -> handleQtyFieldUpdate());
                qtyField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal)
                        handleQtyFieldUpdate();
                });
            }

            private void handleQtyFieldUpdate() {
                SaleItem item = getTableRow().getItem();
                if (item == null)
                    return;
                try {
                    int newQty = Integer.parseInt(qtyField.getText());
                    Product p = findProductById(item.getProductId());
                    if (p != null) {
                        int change = newQty - item.getQuantity();
                        if (change != 0)
                            updateCartQuantity(p, change);
                    }
                } catch (NumberFormatException ex) {
                    qtyField.setText(String.valueOf(item.getQuantity()));
                }
            }

            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    SaleItem item = getTableRow().getItem();
                    if (item == null) {
                        setGraphic(null);
                        return;
                    }
                    qtyField.setText(String.valueOf(item.getQuantity()));

                    // Safe binding to row hover
                    minusBtn.visibleProperty().unbind();
                    plusBtn.visibleProperty().unbind();
                    minusBtn.visibleProperty().bind(getTableRow().hoverProperty());
                    plusBtn.visibleProperty().bind(getTableRow().hoverProperty());

                    setGraphic(container);
                    setText(null);
                }
            }
        });
    }

    private Product findProductById(int id) {
        return catalogViewModel.getAllProducts().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
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
        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        if (cartItems.isEmpty()) {
            NotificationUtils.showWarning(b.getString("dialog.warning"), b.getString("pos.cart.emptyMsg"));
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/checkout_modal.fxml"));
            loader.setResources(b);
            javafx.scene.Parent root = loader.load();

            CheckoutController controller = loader.getController();
            controller.setCheckoutData(cartItems, () -> {
                Platform.runLater(() -> {
                    NotificationUtils.showSuccess(b.getString("dialog.success"),
                            b.getString("pos.transaction.completed"));
                    cartItems.clear();
                    updateTotal();
                    catalogViewModel.loadProducts();
                });
            });

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(totalLabel.getScene().getWindow());
            stage.setTitle(b.getString("checkout.title"));
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            // Re-focus search field after checkout modal closes
            Platform.runLater(() -> searchField.requestFocus());

        } catch (java.io.IOException e) {
            e.printStackTrace();
            NotificationUtils.showError(b.getString("dialog.error"),
                    b.getString("pos.checkout.failed"));
        }
    }

    private void setupPagination() {
        catalogViewModel.getFilteredProducts().addListener((ListChangeListener<Product>) c -> {
            updatePagination();
        });

        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            showPage(newIndex.intValue());
        });

        updatePagination();
    }

    private void updatePagination() {
        ObservableList<Product> allProducts = catalogViewModel.getFilteredProducts();
        int pageCount = (int) Math.ceil((double) allProducts.size() / ITEMS_PER_PAGE);
        if (pageCount <= 0)
            pageCount = 1;

        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        showPage(0);
    }

    private void showPage(int pageIndex) {
        ObservableList<Product> allProducts = catalogViewModel.getFilteredProducts();
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allProducts.size());

        if (allProducts.isEmpty() || fromIndex < 0 || fromIndex >= allProducts.size()) {
            productGridView.setItems(FXCollections.emptyObservableList());
        } else {
            productGridView.setItems(FXCollections.observableArrayList(allProducts.subList(fromIndex, toIndex)));
        }
    }

    private void setupCategoryFilter() {
        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        // Dummy "All Categories" category
        com.pos.system.models.Category allCat = new com.pos.system.models.Category(0, b.getString("pos.allCategories"),
                "");

        Task<ObservableList<com.pos.system.models.Category>> task = new Task<>() {
            @Override
            protected ObservableList<com.pos.system.models.Category> call() throws Exception {
                try (com.pos.system.dao.CategoryDAO categoryDAO = new com.pos.system.dao.CategoryDAO()) {
                    java.util.List<com.pos.system.models.Category> categories = categoryDAO.getAllCategories();
                    ObservableList<com.pos.system.models.Category> comboItems = FXCollections.observableArrayList();
                    comboItems.add(allCat);
                    comboItems.addAll(categories);
                    return comboItems;
                }
            }
        };

        task.setOnSucceeded(e -> {
            categoryFilter.setItems(task.getValue());
            categoryFilter.getSelectionModel().select(allCat);
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            categoryFilter.setItems(FXCollections.observableArrayList(allCat));
            categoryFilter.getSelectionModel().select(allCat);
        });

        new Thread(task).start();

        // Listener for filter
        categoryFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                catalogViewModel.filterByCategory(newVal.getId());
                updatePagination(); // Reset pagination on filter change
            }
        });
    }

    private void updateTotal() {
        double subtotal = cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
        totalLabel.setText(String.format("%,.2f %s", subtotal, currencySymbol));
    }
}
