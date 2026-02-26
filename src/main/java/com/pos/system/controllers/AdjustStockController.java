package com.pos.system.controllers;

import com.pos.system.models.Product;
import com.pos.system.services.StockAdjustmentService;
import com.pos.system.utils.NotificationUtils;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AdjustStockController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField qtyField;
    @FXML
    private TextArea reasonArea;
    @FXML
    private Label messageLabel;

    private Product product;
    private Runnable onSaveCallback;
    private StockAdjustmentService adjustmentService;

    public void setStockAdjustmentService(StockAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    public void setProductContext(Product product) {
        this.product = product;
        if (product != null) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            titleLabel.setText(String.format(b.getString("inventory.adjustStock.title"), product.getName()));
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        if (qtyField.getText().trim().isEmpty()) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("inventory.adjustStock.qtyRequired"));
            return;
        }

        try {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            int qtyChange = Integer.parseInt(qtyField.getText().trim());
            String reason = reasonArea.getText().trim();
            if (reason.isEmpty())
                reason = "Manual Adjustment";

            StockAdjustmentService service = this.adjustmentService != null ? this.adjustmentService
                    : new StockAdjustmentService();
            service.adjustStock(product.getId(), qtyChange, reason,
                    SessionManager.getInstance().getCurrentUser().getId());

            NotificationUtils.showSuccess(b.getString("inventory.adjustStock.successTitle"),
                    b.getString("inventory.adjustStock.successMsg"));

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();
        } catch (NumberFormatException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("inventory.adjustStock.invalidQty"));
        } catch (SQLException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            messageLabel.setText(b.getString("dialog.dbError") + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
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
}
