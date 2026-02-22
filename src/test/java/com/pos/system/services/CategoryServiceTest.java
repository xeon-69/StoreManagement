package com.pos.system.services;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.models.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryDAO categoryDAOMock;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryDAOMock);
    }

    @Test
    void testGetAllCategories() throws SQLException {
        // Arrange
        List<Category> expectedCategories = Arrays.asList(
                new Category(1, "Electronics", "Devices and Gadgets"),
                new Category(2, "Books", "Reading materials"));
        when(categoryDAOMock.getAllCategories()).thenReturn(expectedCategories);

        // Act
        List<Category> actualCategories = categoryService.getAllCategories();

        // Assert
        assertEquals(2, actualCategories.size());
        assertEquals("Electronics", actualCategories.get(0).getName());
        verify(categoryDAOMock, times(1)).getAllCategories();
    }

    @Test
    void testAddCategory() throws SQLException {
        // Arrange
        Category newCategory = new Category(0, "Clothing", "Apparel");

        // Act
        categoryService.addCategory(newCategory);

        // Assert
        verify(categoryDAOMock, times(1)).addCategory(newCategory);
    }

    @Test
    void testUpdateCategory() throws SQLException {
        // Arrange
        Category categoryToUpdate = new Category(1, "Updated Electronics", "Updated Description");

        // Act
        categoryService.updateCategory(categoryToUpdate);

        // Assert
        verify(categoryDAOMock, times(1)).updateCategory(categoryToUpdate);
    }

    @Test
    void testDeleteCategory() throws SQLException {
        // Arrange
        int categoryId = 1;

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        verify(categoryDAOMock, times(1)).deleteCategory(categoryId);
    }
}
