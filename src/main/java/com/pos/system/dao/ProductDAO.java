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
        String sql = "INSERT INTO products (barcode, name, category, cost_price, selling_price, stock, image_blob) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setDouble(4, product.getCostPrice());
            pstmt.setDouble(5, product.getSellingPrice());
            pstmt.setInt(6, product.getStock());
            pstmt.setBytes(7, product.getImageData());
            pstmt.executeUpdate();
        }
    }

    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET barcode=?, name=?, category=?, cost_price=?, selling_price=?, stock=?, image_blob=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setDouble(4, product.getCostPrice());
            pstmt.setDouble(5, product.getSellingPrice());
            pstmt.setInt(6, product.getStock());
            pstmt.setBytes(7, product.getImageData());
            pstmt.setInt(8, product.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("cost_price"),
                        rs.getDouble("selling_price"),
                        rs.getInt("stock"),
                        rs.getBytes("image_blob")));
            }
        }
        return products;
    }

    public Product getProductByBarcode(String barcode) throws SQLException {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getInt("id"),
                            rs.getString("barcode"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("cost_price"),
                            rs.getDouble("selling_price"),
                            rs.getInt("stock"),
                            rs.getBytes("image_blob"));
                }
            }
        }
        return null; // Not found
    }

    public void updateStockQuantity(int productId, int quantityChange) throws SQLException {
        String sql = "UPDATE products SET stock = stock - ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, quantityChange);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }
}
