package com.pos.system.services;

import com.pos.system.dao.BatchDAO;
import com.pos.system.dao.InventoryTransactionDAO;
import com.pos.system.models.Batch;
import com.pos.system.models.InventoryTransaction;
import com.pos.system.models.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private BatchDAO mockBatchDAO;

    @Mock
    private InventoryTransactionDAO mockTransactionDAO;

    @Mock
    private PreparedStatement mockPreparedStatement;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService() {
            @Override
            protected BatchDAO getBatchDAO(Connection connection) {
                return mockBatchDAO;
            }

            @Override
            protected InventoryTransactionDAO getInventoryTransactionDAO(Connection connection) {
                return mockTransactionDAO;
            }
        };
    }

    @Test
    void testAddStock_ValidInput_AddsBatchAndTransaction() throws SQLException {
        // Arrange
        int productId = 101;
        int quantity = 50;
        double costPrice = 10.0;
        LocalDateTime expiryDate = LocalDateTime.now().plusMonths(6);
        String reference = "PO-123";
        Integer createdBy = 1;

        when(mockTransactionDAO.calculateStockLevel(productId)).thenReturn(100);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        inventoryService.addStock(mockConnection, productId, quantity, costPrice, expiryDate, reference, createdBy);

        // Assert
        ArgumentCaptor<Batch> batchCaptor = ArgumentCaptor.forClass(Batch.class);
        verify(mockBatchDAO).insertBatch(batchCaptor.capture());
        assertEquals(quantity, batchCaptor.getValue().getRemainingQuantity());
        assertEquals(costPrice, batchCaptor.getValue().getCostPrice());

        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(mockTransactionDAO).insertTransaction(txCaptor.capture());
        assertEquals(quantity, txCaptor.getValue().getQuantityChange());
        assertEquals(TransactionType.PURCHASE, txCaptor.getValue().getTransactionType());

        // verify cache updated
        verify(mockTransactionDAO).calculateStockLevel(productId);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testDeductStock_SufficientStock_DeductsFromBatches() throws SQLException {
        // Arrange
        int productId = 202;
        int quantityToDeduct = 15;
        String reference = "SALE-456";

        Batch batch1 = new Batch();
        batch1.setId(1);
        batch1.setProductId(productId);
        batch1.setRemainingQuantity(10);

        Batch batch2 = new Batch();
        batch2.setId(2);
        batch2.setProductId(productId);
        batch2.setRemainingQuantity(10);

        // Return a list of available batches
        when(mockBatchDAO.getAvailableBatches(productId)).thenReturn(Arrays.asList(batch1, batch2));
        when(mockTransactionDAO.calculateStockLevel(productId)).thenReturn(5);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        inventoryService.deductStock(mockConnection, productId, quantityToDeduct, TransactionType.SALE, reference, 1);

        // Assert
        // Should fully drain batch 1
        verify(mockBatchDAO).updateRemainingQuantity(1, 0);
        // Should partially drain batch 2
        verify(mockBatchDAO).updateRemainingQuantity(2, 5);

        // verify negatively signed transactions logged for both batch portions
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(mockTransactionDAO, times(2)).insertTransaction(txCaptor.capture());

        assertEquals(-10, txCaptor.getAllValues().get(0).getQuantityChange());
        assertEquals(-5, txCaptor.getAllValues().get(1).getQuantityChange());
    }

    @Test
    void testDeductStock_InsufficientStock_ThrowsException() throws SQLException {
        // Arrange
        int productId = 303;
        Batch batch1 = new Batch();
        batch1.setId(1);
        batch1.setProductId(productId);
        batch1.setRemainingQuantity(5);

        when(mockBatchDAO.getAvailableBatches(productId)).thenReturn(Arrays.asList(batch1));

        // Act & Assert (Try to deduct 15, only 5 available)
        assertThrows(SQLException.class, () -> {
            inventoryService.deductStock(mockConnection, productId, 15, TransactionType.SALE, "SALE-999", 1);
        });
    }
}
