package com.pos.system.controllers;

import com.pos.system.models.Product;
import com.pos.system.services.StockAdjustmentService;
import com.pos.system.utils.NotificationUtils;
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

    public void setProductContext(Product product) {
        this.product = product;
        if (product != null) {
            titleLabel.setText("Adjust Stock: " + product.getName());
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        if (qtyField.getText().trim().isEmpty()) {
            messageLabel.setText("Operation failed: Quantity is required.");
            return;
        }

        try {
            int qtyChange = Integer.parseInt(qtyField.getText().trim());
            String reason = reasonArea.getText().trim();
            if (reason.isEmpty())
                reason = "Manual Adjustment";

            StockAdjustmentService service = new StockAdjustmentService();
            service.adjustStock(product.getId(), qtyChange, reason);

            NotificationUtils.showSuccess("Stock Adjusted", "Ledger updated successfully.");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            closeWindow();
        } catch (NumberFormatException e) {
            messageLabel.setText("Operation failed: Quantity must be a valid number.");
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
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
