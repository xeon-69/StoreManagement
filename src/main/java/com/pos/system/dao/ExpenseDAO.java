package com.pos.system.dao;

import com.pos.system.models.Expense;

import java.sql.*;
import java.time.LocalDateTime;
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
        String sql = "INSERT INTO expenses (category, amount, description, expense_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, expense.getCategory());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getDescription());
            pstmt.setString(4,
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.executeUpdate();
            logAudit("CREATE", "Expense", expense.getCategory(), "Amount: " + expense.getAmount());
        }
    }

    public void deleteExpense(int id) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logAudit("DELETE", "Expense", String.valueOf(id), "Deleted expense");
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

    public List<Expense> getExpensesBetween(LocalDateTime start, LocalDateTime end) throws SQLException {
        return getPaginatedExpensesBetween(start, end, Integer.MAX_VALUE, 0);
    }

    public List<Expense> getPaginatedExpensesBetween(LocalDateTime start, LocalDateTime end, int limit, int offset)
            throws SQLException {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE expense_date >= ? AND expense_date <= ? ORDER BY expense_date DESC LIMIT ? OFFSET ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, start.toString().replace("T", " "));
            pstmt.setString(2, end.toString().replace("T", " "));
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(new Expense(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("amount"),
                            rs.getString("description"),
                            rs.getTimestamp("expense_date").toLocalDateTime()));
                }
            }
        }
        return expenses;
    }

    public int getExpensesCountBetween(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM expenses WHERE expense_date >= ? AND expense_date <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, start.toString().replace("T", " "));
            pstmt.setString(2, end.toString().replace("T", " "));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public double getTotalExpensesBetween(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expense_date >= ? AND expense_date <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, start.toString().replace("T", " "));
            pstmt.setString(2, end.toString().replace("T", " "));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }
}
