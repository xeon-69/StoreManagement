package com.pos.system.dao;

import com.pos.system.models.Batch;
import com.pos.system.models.Category;
import com.pos.system.models.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BatchDAOTest extends BaseDAOTest {

    private BatchDAO batchDAO;
    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;
    private int testProductId;

    @BeforeEach
    public void setUp() throws SQLException {
        batchDAO = new BatchDAO(connection);
        productDAO = new ProductDAO(connection);
        categoryDAO = new CategoryDAO(connection);

        // Preload a category and product for batches
        categoryDAO.addCategory(new Category(0, "Cat1", ""));
        int categoryId = categoryDAO.getAllCategories().get(0).getId();

        productDAO.addProduct(new Product(0, "BPROD", "Batch Product", categoryId, "Cat1", 10.0, 20.0, 100, null));
        testProductId = productDAO.getProductByBarcode("BPROD").getId();
    }

    @Test
    public void testInsertBatch() throws SQLException {
        // Arrange
        Batch batch = new Batch(0, testProductId, "BATCH-001", LocalDateTime.now().plusMonths(6), 10.0, 50, null);

        // Act
        int batchId = batchDAO.insertBatch(batch);

        // Assert
        assertTrue(batchId > 0);
        assertEquals(batchId, batch.getId());
    }

    @Test
    public void testUpdateRemainingQuantity() throws SQLException {
        // Arrange
        Batch batch = new Batch(0, testProductId, "BATCH-002", LocalDateTime.now().plusMonths(6), 10.0, 50, null);
        int batchId = batchDAO.insertBatch(batch);

        // Act
        batchDAO.updateRemainingQuantity(batchId, 30);

        List<Batch> availableBatches = batchDAO.getAvailableBatches(testProductId);

        // Assert
        assertEquals(1, availableBatches.size());
        assertEquals(30, availableBatches.get(0).getRemainingQuantity());
    }

    @Test
    public void testGetExpiredBatches() throws SQLException {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Expired batch
        Batch expiredBatch = new Batch(0, testProductId, "BATCH-EXP", now.minusDays(1), 10.0, 10, null);
        batchDAO.insertBatch(expiredBatch);

        // Valid batch
        Batch validBatch = new Batch(0, testProductId, "BATCH-VAL", now.plusDays(10), 10.0, 10, null);
        batchDAO.insertBatch(validBatch);

        // Act
        List<Batch> expiredBatches = batchDAO.getExpiredBatches(now);

        // Assert
        assertEquals(1, expiredBatches.size());
        assertEquals("BATCH-EXP", expiredBatches.get(0).getBatchNumber());
    }

    @Test
    public void testGetAvailableBatchesFEFO() throws SQLException {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Batch expiring later
        Batch batchLater = new Batch(0, testProductId, "LATER", now.plusMonths(2), 10.0, 10, null);
        batchDAO.insertBatch(batchLater);

        // Batch expiring sooner
        Batch batchSooner = new Batch(0, testProductId, "SOONER", now.plusMonths(1), 10.0, 10, null);
        batchDAO.insertBatch(batchSooner);

        // Batch with no expiry
        Batch batchNoExpiry = new Batch(0, testProductId, "NO-EXP", null, 10.0, 10, null);
        batchDAO.insertBatch(batchNoExpiry);

        // Act (should order by Expiry Date ASC)
        List<Batch> availableBatches = batchDAO.getAvailableBatches(testProductId);

        // Assert
        assertEquals(3, availableBatches.size());
        // FEFO should prioritize sooner expiries
        assertEquals("SOONER", availableBatches.get(0).getBatchNumber());
        assertEquals("LATER", availableBatches.get(1).getBatchNumber());
        assertEquals("NO-EXP", availableBatches.get(2).getBatchNumber());
    }
}
