package com.pos.system.dao;

import com.pos.system.models.CashDrawerTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CashDrawerTransactionDAO extends BaseDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CashDrawerTransactionDAO(Connection connection) {
        super(connection);
    }

    public CashDrawerTransactionDAO() throws SQLException {
        super();
    }

    public CashDrawerTransaction create(CashDrawerTransaction trx) throws SQLException {
        String sql = "INSERT INTO cash_drawer_transactions (shift_id, user_id, amount, transaction_type, description, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, trx.getShiftId());
            stmt.setInt(2, trx.getUserId());
            stmt.setDouble(3, trx.getAmount());
            stmt.setString(4, trx.getTransactionType());
            stmt.setString(5, trx.getDescription());
            stmt.setString(6, trx.getTransactionDate() != null ? trx.getTransactionDate().format(FORMATTER)
                    : LocalDateTime.now().format(FORMATTER));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    trx.setId(rs.getInt(1));
                }
            }
        }
        return trx;
    }

    public List<CashDrawerTransaction> findByShiftId(int shiftId) throws SQLException {
        List<CashDrawerTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM cash_drawer_transactions WHERE shift_id = ? ORDER BY transaction_date ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, shiftId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTrx(rs));
                }
            }
        }
        return list;
    }

    private CashDrawerTransaction mapResultSetToTrx(ResultSet rs) throws SQLException {
        CashDrawerTransaction trx = new CashDrawerTransaction();
        trx.setId(rs.getInt("id"));
        trx.setShiftId(rs.getInt("shift_id"));
        trx.setUserId(rs.getInt("user_id"));
        trx.setAmount(rs.getDouble("amount"));
        trx.setTransactionType(rs.getString("transaction_type"));
        trx.setDescription(rs.getString("description"));

        String dateStr = rs.getString("transaction_date");
        if (dateStr != null)
            trx.setTransactionDate(LocalDateTime.parse(dateStr, FORMATTER));

        return trx;
    }
}
