package com.pos.system.dao;

import com.pos.system.models.InventoryTransaction;
import com.pos.system.models.Category;
import com.pos.system.models.Product;
import com.pos.system.models.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryTransactionDAOTest extends BaseDAOTest {

    private InventoryTransactionDAO transactionDAO;
    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;
    private int testProductId;

    @BeforeEach
    public void setUp() throws SQLException {
        transactionDAO = new InventoryTransactionDAO(connection);
        productDAO = new ProductDAO(connection);
        categoryDAO = new CategoryDAO(connection);

        categoryDAO.addCategory(new Category(0, "Cat1", ""));
        int catId = categoryDAO.getAllCategories().get(0).getId();

        productDAO.addProduct(new Product(0, "INV-PROD", "Test Inv", catId, "Cat1", 5.0, 10.0, 0, null));
        testProductId = productDAO.getProductByBarcode("INV-PROD").getId();
    }

    @Test
    public void testInsertAndGetTransactions() throws SQLException {
        // Arrange
        InventoryTransaction tx1 = new InventoryTransaction();
        tx1.setProductId(testProductId);
        tx1.setQuantityChange(50);
        tx1.setTransactionType(TransactionType.PURCHASE);
        tx1.setReferenceId("PO-123");

        InventoryTransaction tx2 = new InventoryTransaction();
        tx2.setProductId(testProductId);
        tx2.setQuantityChange(-10);
        tx2.setTransactionType(TransactionType.SALE);
        tx2.setReferenceId("SALE-456");

        // Act
        transactionDAO.insertTransaction(tx1);
        transactionDAO.insertTransaction(tx2);

        List<InventoryTransaction> history = transactionDAO.getTransactionHistory(testProductId);

        // Assert
        assertEquals(2, history.size());
        // Ordered by created_at DESC, so tx2 should be first if inserted later, or we
        // just check contents
        assertTrue(history.stream()
                .anyMatch(t -> t.getQuantityChange() == 50 && t.getTransactionType() == TransactionType.PURCHASE));
        assertTrue(history.stream()
                .anyMatch(t -> t.getQuantityChange() == -10 && t.getTransactionType() == TransactionType.SALE));
    }

    @Test
    public void testCalculateStockLevel() throws SQLException {
        // Arrange
        InventoryTransaction tx1 = new InventoryTransaction();
        tx1.setProductId(testProductId);
        tx1.setQuantityChange(100);
        tx1.setTransactionType(TransactionType.PURCHASE);

        InventoryTransaction tx2 = new InventoryTransaction();
        tx2.setProductId(testProductId);
        tx2.setQuantityChange(-25);
        tx2.setTransactionType(TransactionType.SALE);

        InventoryTransaction tx3 = new InventoryTransaction();
        tx3.setProductId(testProductId);
        tx3.setQuantityChange(-5);
        tx3.setTransactionType(TransactionType.RETURN);

        transactionDAO.insertTransaction(tx1);
        transactionDAO.insertTransaction(tx2);
        transactionDAO.insertTransaction(tx3);

        // Act
        int stockLevel = transactionDAO.calculateStockLevel(testProductId);

        // Assert
        assertEquals(70, stockLevel); // 100 - 25 - 5
    }

    @Test
    public void testCalculateStockLevelEmpty() throws SQLException {
        // Act
        int stockLevel = transactionDAO.calculateStockLevel(testProductId);

        // Assert
        assertEquals(0, stockLevel);
    }
}
