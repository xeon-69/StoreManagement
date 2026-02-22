package com.pos.system.dao;

import com.pos.system.models.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExpenseDAOTest extends BaseDAOTest {

    private ExpenseDAO expenseDAO;

    @BeforeEach
    public void setUp() throws SQLException {
        expenseDAO = new ExpenseDAO(connection);
    }

    @Test
    public void testAddAndGetAllExpenses() throws SQLException {
        // Arrange
        Expense exp1 = new Expense(0, "Office Supplies", 50.0, "Pens and Paper", LocalDateTime.now());
        Expense exp2 = new Expense(0, "Utilities", 150.0, "Electricity Bill", LocalDateTime.now().minusDays(1));

        // Act
        expenseDAO.addExpense(exp1);
        expenseDAO.addExpense(exp2);

        List<Expense> expenses = expenseDAO.getAllExpenses();

        // Assert
        assertEquals(2, expenses.size());

        // Ordered by expense_date DESC
        assertEquals("Office Supplies", expenses.get(0).getCategory());
        assertEquals(50.0, expenses.get(0).getAmount(), 0.001);

        assertEquals("Utilities", expenses.get(1).getCategory());
        assertEquals(150.0, expenses.get(1).getAmount(), 0.001);
    }
}
