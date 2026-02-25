package com.pos.system.dao;

import com.pos.system.models.Batch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BatchDAO extends BaseDAO {

    public BatchDAO() throws SQLException {
        super();
    }

    public BatchDAO(Connection connection) {
        super(connection);
    }

    public int insertBatch(Batch batch) throws SQLException {
        String sql = "INSERT INTO batches (product_id, batch_number, expiry_date, cost_price, remaining_quantity, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, batch.getProductId());
            stmt.setString(2, batch.getBatchNumber());
            if (batch.getExpiryDate() != null) {
                stmt.setString(3, batch.getExpiryDate().toString());
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }
            stmt.setDouble(4, batch.getCostPrice());
            stmt.setInt(5, batch.getRemainingQuantity());
            stmt.setString(6,
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    batch.setId(id);
                    logAudit("CREATE", "Batch", batch.getBatchNumber(),
                            "Product ID: " + batch.getProductId() + ", Qty: " + batch.getRemainingQuantity());
                    return id;
                } else {
                    throw new SQLException("Failed to create batch, no ID obtained.");
                }
            }
        }
    }

    public void updateRemainingQuantity(int batchId, int remainingQuantity) throws SQLException {
        String sql = "UPDATE batches SET remaining_quantity = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, remainingQuantity);
            stmt.setInt(2, batchId);
            stmt.executeUpdate();
            logAudit("UPDATE", "Batch", String.valueOf(batchId), "Remaining quantity updated to: " + remainingQuantity);
        }
    }

    public List<Batch> getExpiredBatches(LocalDateTime threshold) throws SQLException {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM batches WHERE remaining_quantity > 0 AND expiry_date IS NOT NULL AND expiry_date < ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, threshold.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapResultSetToBatch(rs));
                }
            }
        }
        return batches;
    }

    /**
     * Finds active batches for a product. Orders by Expiry Date (FEFO) then Created
     * At (FIFO).
     */
    public List<Batch> getAvailableBatches(int productId) throws SQLException {
        List<Batch> batches = new ArrayList<>();
        // COALESCE expiry_date to far future so non-expiring items go last in FEFO.
        String sql = "SELECT * FROM batches WHERE product_id = ? AND remaining_quantity > 0 " +
                "ORDER BY COALESCE(expiry_date, '9999-12-31') ASC, created_at ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapResultSetToBatch(rs));
                }
            }
        }
        return batches;
    }

    private Batch mapResultSetToBatch(ResultSet rs) throws SQLException {
        Batch b = new Batch();
        b.setId(rs.getInt("id"));
        b.setProductId(rs.getInt("product_id"));
        b.setBatchNumber(rs.getString("batch_number"));

        String expiryStr = rs.getString("expiry_date");
        if (expiryStr != null && !expiryStr.isEmpty()) {
            b.setExpiryDate(LocalDateTime.parse(expiryStr.replace(" ", "T")));
        }

        b.setCostPrice(rs.getDouble("cost_price"));
        b.setRemainingQuantity(rs.getInt("remaining_quantity"));

        String createdStr = rs.getString("created_at");
        if (createdStr != null && !createdStr.isEmpty()) {
            b.setCreatedAt(LocalDateTime.parse(createdStr.replace(" ", "T")));
        }
        return b;
    }
}
