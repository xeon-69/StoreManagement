package com.pos.system.services;

import com.pos.system.dao.ProductDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CheckoutServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private SaleDAO mockSaleDAO;

    @Mock
    private ProductDAO mockProductDAO;

    @Mock
    private SalePaymentDAO mockSalePaymentDAO;

    @Mock
    private InventoryService mockInventoryService;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        // We create a testable subclass of CheckoutService to inject our mocked
        // Connection and DAOs
        checkoutService = new CheckoutService(mockInventoryService) {
            @Override
            protected Connection getConnection() {
                return mockConnection;
            }

            @Override
            protected SaleDAO getSaleDAO(Connection connection) {
                return mockSaleDAO;
            }

            @Override
            protected ProductDAO getProductDAO(Connection connection) {
                return mockProductDAO;
            }

            @Override
            protected SalePaymentDAO getSalePaymentDAO(Connection connection) {
                return mockSalePaymentDAO;
            }
        };
    }

    @Test
    void testProcessCheckout_SuccessfulTransaction() throws SQLException {
        // Arrange
        Sale sale = new Sale(0, 1, 100.0, 20.0, java.time.LocalDateTime.now());

        List<SaleItem> items = new ArrayList<>();
        SaleItem item1 = new SaleItem(0, 0, 101, "Test Product", 2, 50.0, 40.0);
        items.add(item1);

        int expectedSaleId = 500;
        when(mockSaleDAO.insertSale(sale)).thenReturn(expectedSaleId);

        // Act
        checkoutService.processCheckout(sale, items);

        // Assert
        verify(mockConnection).setAutoCommit(false); // transaction started
        verify(mockSaleDAO).insertSale(sale); // header inserted
        verify(mockSaleDAO).insertSaleItems(expectedSaleId, items); // items inserted

        // inventory updated
        verify(mockInventoryService).deductStock(
                eq(mockConnection),
                eq(101),
                eq(2),
                eq(TransactionType.SALE),
                eq("SALE-" + expectedSaleId),
                eq(1));

        verify(mockConnection).commit(); // transaction committed
        verify(mockConnection).setAutoCommit(true); // reset auto-commit
    }

    @Test
    void testProcessCheckout_RollbackOnException() throws SQLException {
        // Arrange
        Sale sale = new Sale(0, 1, 100.0, 20.0, java.time.LocalDateTime.now());
        List<SaleItem> items = new ArrayList<>();

        // Simulate an SQL exception when inserting sale
        when(mockSaleDAO.insertSale(any(Sale.class))).thenThrow(new SQLException("Database error"));

        // Act & Assert
        assertThrows(SQLException.class, () -> {
            checkoutService.processCheckout(sale, items);
        });

        // verify rollback occurred
        verify(mockConnection).rollback();
        verify(mockConnection, never()).commit();
        verify(mockConnection).setAutoCommit(true);
    }
}
