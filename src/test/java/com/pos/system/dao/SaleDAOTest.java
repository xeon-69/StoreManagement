package com.pos.system.dao;

import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.Category;
import com.pos.system.models.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SaleDAOTest extends BaseDAOTest {

    private SaleDAO saleDAO;
    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;
    private int productId1;
    private int productId2;

    @BeforeEach
    public void setUp() throws SQLException {
        saleDAO = new SaleDAO(connection);
        productDAO = new ProductDAO(connection);
        categoryDAO = new CategoryDAO(connection);

        categoryDAO.addCategory(new Category(0, "CatSale", "Sale Items"));
        int catId = categoryDAO.getAllCategories().get(0).getId();

        productDAO.addProduct(new Product(0, "S1", "Item 1", catId, "CatSale", 5.0, 10.0, 100, null));
        productDAO.addProduct(new Product(0, "S2", "Item 2", catId, "CatSale", 10.0, 20.0, 50, null));

        productId1 = productDAO.getProductByBarcode("S1").getId();
        productId2 = productDAO.getProductByBarcode("S2").getId();
    }

    @Test
    public void testCreateSaleWithItems() throws SQLException {
        // Arrange
        Sale sale = new Sale(0, 1, 44.0, 20.0, LocalDateTime.now());
        SaleItem item1 = new SaleItem(0, 0, productId1, "Item 1", 2, 10.0, 5.0);
        SaleItem item2 = new SaleItem(0, 0, productId2, "Item 2", 1, 20.0, 10.0);

        // Act
        saleDAO.createSale(sale, Arrays.asList(item1, item2));

        // Assert
        assertTrue(sale.getId() > 0);

        // Verify items were added
        List<SaleItem> retrievedItems = saleDAO.getItemsBySaleId(sale.getId());
        assertEquals(2, retrievedItems.size());

        // Verify stock was updated
        Product p1 = productDAO.getProductByBarcode("S1");
        Product p2 = productDAO.getProductByBarcode("S2");
        assertEquals(98, p1.getStock()); // 100 - 2
        assertEquals(49, p2.getStock()); // 50 - 1
    }

    @Test
    public void testInsertSaleAndGetSales() throws SQLException {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Sale sale = new Sale(0, 1, 55.0, 25.0, now.minusDays(1));

        // Act
        int saleId = saleDAO.insertSale(sale);

        Sale sale2 = new Sale(0, 1, 110.0, 50.0, now);
        saleDAO.insertSale(sale2);

        List<Sale> allSales = saleDAO.getAllSales();
        List<Sale> filteredSales = saleDAO.getSalesBetween(now.minusDays(2), now.minusHours(1));

        // Assert
        assertTrue(saleId > 0);
        assertEquals(2, allSales.size());
        assertEquals(1, filteredSales.size()); // Should only find the first sale
        assertEquals(55.0, filteredSales.get(0).getTotalAmount(), 0.001);
    }

    @Test
    public void testTotalCalculations() throws SQLException {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Sale s1 = new Sale(0, 1, 110.0, 50.0, now.minusDays(1));
        Sale s2 = new Sale(0, 1, 220.0, 100.0, now.minusHours(1));

        saleDAO.insertSale(s1);
        saleDAO.insertSale(s2);

        // Act
        double totalSales = saleDAO.getTotalSalesBetween(now.minusDays(2), now.plusDays(1));
        double totalProfit = saleDAO.getTotalProfitBetween(now.minusDays(2), now.plusDays(1));

        // Assert
        assertEquals(330.0, totalSales, 0.001);
        assertEquals(150.0, totalProfit, 0.001);
    }
}
