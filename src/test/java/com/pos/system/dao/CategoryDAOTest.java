package com.pos.system.dao;

import com.pos.system.models.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryDAOTest extends BaseDAOTest {

    private CategoryDAO categoryDAO;

    @BeforeEach
    public void setUp() {
        categoryDAO = new CategoryDAO(connection);
    }

    @Test
    public void testAddAndGetAllCategories() throws SQLException {
        // Arrange
        Category category1 = new Category(0, "Beverages", "Drinks and juices");
        Category category2 = new Category(0, "Snacks", "Chips and cookies");

        // Act
        categoryDAO.addCategory(category1);
        categoryDAO.addCategory(category2);

        List<Category> categories = categoryDAO.getAllCategories();

        // Assert
        assertEquals(2, categories.size());

        // Since sqlite might auto-increment ids starting from 1, let's just check
        // contents
        assertTrue(categories.stream()
                .anyMatch(c -> c.getName().equals("Beverages") && c.getDescription().equals("Drinks and juices")));
        assertTrue(categories.stream()
                .anyMatch(c -> c.getName().equals("Snacks") && c.getDescription().equals("Chips and cookies")));
    }

    @Test
    public void testUpdateCategory() throws SQLException {
        // Arrange
        Category category = new Category(0, "Beverages", "Drinks and juices");
        categoryDAO.addCategory(category);

        List<Category> categories = categoryDAO.getAllCategories();
        Category savedCategory = categories.get(0);

        // Act
        savedCategory.setName("Cold Beverages");
        savedCategory.setDescription("Sodas and more");
        categoryDAO.updateCategory(savedCategory);

        List<Category> updatedCategories = categoryDAO.getAllCategories();
        Category updatedCategory = updatedCategories.get(0);

        // Assert
        assertEquals("Cold Beverages", updatedCategory.getName());
        assertEquals("Sodas and more", updatedCategory.getDescription());
    }

    @Test
    public void testDeleteCategory() throws SQLException {
        // Arrange
        Category category = new Category(0, "Beverages", "Drinks and juices");
        categoryDAO.addCategory(category);

        List<Category> categories = categoryDAO.getAllCategories();
        int savedId = categories.get(0).getId();

        // Act
        categoryDAO.deleteCategory(savedId);

        List<Category> remainingCategories = categoryDAO.getAllCategories();

        // Assert
        assertTrue(remainingCategories.isEmpty());
    }
}
