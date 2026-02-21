package com.pos.system.controllers;

import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.SalePayment;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CheckoutController {

    @FXML
    private Label subtotalLabel;
    @FXML
    private TextField discountField;
    @FXML
    private TextField taxField;
    @FXML
    private Label totalLabel;

    @FXML
    private ComboBox<String> paymentMethodCombo;
    @FXML
    private TextField paymentAmountField;
    @FXML
    private TableView<SalePayment> paymentsTable;
    @FXML
    private TableColumn<SalePayment, String> pmMethodCol;
    @FXML
    private TableColumn<SalePayment, Double> pmAmountCol;
    @FXML
    private TableColumn<SalePayment, Void> pmActionCol;

    @FXML
    private Label remainingLabel;
    @FXML
    private Label changeLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Button confirmButton;

    private double subtotal = 0.0;
    private double calculatedTotal = 0.0;
    private ObservableList<SaleItem> cartItems;
    private ObservableList<SalePayment> payments = FXCollections.observableArrayList();
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        paymentMethodCombo.getItems().addAll("CASH", "CARD", "MOBILE_MONEY", "QR");
        paymentMethodCombo.setValue("CASH");

        pmMethodCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaymentMethod()));
        pmAmountCol
                .setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());

        pmActionCol.setCellFactory(col -> new TableCell<SalePayment, Void>() {
            private final Button btn = new Button(com.pos.system.App.getBundle().getString("checkout.removeBtn"));
            {
                btn.getStyleClass().add("btn-danger");
                btn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                btn.setOnAction(e -> {
                    SalePayment payment = getTableView().getItems().get(getIndex());
                    payments.remove(payment);
                    updatePayments();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        paymentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        paymentsTable.setItems(payments);

        discountField.textProperty().addListener((obs, oldV, newV) -> recalculateTotal());
        taxField.textProperty().addListener((obs, oldV, newV) -> recalculateTotal());
    }

    public void setCheckoutData(ObservableList<SaleItem> cartItems, Runnable onSuccessCallback) {
        this.cartItems = cartItems;
        this.onSuccessCallback = onSuccessCallback;

        this.subtotal = cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
        subtotalLabel.setText(String.format("%.2f", subtotal));

        recalculateTotal();
    }

    private void recalculateTotal() {
        try {
            double discountPct = Double.parseDouble(discountField.getText().isEmpty() ? "0" : discountField.getText());
            double taxPct = Double.parseDouble(taxField.getText().isEmpty() ? "0" : taxField.getText());

            double discountAmt = subtotal * (discountPct / 100.0);
            double taxAmt = (subtotal - discountAmt) * (taxPct / 100.0);

            this.calculatedTotal = subtotal - discountAmt + taxAmt;
            totalLabel.setText(String.format("%.2f", calculatedTotal));

            updatePayments();
        } catch (NumberFormatException e) {
            // Ignore temporary broken input
        }
    }

    @FXML
    private void handleAddPayment() {
        try {
            double amount = Double.parseDouble(paymentAmountField.getText());
            if (amount <= 0)
                return;

            SalePayment payment = new SalePayment(0, 0, paymentMethodCombo.getValue(), amount,
                    java.time.LocalDateTime.now());
            payments.add(payment);
            paymentAmountField.clear();
            updatePayments();
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid amount.");
        }
    }

    private void updatePayments() {
        errorLabel.setText("");
        double totalPaid = payments.stream().mapToDouble(SalePayment::getAmount).sum();
        double remaining = calculatedTotal - totalPaid;

        if (remaining <= 0) {
            remainingLabel.setText("0.00");
            changeLabel.setText(String.format("%.2f", Math.abs(remaining)));
            confirmButton.setDisable(false);
        } else {
            remainingLabel.setText(String.format("%.2f", remaining));
            changeLabel.setText("0.00");
            confirmButton.setDisable(true);
        }

        // Suggest remaining balance in the input field
        if (remaining > 0) {
            paymentAmountField.setText(String.format("%.2f", remaining));
        }
    }

    @FXML
    private void handleConfirm() {
        double totalCost = cartItems.stream().mapToDouble(i -> i.getCostAtSale() * i.getQuantity()).sum();
        double discountPct = Double.parseDouble(discountField.getText().isEmpty() ? "0" : discountField.getText());
        double taxPct = Double.parseDouble(taxField.getText().isEmpty() ? "0" : taxField.getText());

        double discountAmt = subtotal * (discountPct / 100.0);
        double taxAmt = (subtotal - discountAmt) * (taxPct / 100.0);
        double totalProfit = calculatedTotal - totalCost - taxAmt; // crude profit calc

        // Pass data back or process checkout right here
        com.pos.system.models.User currentUser = com.pos.system.utils.SessionManager.getInstance().getCurrentUser();
        com.pos.system.models.Shift currentShift = com.pos.system.utils.SessionManager.getInstance().getCurrentShift();

        Sale sale = new Sale(0, currentUser.getId(), currentShift != null ? currentShift.getId() : null, subtotal,
                taxAmt, discountAmt, calculatedTotal, totalProfit, java.time.LocalDateTime.now());

        javafx.concurrent.Task<Void> checkoutTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                com.pos.system.services.CheckoutService checkoutService = new com.pos.system.services.CheckoutService();
                checkoutService.processCheckoutWithPayments(sale, cartItems, payments);
                return null;
            }
        };

        checkoutTask.setOnSucceeded(e -> {
            // Apply cash drawer transaction if cash is involved and there's a shift
            try {
                if (currentShift != null) {
                    double cashPaid = payments.stream().filter(pm -> "CASH".equals(pm.getPaymentMethod()))
                            .mapToDouble(SalePayment::getAmount).sum();
                    double change = Double.parseDouble(changeLabel.getText());
                    double netCash = cashPaid - change; // Add change if needed
                    if (netCash > 0) {
                        try (com.pos.system.dao.CashDrawerTransactionDAO trxDAO = new com.pos.system.dao.CashDrawerTransactionDAO()) {
                            trxDAO.create(new com.pos.system.models.CashDrawerTransaction(0, currentShift.getId(),
                                    currentUser.getId(), netCash, "CASH_SALE", "Sale ID " + sale.getId(),
                                    java.time.LocalDateTime.now()));
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(); // Non-fatal for the sale
            }

            // Print receipt
            double amountTendered = payments.stream().mapToDouble(SalePayment::getAmount).sum();
            double changeDue = Double.parseDouble(changeLabel.getText());
            com.pos.system.services.PrinterService printer = new com.pos.system.services.PrinterService();
            printer.printReceipt(sale, cartItems, amountTendered, changeDue);

            if (onSuccessCallback != null)
                onSuccessCallback.run();
            closeModal();
        });

        checkoutTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            errorLabel.setText("Checkout Failed: " + e.getSource().getException().getMessage());
        });

        new Thread(checkoutTask).start();
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }
}
