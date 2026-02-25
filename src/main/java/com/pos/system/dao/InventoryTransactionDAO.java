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
        String sql = "INSERT INTO inventory_transactions (product_id, batch_id, quantity_change, transaction_type, reference_id, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            stmt.setString(7,
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            stmt.executeUpdate();
            logAudit("CREATE", "InventoryTransaction", tx.getTransactionType().name(),
                    "Product: " + tx.getProductId() + ", Change: " + tx.getQuantityChange());
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

    public List<InventoryTransaction> getTransactionsBetween(LocalDateTime start, LocalDateTime end)
            throws SQLException {
        List<InventoryTransaction> transactions = new ArrayList<>();
        String sql = "SELECT it.*, p.name as product_name FROM inventory_transactions it " +
                "JOIN products p ON it.product_id = p.id " +
                "WHERE it.created_at BETWEEN ? AND ? ORDER BY it.created_at ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            stmt.setString(2, end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public List<InventoryTransaction> getTransactionHistory(int productId) throws SQLException {
        List<InventoryTransaction> transactions = new ArrayList<>();
        String sql = "SELECT it.*, p.name as product_name FROM inventory_transactions it " +
                "JOIN products p ON it.product_id = p.id " +
                "WHERE it.product_id = ? ORDER BY it.created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    private InventoryTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setId(rs.getInt("id"));
        tx.setProductId(rs.getInt("product_id"));

        try {
            tx.setProductName(rs.getString("product_name"));
        } catch (SQLException e) {
            // Field might not exist in all queries
        }

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
        return tx;
    }
}
