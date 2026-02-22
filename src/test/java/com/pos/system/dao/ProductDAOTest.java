package com.pos.system.dao;

import com.pos.system.models.Category;
import com.pos.system.models.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductDAOTest extends BaseDAOTest {

    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;

    @BeforeEach
    public void setUp() throws SQLException {
        productDAO = new ProductDAO(connection);
        categoryDAO = new CategoryDAO(connection);
    }

    @Test
    public void testAddAndGetProduct() throws SQLException {
        // Arrange
        Category category = new Category(0, "Electronics", "Devices and gadgets");
        categoryDAO.addCategory(category);

        // Since sqlite auto-increments, let's get the category ID
        int categoryId = categoryDAO.getAllCategories().get(0).getId();

        Product product = new Product(0, "BC-123", "Laptop", categoryId, "Electronics", 500.0, 799.99, 10, null);

        // Act
        productDAO.addProduct(product);
        List<Product> products = productDAO.getAllProducts();

        // Assert
        assertEquals(1, products.size());
        Product savedProduct = products.get(0);
        assertEquals("BC-123", savedProduct.getBarcode());
        assertEquals("Laptop", savedProduct.getName());
        assertEquals(categoryId, savedProduct.getCategoryId());
        // Verify JOIN returns category name
        assertEquals("Electronics", savedProduct.getCategory());
        assertEquals(500.0, savedProduct.getCostPrice(), 0.001);
        assertEquals(799.99, savedProduct.getSellingPrice(), 0.001);
        assertEquals(10, savedProduct.getStock());
    }

    @Test
    public void testGetProductByBarcode() throws SQLException {
        // Arrange
        Product product = new Product(0, "BC-456", "Mouse", 0, null, 10.0, 25.0, 50, null);
        productDAO.addProduct(product);

        // Act
        Product retrievedProduct = productDAO.getProductByBarcode("BC-456");

        // Assert
        assertNotNull(retrievedProduct);
        assertEquals("Mouse", retrievedProduct.getName());
        assertEquals(10.0, retrievedProduct.getCostPrice(), 0.001);
    }

    @Test
    public void testGetProductByBarcodeNotFound() throws SQLException {
        // Act
        Product retrievedProduct = productDAO.getProductByBarcode("NON_EXISTENT");

        // Assert
        assertNull(retrievedProduct);
    }

    @Test
    public void testUpdateProduct() throws SQLException {
        // Arrange
        Product product = new Product(0, "BC-789", "Keyboard", 0, null, 20.0, 45.0, 30, null);
        productDAO.addProduct(product);
        Product savedProduct = productDAO.getProductByBarcode("BC-789");

        // Act
        savedProduct.setName("Mechanical Keyboard");
        savedProduct.setSellingPrice(65.0);
        productDAO.updateProduct(savedProduct);

        Product updatedProduct = productDAO.getProductByBarcode("BC-789");

        // Assert
        assertEquals("Mechanical Keyboard", updatedProduct.getName());
        assertEquals(65.0, updatedProduct.getSellingPrice(), 0.001);
    }

    @Test
    public void testDeleteProduct() throws SQLException {
        // Arrange
        Product product = new Product(0, "BC-DEL", "To Delete", 0, null, 5.0, 10.0, 5, null);
        productDAO.addProduct(product);
        int savedId = productDAO.getProductByBarcode("BC-DEL").getId();

        // Act
        productDAO.deleteProduct(savedId);

        List<Product> products = productDAO.getAllProducts();

        // Assert
        assertTrue(products.isEmpty());
    }
}
