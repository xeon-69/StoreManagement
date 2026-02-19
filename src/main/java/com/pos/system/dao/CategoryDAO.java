package com.pos.system.dao;

import com.pos.system.models.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends BaseDAO {

    public CategoryDAO() throws SQLException {
        super();
    }

    public CategoryDAO(Connection connection) {
        super(connection);
    }

    public void addCategory(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.executeUpdate();
        }
    }

    public void updateCategory(Category category) throws SQLException {
        String sql = "UPDATE categories SET name=?, description=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setInt(3, category.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<Category> getAllCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")));
            }
        }
        return categories;
    }
}
