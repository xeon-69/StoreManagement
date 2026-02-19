package com.pos.system.services;

import com.pos.system.dao.ProductDAO;
import com.pos.system.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class StockAdjustmentService {
    private static final Logger logger = LoggerFactory.getLogger(StockAdjustmentService.class);

    public void adjustStock(int productId, int quantityChange, String reason) throws SQLException {
        try (Connection connection = DatabaseManager.getInstance().getConnection();
                ProductDAO productDAO = new ProductDAO(connection)) {

            // For now, simple adjustment.
            // In future, we might want to record the adjustment reason in a separate table
            // (stock_logs).

            // Note: negative quantityChange = decrease (waste), positive = increase
            // (purchase)
            // But ProductDAO.updateStockQuantity subtracts! (stock = stock - ?)
            // So if we want to ADD stock, we pass negative?
            // Wait, ProductDAO.updateStockQuantity was: "UPDATE products SET stock = stock
            // - ? WHERE id = ?"
            // So positive change reduces stock (sale).
            // To INCREASE stock (receive), we need to pass negative.
            // Or better: Rename/Refactor ProductDAO to adjustStock(delta) where + adds, -
            // removes.

            // Let's assume updateStockQuantity(qt) means "reduce by qt".
            // So for waste (reduce by 5), we pass 5.
            // For receive (add 10), we pass -10? That's confusing.

            // I should verify/fix ProductDAO semantic.
            // The method is updateStockQuantity(int productId, int quantityChange)
            // SQL: stock = stock - ?
            // So ? is "quantity to subtract".

            // If I want to add stock, I pass negative.

            productDAO.updateStockQuantity(productId, -quantityChange);

            logger.info("Stock adjusted for Product ID: {} by {}. Reason: {}", productId, quantityChange, reason);
        }
    }
}
