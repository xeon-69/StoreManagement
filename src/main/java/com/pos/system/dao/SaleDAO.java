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

        String insertSale = "INSERT INTO sales (user_id, subtotal, tax_amount, discount_amount, total_amount, total_profit, sale_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO sale_items (sale_id, product_id, quantity, price_at_sale, cost_at_sale, discount_amount, tax_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateStock = "UPDATE products SET stock = stock - ? WHERE id = ?";

        try {
            connection.setAutoCommit(false); // Start Transaction

            // 1. Insert Sale Header
            saleStmt = connection.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS);
            saleStmt.setInt(1, sale.getUserId());
            saleStmt.setDouble(2, sale.getSubtotal());
            saleStmt.setDouble(3, sale.getTaxAmount());
            saleStmt.setDouble(4, sale.getDiscountAmount());
            saleStmt.setDouble(5, sale.getTotalAmount());
            saleStmt.setDouble(6, sale.getTotalProfit());
            saleStmt.setString(7,
                    sale.getSaleDate() != null ? sale.getSaleDate().toString() : LocalDateTime.now().toString());
            saleStmt.executeUpdate();

            int saleId = 0;
            try (ResultSet generatedKeys = saleStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saleId = generatedKeys.getInt(1);
                    sale.setId(saleId);
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
                itemStmt.setDouble(6, item.getDiscountAmount());
                itemStmt.setDouble(7, item.getTaxAmount());
                itemStmt.addBatch();

                // Update Stock
                updateStockStmt.setInt(1, item.getQuantity());
                updateStockStmt.setInt(2, item.getProductId());
                updateStockStmt.addBatch();
            }

            itemStmt.executeBatch();
            updateStockStmt.executeBatch();
            logAudit("CREATE", "Sale", String.valueOf(saleId), "Total: " + sale.getTotalAmount());

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

    public int insertSale(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (user_id, subtotal, tax_amount, discount_amount, total_amount, total_profit, sale_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, sale.getUserId());
            stmt.setDouble(2, sale.getSubtotal());
            stmt.setDouble(3, sale.getTaxAmount());
            stmt.setDouble(4, sale.getDiscountAmount());
            stmt.setDouble(5, sale.getTotalAmount());
            stmt.setDouble(6, sale.getTotalProfit());
            stmt.setString(7, sale.getSaleDate().toString().replace("T", " "));
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sale.setId(generatedKeys.getInt(1));
                    logAudit("CREATE", "Sale", String.valueOf(sale.getId()), "Total: " + sale.getTotalAmount());
                    return sale.getId();
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }
        }
    }

    public void insertSaleItems(int saleId, List<SaleItem> items) throws SQLException {
        String sql = "INSERT INTO sale_items (sale_id, product_id, quantity, price_at_sale, cost_at_sale, discount_amount, tax_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (SaleItem item : items) {
                stmt.setInt(1, saleId);
                stmt.setInt(2, item.getProductId());
                stmt.setInt(3, item.getQuantity());
                stmt.setDouble(4, item.getPriceAtSale());
                stmt.setDouble(5, item.getCostAtSale());
                stmt.setDouble(6, item.getDiscountAmount());
                stmt.setDouble(7, item.getTaxAmount());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public double getTotalSalesBetween(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT SUM(total_amount) FROM sales WHERE sale_date >= ? AND sale_date <= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, start.toString().replace("T", " "));
            stmt.setString(2, end.toString().replace("T", " "));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getTotalProfitBetween(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT SUM(total_profit) FROM sales WHERE sale_date >= ? AND sale_date <= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, start.toString().replace("T", " "));
            stmt.setString(2, end.toString().replace("T", " "));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public List<Sale> getAllSales() throws SQLException {
        List<Sale> sales = new java.util.ArrayList<>();
        String sql = "SELECT s.*, " +
                "(SELECT GROUP_CONCAT(p.name || ' (x' || si.quantity || ')', ', ') " +
                " FROM sale_items si JOIN products p ON si.product_id = p.id WHERE si.sale_id = s.id) AS details " +
                "FROM sales s ORDER BY s.sale_date DESC LIMIT 100";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        }
        return sales;
    }

    public List<Sale> getSalesBetween(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<Sale> sales = new java.util.ArrayList<>();
        String sql = "SELECT s.*, " +
                "(SELECT GROUP_CONCAT(p.name || ' (x' || si.quantity || ')', ', ') " +
                " FROM sale_items si JOIN products p ON si.product_id = p.id WHERE si.sale_id = s.id) AS details " +
                "FROM sales s WHERE s.sale_date >= ? AND s.sale_date <= ? ORDER BY s.sale_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, start.toString().replace("T", " "));
            stmt.setString(2, end.toString().replace("T", " "));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    public List<SaleItem> getItemsBySaleId(int saleId) throws SQLException {
        List<SaleItem> items = new java.util.ArrayList<>();
        String sql = "SELECT si.*, p.name as product_name, c.name as category_name " +
                "FROM sale_items si " +
                "JOIN products p ON si.product_id = p.id " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE si.sale_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = mapResultSetToSaleItem(rs);
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<SaleItem> getItemsBySaleIds(List<Integer> saleIds) throws SQLException {
        List<SaleItem> items = new java.util.ArrayList<>();
        if (saleIds == null || saleIds.isEmpty())
            return items;

        StringBuilder sql = new StringBuilder("SELECT si.*, p.name as product_name, c.name as category_name " +
                "FROM sale_items si " +
                "JOIN products p ON si.product_id = p.id " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE si.sale_id IN (");

        for (int i = 0; i < saleIds.size(); i++) {
            sql.append("?");
            if (i < saleIds.size() - 1)
                sql.append(",");
        }
        sql.append(")");

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < saleIds.size(); i++) {
                stmt.setInt(i + 1, saleIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToSaleItem(rs));
                }
            }
        }
        return items;
    }

    private SaleItem mapResultSetToSaleItem(ResultSet rs) throws SQLException {
        SaleItem item = new SaleItem(
                rs.getInt("id"),
                rs.getInt("sale_id"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("price_at_sale"),
                rs.getDouble("cost_at_sale"));
        item.setDiscountAmount(rs.getDouble("discount_amount"));
        item.setTaxAmount(rs.getDouble("tax_amount"));
        item.setCategoryName(rs.getString("category_name"));
        return item;
    }

    private Sale mapResultSetToSale(ResultSet rs) throws SQLException {
        LocalDateTime date;
        try {
            date = LocalDateTime.parse(rs.getString("sale_date"));
        } catch (Exception e) {
            date = LocalDateTime.parse(rs.getString("sale_date").replace(" ", "T"));
        }

        Sale sale = new Sale(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getDouble("subtotal"),
                rs.getDouble("tax_amount"),
                rs.getDouble("discount_amount"),
                rs.getDouble("total_amount"),
                rs.getDouble("total_profit"),
                date);
        sale.setTransactionDetails(rs.getString("details"));
        return sale;
    }
}
