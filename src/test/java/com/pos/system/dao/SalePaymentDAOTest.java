package com.pos.system.dao;

import com.pos.system.models.Sale;
import com.pos.system.models.SalePayment;
import com.pos.system.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SalePaymentDAOTest extends BaseDAOTest {

    private SalePaymentDAO paymentDAO;
    private SaleDAO saleDAO;
    private UserDAO userDAO;
    private int testSaleId;

    @BeforeEach
    public void setUp() throws SQLException {
        paymentDAO = new SalePaymentDAO(connection);
        saleDAO = new SaleDAO(connection);
        userDAO = new UserDAO(connection);

        // Preload user and sale
        userDAO.createUser(new User(0, "payuser", "pass", "CASHIER", false));
        userDAO.getUserByUsername("payuser");

        Sale sale = new Sale(1, 1, 100.0, 0.0, LocalDateTime.now());
        testSaleId = saleDAO.insertSale(sale);
    }

    @Test
    public void testCreateAndGetPayments() throws SQLException {
        // Arrange
        SalePayment payment1 = new SalePayment();
        payment1.setSaleId(testSaleId);
        payment1.setPaymentMethod("CASH");
        payment1.setAmount(60.0);
        payment1.setPaymentDate(LocalDateTime.now().minusMinutes(10));

        SalePayment payment2 = new SalePayment();
        payment2.setSaleId(testSaleId);
        payment2.setPaymentMethod("CARD");
        payment2.setAmount(50.0);
        payment2.setPaymentDate(LocalDateTime.now());

        // Act
        paymentDAO.create(payment1);
        paymentDAO.create(payment2);

        List<SalePayment> payments = paymentDAO.findBySaleId(testSaleId);

        // Assert
        assertEquals(2, payments.size());

        // Ordered ASC
        assertEquals("CASH", payments.get(0).getPaymentMethod());
        assertEquals(60.0, payments.get(0).getAmount(), 0.001);

        assertEquals("CARD", payments.get(1).getPaymentMethod());
        assertEquals(50.0, payments.get(1).getAmount(), 0.001);
    }
}
