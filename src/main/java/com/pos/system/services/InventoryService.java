package com.pos.system.services;

import com.pos.system.dao.BatchDAO;
import com.pos.system.dao.InventoryTransactionDAO;
import com.pos.system.models.Batch;
import com.pos.system.models.InventoryTransaction;
import com.pos.system.models.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InventoryService {
    private final SecurityService securityService;

    public InventoryService() {
        SecurityService ss = null;
        try {
            ss = new SecurityService();
        } catch (SQLException e) {
            // Cannot log to audit log if security service fails, but we have logger in
            // other services
        }
        this.securityService = ss;
    }

    public InventoryService(SecurityService securityService) {
        this.securityService = securityService;
    }

    // Protected methods to allow mocking DAOs in tests
    protected BatchDAO getBatchDAO(Connection connection) {
        return new BatchDAO(connection);
    }

    protected InventoryTransactionDAO getInventoryTransactionDAO(Connection connection) {
        return new InventoryTransactionDAO(connection);
    }

    /**
     * Receives new stock by creating a new Batch and logging a PURCHASE
     * transaction.
     */
    public void addStock(Connection conn, int productId, int quantity, double costPrice, LocalDateTime expiryDate,
            String referenceId, Integer createdBy) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive.");
        }

        try (BatchDAO batchDAO = getBatchDAO(conn);
                InventoryTransactionDAO txDAO = getInventoryTransactionDAO(conn)) {

            // 1. Create Batch
            Batch newBatch = new Batch(0, productId, "BATCH-" + UUID.randomUUID().toString().substring(0, 8),
                    expiryDate, costPrice, quantity, null);
            batchDAO.insertBatch(newBatch);

            // 2. Log Transaction
            InventoryTransaction tx = new InventoryTransaction(0, productId, newBatch.getId(), quantity,
                    TransactionType.PURCHASE, referenceId, null, createdBy);
            txDAO.insertTransaction(tx);

            // 3. Update Cached Product Stock
            updateCachedProductStock(conn, productId, txDAO);

            // 4. Audit Log
            if (securityService != null) {
                securityService.logAction(createdBy, "STOCK_PURCHASE", "Product", String.valueOf(productId),
                        "Qty added: " + quantity + ", Ref: " + referenceId);
            }
        }
    }

    /**
     * Deducts stock fulfilling standard FEFO/FIFO ordering. Logs multiple
     * transactions if deduction spans across multiple batches.
     */
    public void deductStock(Connection conn, int productId, int quantityToDeduct, TransactionType type,
            String referenceId, Integer createdBy) throws SQLException {
        if (quantityToDeduct <= 0) {
            throw new IllegalArgumentException("Quantity to deduct must be positive.");
        }

        try (BatchDAO batchDAO = getBatchDAO(conn);
                InventoryTransactionDAO txDAO = getInventoryTransactionDAO(conn)) {

            List<Batch> availableBatches = batchDAO.getAvailableBatches(productId);
            int remainingToDeduct = quantityToDeduct;

            for (Batch batch : availableBatches) {
                if (remainingToDeduct <= 0)
                    break;

                int batchQty = batch.getRemainingQuantity();
                int deductionFromThisBatch = Math.min(batchQty, remainingToDeduct);

                // Deduct from batch
                batchDAO.updateRemainingQuantity(batch.getId(), batchQty - deductionFromThisBatch);

                // Log negative transaction tied to this exact batch
                InventoryTransaction tx = new InventoryTransaction(0, productId, batch.getId(), -deductionFromThisBatch,
                        type, referenceId, null, createdBy);
                txDAO.insertTransaction(tx);

                remainingToDeduct -= deductionFromThisBatch;
            }

            if (remainingToDeduct > 0) {
                // We oversold based on active batches. In a strict system, throw Exception.
                // For POS, we might log a generic negative transaction without a batch ID to
                // allow negative stock (if permitted),
                // but since requirement is "Prevent negative stock":
                throw new SQLException("Insufficient stock to fulfill deduction for Product ID: " + productId
                        + " Short by: " + remainingToDeduct);
            }

            // Sync cache
            updateCachedProductStock(conn, productId, txDAO);
        }
    }

    /**
     * Generic adjustments (audits, shrinkage, found stock, etc).
     */
    public void adjustStock(Connection conn, int productId, int quantityChange, String referenceId, Integer createdBy)
            throws SQLException {
        if (quantityChange == 0)
            return;

        try (InventoryTransactionDAO txDAO = getInventoryTransactionDAO(conn)) {
            if (quantityChange > 0) {
                // Treat positive adjustment like adding generic stock: Needs a batch or generic
                // pool.
                addStock(conn, productId, quantityChange, 0.0, null, referenceId, createdBy);
            } else {
                // Treat negative adjustment like a standard deduction to ensure FEFO is honored
                deductStock(conn, productId, Math.abs(quantityChange), TransactionType.ADJUSTMENT, referenceId,
                        createdBy);
            }

            // Audit Log
            if (securityService != null) {
                securityService.logAction(createdBy, "STOCK_ADJUSTMENT", "Product", String.valueOf(productId),
                        "Qty change: " + quantityChange + ", Ref: " + referenceId);
            }
        }
    }

    /**
     * Hydrates the physical 'stock' column on the products table using the absolute
     * summation of ledger histories.
     * This acts as a hyper-fast read cache for UI grids.
     */
    private void updateCachedProductStock(Connection conn, int productId, InventoryTransactionDAO txDAO)
            throws SQLException {
        int actualStock = txDAO.calculateStockLevel(productId);

        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, actualStock);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    /**
     * Automatically scans for natively expired Batches, removes them from
     * circulation, and logs an EXPIRE transaction.
     */
    public void expireItems(Connection conn, Integer createdBy) throws SQLException {
        try (BatchDAO batchDAO = getBatchDAO(conn);
                InventoryTransactionDAO txDAO = getInventoryTransactionDAO(conn)) {

            List<Batch> expiredBatches = batchDAO.getExpiredBatches(LocalDateTime.now());

            for (Batch batch : expiredBatches) {
                int quantityToExpire = batch.getRemainingQuantity();

                // Zero out batch
                batchDAO.updateRemainingQuantity(batch.getId(), 0);

                // Log EXPIRE transaction
                InventoryTransaction tx = new InventoryTransaction(0, batch.getProductId(), batch.getId(),
                        -quantityToExpire, TransactionType.EXPIRE, "AUTO-EXPIRE", null, createdBy);
                txDAO.insertTransaction(tx);

                // Update Cache
                updateCachedProductStock(conn, batch.getProductId(), txDAO);
            }
        }
    }

    /**
     * Retrieves the complete lifecycle transaction ledger for a specific product.
     */
    public List<InventoryTransaction> getTransactionHistory(Connection conn, int productId) throws SQLException {
        try (InventoryTransactionDAO txDAO = getInventoryTransactionDAO(conn)) {
            return txDAO.getTransactionHistory(productId);
        }
    }
}
