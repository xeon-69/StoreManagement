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

import java.util.List;

public class InventoryController {

    // For Dependency Injection in Tests only
    private ProductDAO injectedProductDAO;

    public void setProductDAO(ProductDAO productDAO) {
        this.injectedProductDAO = productDAO;
    }

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
    private TableColumn<Product, Void> actionCol;
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

        // Highlight low stock cells with red background
        stockCol.setCellFactory(col -> new javafx.scene.control.TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    if (stock <= 10) {
                        setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        setupActionColumn();

        // Auto-resize columns
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // No longer create a stored productDAO here - use try-with-resources in tasks

        loadProducts();
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<Product, Void>() {
            private final javafx.scene.control.Button editBtn = new javafx.scene.control.Button();
            private final javafx.scene.control.Button adjustBtn = new javafx.scene.control.Button();
            private final javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button();
            private final javafx.scene.control.Button ledgerBtn = new javafx.scene.control.Button();
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(8, ledgerBtn, adjustBtn, editBtn,
                    deleteBtn);

            {
                org.kordamp.ikonli.javafx.FontIcon ledgerIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-book");
                ledgerIcon.setIconColor(javafx.scene.paint.Color.BLACK);
                ledgerBtn.setGraphic(ledgerIcon);
                ledgerBtn.getStyleClass().add("btn-secondary");
                ledgerBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleViewLedger(product);
                });

                org.kordamp.ikonli.javafx.FontIcon adjustIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-sliders-h");
                adjustIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                adjustBtn.setGraphic(adjustIcon);
                adjustBtn.getStyleClass().add("btn-success");
                adjustBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleAdjustStock(product);
                });

                org.kordamp.ikonli.javafx.FontIcon editIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-edit");
                editIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                editBtn.setGraphic(editIcon);
                editBtn.getStyleClass().add("btn-primary");
                editBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });

                org.kordamp.ikonli.javafx.FontIcon deleteIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-trash-alt");
                deleteIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });

                pane.setStyle("-fx-alignment: CENTER;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void handleDeleteProduct(Product selectedProduct) {
        if (selectedProduct == null) {
            return;
        }

        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        boolean confirm = com.pos.system.utils.NotificationUtils.showConfirmation(
                b.getString("inventory.delete.title"),
                String.format(b.getString("inventory.delete.confirmMsg"), selectedProduct.getName()));

        if (confirm) {

            javafx.concurrent.Task<Void> deleteTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    try (ProductDAO productDAO = new ProductDAO()) {
                        productDAO.deleteProduct(selectedProduct.getId());
                    }
                    return null;
                }
            };

            deleteTask.setOnSucceeded(e -> {
                loadProducts();
            });

            deleteTask.setOnFailed(e -> {
                e.getSource().getException().printStackTrace();
                com.pos.system.utils.NotificationUtils.showError(b.getString("dialog.dbError"),
                        String.format(b.getString("inventory.delete.error"),
                                e.getSource().getException().getMessage()));
            });

            new Thread(deleteTask).start();
        }
    }

    private void loadProducts() {
        javafx.concurrent.Task<List<Product>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                try (ProductDAO dao = injectedProductDAO != null ? null : new ProductDAO()) {
                    ProductDAO activeDAO = injectedProductDAO != null ? injectedProductDAO : dao;
                    return activeDAO.getAllProducts();
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                    return java.util.Collections.emptyList();
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            ObservableList<Product> observableProducts = FXCollections.observableArrayList(loadTask.getValue());
            productTable.setItems(observableProducts);
        });

        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            // In a real app, show an Alert
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadProducts();
            return;
        }

        String lowerCaseQuery = query.toLowerCase();

        javafx.concurrent.Task<List<Product>> searchTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                try (ProductDAO dao = injectedProductDAO != null ? null : new ProductDAO()) {
                    ProductDAO activeDAO = injectedProductDAO != null ? injectedProductDAO : dao;
                    List<Product> allProducts = activeDAO.getAllProducts();
                    return allProducts.stream()
                            .filter(p -> p.getName().toLowerCase().contains(lowerCaseQuery) ||
                                    (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(lowerCaseQuery)))
                            .toList();
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                    return java.util.Collections.emptyList();
                }
            }
        };

        searchTask.setOnSucceeded(e -> {
            ObservableList<Product> observableProducts = FXCollections.observableArrayList(searchTask.getValue());
            productTable.setItems(observableProducts);
        });

        searchTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
        });

        new Thread(searchTask).start();
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
    }

    @FXML
    private void handleExpireStock() {
        javafx.concurrent.Task<Void> expireTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                try (java.sql.Connection conn = com.pos.system.database.DatabaseManager.getInstance().getConnection()) {
                    com.pos.system.services.InventoryService invService = new com.pos.system.services.InventoryService();
                    invService.expireItems(conn, null);
                }
                return null;
            }
        };

        expireTask.setOnSucceeded(e -> {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            com.pos.system.utils.NotificationUtils.showInfo(b.getString("inventory.expiry.success.title"),
                    b.getString("inventory.expiry.success.msg"));
            loadProducts();
        });

        expireTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            com.pos.system.utils.NotificationUtils.showError(b.getString("inventory.expiry.fail.title"),
                    e.getSource().getException().getMessage());
        });

        new Thread(expireTask).start();
    }

    private void handleViewLedger(Product selectedProduct) {
        if (selectedProduct == null) {
            return;
        }

        javafx.concurrent.Task<List<com.pos.system.models.InventoryTransaction>> ledgerTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<com.pos.system.models.InventoryTransaction> call() throws Exception {
                try (java.sql.Connection conn = com.pos.system.database.DatabaseManager.getInstance().getConnection()) {
                    com.pos.system.services.InventoryService invService = new com.pos.system.services.InventoryService();
                    return invService.getTransactionHistory(conn, selectedProduct.getId());
                }
            }
        };

        ledgerTask.setOnSucceeded(e -> {
            List<com.pos.system.models.InventoryTransaction> history = ledgerTask.getValue();

            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle(b.getString("inventory.ledger.title") + " - "
                    + selectedProduct.getName());
            dialog.setHeaderText(b.getString("inventory.ledger.history"));

            javafx.scene.control.TableView<com.pos.system.models.InventoryTransaction> table = new javafx.scene.control.TableView<>();

            TableColumn<com.pos.system.models.InventoryTransaction, java.time.LocalDateTime> dateCol = new TableColumn<>(
                    com.pos.system.App.getBundle().getString("inventory.ledger.date"));
            dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            dateCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                        .ofPattern("MMM dd, yyyy hh:mm a");

                @Override
                protected void updateItem(java.time.LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            });
            dateCol.setPrefWidth(180);
            dateCol.setMinWidth(150);

            TableColumn<com.pos.system.models.InventoryTransaction, String> typeCol = new TableColumn<>(
                    com.pos.system.App.getBundle().getString("inventory.ledger.type"));
            typeCol.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
            typeCol.setPrefWidth(140);
            typeCol.setMinWidth(120);

            TableColumn<com.pos.system.models.InventoryTransaction, Integer> qtyCol = new TableColumn<>(
                    com.pos.system.App.getBundle().getString("inventory.ledger.qtyChange"));
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantityChange"));
            qtyCol.setPrefWidth(180);
            qtyCol.setMinWidth(150);

            TableColumn<com.pos.system.models.InventoryTransaction, String> refCol = new TableColumn<>(
                    com.pos.system.App.getBundle().getString("inventory.ledger.reference"));
            refCol.setCellValueFactory(new PropertyValueFactory<>("referenceId"));
            refCol.setPrefWidth(220);
            refCol.setMinWidth(150);

            TableColumn<com.pos.system.models.InventoryTransaction, Integer> batchCol = new TableColumn<>(
                    com.pos.system.App.getBundle().getString("inventory.ledger.batchId"));
            batchCol.setCellValueFactory(new PropertyValueFactory<>("batchId"));
            batchCol.setPrefWidth(100);
            batchCol.setMinWidth(80);

            table.getColumns().addAll(dateCol, typeCol, qtyCol, refCol, batchCol);
            table.setItems(FXCollections.observableArrayList(history));
            table.setPrefWidth(900);
            table.setPrefHeight(450);

            dialog.getDialogPane().setContent(table);
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
            dialog.showAndWait();
        });

        ledgerTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            com.pos.system.utils.NotificationUtils.showError(b.getString("inventory.ledger.error"),
                    e.getSource().getException().getMessage());
        });

        new Thread(ledgerTask).start();
    }

    private void handleEditProduct(Product selectedProduct) {
        if (selectedProduct == null) {
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/add_product.fxml"));
            loader.setResources(com.pos.system.App.getBundle());
            javafx.scene.Parent root = loader.load();

            AddProductController controller = loader.getController();
            controller.setProductToEdit(selectedProduct); // Pre-fill data
            controller.setOnSaveCallback(this::loadProducts);

            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(b.getString("product.editTitle"));
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
            loader.setResources(com.pos.system.App.getBundle());
            javafx.scene.Parent root = loader.load();

            AddProductController controller = loader.getController();
            controller.setOnSaveCallback(this::loadProducts);

            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(b.getString("product.addTitle"));
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAdjustStock(Product selectedProduct) {
        if (selectedProduct == null)
            return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/adjust_stock.fxml"));
            loader.setResources(com.pos.system.App.getBundle());
            javafx.scene.Parent root = loader.load();

            AdjustStockController controller = loader.getController();
            controller.setProductContext(selectedProduct);
            controller.setOnSaveCallback(this::handleRefresh); // Assuming there's a refresh or loadProducts

            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(b.getString("stock.adjust.title").replace("%s", selectedProduct.getName()));
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (java.io.IOException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            com.pos.system.utils.NotificationUtils.showError(b.getString("dialog.error"),
                    b.getString("inventory.adjust.error"));
        }
    }
}
