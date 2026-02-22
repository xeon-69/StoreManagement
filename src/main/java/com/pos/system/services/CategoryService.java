package com.pos.system.services;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.models.Category;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() throws SQLException {
        this.categoryDAO = new CategoryDAO();
    }

    // For Dependency Injection in unit tests
    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public List<Category> getAllCategories() throws SQLException {
        return categoryDAO.getAllCategories();
    }

    public void addCategory(Category category) throws SQLException {
        categoryDAO.addCategory(category);
    }

    public void updateCategory(Category category) throws SQLException {
        categoryDAO.updateCategory(category);
    }

    public void deleteCategory(int id) throws SQLException {
        categoryDAO.deleteCategory(id);
    }
}
