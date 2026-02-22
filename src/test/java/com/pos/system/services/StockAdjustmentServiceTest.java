package com.pos.system.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;

@ExtendWith(MockitoExtension.class)
public class StockAdjustmentServiceTest {

    @Mock
    private InventoryService inventoryServiceMock;

    private StockAdjustmentService stockAdjustmentService;

    @BeforeEach
    void setUp() {
        stockAdjustmentService = new StockAdjustmentService(inventoryServiceMock);
    }

    @Test
    void testAdjustStockDelegatesToInventoryService() throws SQLException {
        // Arrange
        int productId = 10;
        int quantityChange = -5;
        String reason = "Damaged Goods";

        // Act
        stockAdjustmentService.adjustStock(productId, quantityChange, reason);

        // Assert
        // We verify that the underlying inventoryService.adjustStock was called.
        // connection is handled internally, so we use any(Connection.class).
        verify(inventoryServiceMock, times(1)).adjustStock(
                any(Connection.class),
                eq(productId),
                eq(quantityChange),
                eq(reason),
                isNull());
    }
}
