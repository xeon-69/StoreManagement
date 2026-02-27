package com.pos.system.dao;

import com.pos.system.models.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO extends BaseDAO {

    public ProductDAO() throws SQLException {
        super();
    }

    public ProductDAO(Connection connection) {
        super(connection);
    }

    public void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (barcode, name, category_id, cost_price, selling_price, stock, image_blob) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setInt(3, product.getCategoryId());
            pstmt.setDouble(4, product.getCostPrice());
            pstmt.setDouble(5, product.getSellingPrice());
            pstmt.setInt(6, product.getStock());
            pstmt.setBytes(7, product.getImageData());
            pstmt.executeUpdate();
            logAudit("CREATE", "Product", product.getBarcode(),
                    "Name: " + product.getName() + ", Price: " + product.getSellingPrice());
        }
    }

    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET barcode=?, name=?, category_id=?, cost_price=?, selling_price=?, image_blob=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setInt(3, product.getCategoryId());
            pstmt.setDouble(4, product.getCostPrice());
            pstmt.setDouble(5, product.getSellingPrice());
            pstmt.setBytes(6, product.getImageData());
            pstmt.setInt(7, product.getId());
            pstmt.executeUpdate();
            logAudit("UPDATE", "Product", String.valueOf(product.getId()),
                    "Barcode: " + product.getBarcode() + ", Name: " + product.getName());
        }
    }

    public void deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logAudit("DELETE", "Product", String.valueOf(id), "Deleted product");
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.id, p.barcode, p.name, p.category_id, c.name AS category_name, p.cost_price, p.selling_price, p.stock, p.image_blob "
                +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getDouble("cost_price"),
                        rs.getDouble("selling_price"),
                        rs.getInt("stock"),
                        rs.getBytes("image_blob")));
            }
        }
        return products;
    }

    public Product getProductByBarcode(String barcode) throws SQLException {
        String sql = "SELECT p.id, p.barcode, p.name, p.category_id, c.name AS category_name, p.cost_price, p.selling_price, p.stock, p.image_blob "
                +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.barcode = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getInt("id"),
                            rs.getString("barcode"),
                            rs.getString("name"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getDouble("cost_price"),
                            rs.getDouble("selling_price"),
                            rs.getInt("stock"),
                            rs.getBytes("image_blob"));
                }
            }
        }
        return null; // Not found
    }

    public List<Product> getAllProductsSummary() throws SQLException {
        List<Product> products = new ArrayList<>();
        // Select all EXCEPT image_blob
        String sql = "SELECT p.id, p.barcode, p.name, p.category_id, c.name AS category_name, p.cost_price, p.selling_price, p.stock "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.id";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Pass null for imageData
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getDouble("cost_price"),
                        rs.getDouble("selling_price"),
                        rs.getInt("stock"),
                        null));
            }
        }
        return products;
    }

    public byte[] getProductImage(int id) throws SQLException {
        String sql = "SELECT image_blob FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("image_blob");
                }
            }
        }
        return null;
    }

    public Product getProductById(int id) throws SQLException {
        String sql = "SELECT p.id, p.barcode, p.name, p.category_id, c.name AS category_name, p.cost_price, p.selling_price, p.stock, p.image_blob "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getInt("id"),
                            rs.getString("barcode"),
                            rs.getString("name"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getDouble("cost_price"),
                            rs.getDouble("selling_price"),
                            rs.getInt("stock"),
                            rs.getBytes("image_blob"));
                }
            }
        }
        return null; // Not found
    }

}
