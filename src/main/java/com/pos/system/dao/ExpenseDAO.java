package com.pos.system.dao;

import com.pos.system.models.Expense;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO extends BaseDAO {

    public ExpenseDAO() throws SQLException {
        super();
    }

    public ExpenseDAO(Connection conn) {
        super(conn);
    }

    public void addExpense(Expense expense) throws SQLException {
        String sql = "INSERT INTO expenses (category, amount, description) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, expense.getCategory());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getDescription());
            pstmt.executeUpdate();
        }
    }

    public List<Expense> getAllExpenses() throws SQLException {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses ORDER BY expense_date DESC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                expenses.add(new Expense(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getTimestamp("expense_date").toLocalDateTime()));
            }
        }
        return expenses;
    }
}
