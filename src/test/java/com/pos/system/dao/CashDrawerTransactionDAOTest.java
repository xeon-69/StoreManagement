package com.pos.system.dao;

import com.pos.system.models.CashDrawerTransaction;
import com.pos.system.models.Shift;
import com.pos.system.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CashDrawerTransactionDAOTest extends BaseDAOTest {

    private CashDrawerTransactionDAO trxDAO;
    private ShiftDAO shiftDAO;
    private UserDAO userDAO;
    private int testUserId;
    private int testShiftId;

    @BeforeEach
    public void setUp() throws SQLException {
        trxDAO = new CashDrawerTransactionDAO(connection);
        shiftDAO = new ShiftDAO(connection);
        userDAO = new UserDAO(connection);

        // Preload user and shift
        userDAO.createUser(new User(0, "drawuser", "pass", "CASHIER", false));
        testUserId = userDAO.getUserByUsername("drawuser").getId();

        Shift shift = shiftDAO.create(new Shift(0, testUserId, LocalDateTime.now(), null, 100.0, null, null, "OPEN"));
        testShiftId = shift.getId();
    }

    @Test
    public void testCreateAndGetTransactions() throws SQLException {
        // Arrange
        CashDrawerTransaction trx1 = new CashDrawerTransaction();
        trx1.setShiftId(testShiftId);
        trx1.setUserId(testUserId);
        trx1.setAmount(50.0);
        trx1.setTransactionType("MANUAL_IN");
        trx1.setDescription("Change refill");
        trx1.setTransactionDate(LocalDateTime.now().minusMinutes(30));

        CashDrawerTransaction trx2 = new CashDrawerTransaction();
        trx2.setShiftId(testShiftId);
        trx2.setUserId(testUserId);
        trx2.setAmount(-20.0);
        trx2.setTransactionType("MANUAL_OUT");
        trx2.setDescription("Buy supplies");
        trx2.setTransactionDate(LocalDateTime.now());

        // Act
        trxDAO.create(trx1);
        trxDAO.create(trx2);

        List<CashDrawerTransaction> transactions = trxDAO.findByShiftId(testShiftId);

        // Assert
        assertEquals(2, transactions.size());

        // Ordered ASC securely
        assertEquals(50.0, transactions.get(0).getAmount(), 0.001);
        assertEquals("MANUAL_IN", transactions.get(0).getTransactionType());

        assertEquals(-20.0, transactions.get(1).getAmount(), 0.001);
        assertEquals("MANUAL_OUT", transactions.get(1).getTransactionType());
    }
}
