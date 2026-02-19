package com.pos.system.dao;

import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class SaleDAO extends BaseDAO {

    public SaleDAO() throws SQLException {
        super();
    }

    public SaleDAO(Connection connection) {
        super(connection);
    }

    public void createSale(Sale sale, List<SaleItem> items) throws SQLException {
        PreparedStatement saleStmt = null;
        PreparedStatement itemStmt = null;
        PreparedStatement updateStockStmt = null;

        String insertSale = "INSERT INTO sales (user_id, total_amount, total_profit) VALUES (?, ?, ?)";
        String insertItem = "INSERT INTO sale_items (sale_id, product_id, quantity, price_at_sale, cost_at_sale) VALUES (?, ?, ?, ?, ?)";
        String updateStock = "UPDATE products SET stock = stock - ? WHERE id = ?";

        try {
            connection.setAutoCommit(false); // Start Transaction

            // 1. Insert Sale Header
            saleStmt = connection.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS);
            saleStmt.setInt(1, sale.getUserId());
            saleStmt.setDouble(2, sale.getTotalAmount());
            saleStmt.setDouble(3, sale.getTotalProfit());
            saleStmt.executeUpdate();

            int saleId = 0;
            try (ResultSet generatedKeys = saleStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saleId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }

            // 2. Insert Items & Update Stock
            itemStmt = connection.prepareStatement(insertItem);
            updateStockStmt = connection.prepareStatement(updateStock);

            for (SaleItem item : items) {
                // Add Item
                itemStmt.setInt(1, saleId);
                itemStmt.setInt(2, item.getProductId());
                itemStmt.setInt(3, item.getQuantity());
                itemStmt.setDouble(4, item.getPriceAtSale());
                itemStmt.setDouble(5, item.getCostAtSale());
                itemStmt.addBatch();

                // Update Stock
                updateStockStmt.setInt(1, item.getQuantity());
                updateStockStmt.setInt(2, item.getProductId());
                updateStockStmt.addBatch();
            }

            itemStmt.executeBatch();
            updateStockStmt.executeBatch();

            connection.commit(); // Global Commit

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    logger.error("Error during rollback", ex);
                }
            }
            throw e;
        } finally {
            closeStatement(saleStmt);
            closeStatement(itemStmt);
            closeStatement(updateStockStmt);
            try {
                if (connection != null)
                    connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Error resetting auto-commit", e);
            }
        }
    }

    // New granular methods for Service Layer
    public int insertSale(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (user_id, total_amount, total_profit) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, sale.getUserId());
            stmt.setDouble(2, sale.getTotalAmount());
            stmt.setDouble(3, sale.getTotalProfit());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }
        }
    }

    public void insertSaleItems(int saleId, List<SaleItem> items) throws SQLException {
        String sql = "INSERT INTO sale_items (sale_id, product_id, quantity, price_at_sale, cost_at_sale) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (SaleItem item : items) {
                stmt.setInt(1, saleId);
                stmt.setInt(2, item.getProductId());
                stmt.setInt(3, item.getQuantity());
                stmt.setDouble(4, item.getPriceAtSale());
                stmt.setDouble(5, item.getCostAtSale());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public double getTotalSalesSince(LocalDateTime since) throws SQLException {
        String sql = "SELECT SUM(total_amount) FROM sales WHERE sale_date >= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, since.toString()); // SQLite stores dates as strings usually, or use Timestamp
            // If using standard SQLite layout:
            // stmt.setObject(1, since); // JDBC might handle it if using robust driver
            // Safest for SQLite default:
            // stmt.setString(1, since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            // Let's assume standard ISO format string.

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }
}
