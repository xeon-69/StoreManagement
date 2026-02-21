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
        try (Connection connection = DatabaseManager.getInstance().getConnection()) {

            com.pos.system.services.InventoryService inventoryService = new com.pos.system.services.InventoryService();
            inventoryService.adjustStock(connection, productId, quantityChange, reason, null);

            logger.info("Stock adjusted for Product ID: {} by {}. Reason: {}", productId, quantityChange, reason);
        }
    }
}
