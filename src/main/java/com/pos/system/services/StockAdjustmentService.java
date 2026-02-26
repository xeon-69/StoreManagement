package com.pos.system.services;

import com.pos.system.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class StockAdjustmentService {
    private static final Logger logger = LoggerFactory.getLogger(StockAdjustmentService.class);

    private final InventoryService inventoryService;

    public StockAdjustmentService() {
        this.inventoryService = new InventoryService();
    }

    // For Dependency Injection in tests
    public StockAdjustmentService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void adjustStock(int productId, int quantityChange, String reason, Integer createdBy) throws SQLException {
        try (Connection connection = DatabaseManager.getInstance().getConnection()) {
            inventoryService.adjustStock(connection, productId, quantityChange, reason, createdBy);
            logger.info("Stock adjusted for Product ID: {} by {}. Reason: {}", productId, quantityChange, reason);
        }
    }
}
