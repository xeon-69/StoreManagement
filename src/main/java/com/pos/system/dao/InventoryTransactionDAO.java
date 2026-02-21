package com.pos.system.dao;

import com.pos.system.models.InventoryTransaction;
import com.pos.system.models.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryTransactionDAO extends BaseDAO {

    public InventoryTransactionDAO() throws SQLException {
        super();
    }

    public InventoryTransactionDAO(Connection connection) {
        super(connection);
    }

    public void insertTransaction(InventoryTransaction tx) throws SQLException {
        String sql = "INSERT INTO inventory_transactions (product_id, batch_id, quantity_change, transaction_type, reference_id, created_by) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, tx.getProductId());

            if (tx.getBatchId() != null) {
                stmt.setInt(2, tx.getBatchId());
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }

            stmt.setInt(3, tx.getQuantityChange());
            stmt.setString(4, tx.getTransactionType().name());
            stmt.setString(5, tx.getReferenceId());

            if (tx.getCreatedBy() != null) {
                stmt.setInt(6, tx.getCreatedBy());
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }

            stmt.executeUpdate();
        }
    }

    /**
     * Aggregates the absolute stock level for a product by summing all IN and OUT
     * transactions.
     * This acts as the single source of truth for stock quantities.
     */
    public int calculateStockLevel(int productId) throws SQLException {
        String sql = "SELECT SUM(quantity_change) FROM inventory_transactions WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0; // Default if no transactions exist
    }

    public List<InventoryTransaction> getTransactionHistory(int productId) throws SQLException {
        List<InventoryTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM inventory_transactions WHERE product_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setId(rs.getInt("id"));
                    tx.setProductId(rs.getInt("product_id"));

                    int batchId = rs.getInt("batch_id");
                    if (!rs.wasNull()) {
                        tx.setBatchId(batchId);
                    }

                    tx.setQuantityChange(rs.getInt("quantity_change"));
                    tx.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
                    tx.setReferenceId(rs.getString("reference_id"));

                    int createdBy = rs.getInt("created_by");
                    if (!rs.wasNull()) {
                        tx.setCreatedBy(createdBy);
                    }

                    String createdStr = rs.getString("created_at");
                    if (createdStr != null && !createdStr.isEmpty()) {
                        tx.setCreatedAt(LocalDateTime.parse(createdStr.replace(" ", "T")));
                    }

                    transactions.add(tx);
                }
            }
        }
        return transactions;
    }
}
