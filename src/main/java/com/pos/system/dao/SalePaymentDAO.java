package com.pos.system.dao;

import com.pos.system.models.SalePayment;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SalePaymentDAO extends BaseDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SalePaymentDAO(Connection connection) {
        super(connection);
    }

    public SalePaymentDAO() throws SQLException {
        super();
    }

    public SalePayment create(SalePayment payment) throws SQLException {
        String sql = "INSERT INTO sale_payments (sale_id, payment_method, amount, payment_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, payment.getSaleId());
            stmt.setString(2, payment.getPaymentMethod());
            stmt.setDouble(3, payment.getAmount());
            stmt.setString(4, payment.getPaymentDate() != null ? payment.getPaymentDate().format(FORMATTER)
                    : LocalDateTime.now().format(FORMATTER));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    payment.setId(rs.getInt(1));
                }
            }
        }
        return payment;
    }

    public List<SalePayment> findBySaleId(int saleId) throws SQLException {
        List<SalePayment> list = new ArrayList<>();
        String sql = "SELECT * FROM sale_payments WHERE sale_id = ? ORDER BY payment_date ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        }
        return list;
    }

    public List<SalePayment> findBySaleIds(List<Integer> saleIds) throws SQLException {
        List<SalePayment> list = new ArrayList<>();
        if (saleIds == null || saleIds.isEmpty())
            return list;

        // Construct IN clause
        StringBuilder sql = new StringBuilder("SELECT * FROM sale_payments WHERE sale_id IN (");
        for (int i = 0; i < saleIds.size(); i++) {
            sql.append("?");
            if (i < saleIds.size() - 1)
                sql.append(",");
        }
        sql.append(") ORDER BY sale_id, payment_date ASC");

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < saleIds.size(); i++) {
                stmt.setInt(i + 1, saleIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        }
        return list;
    }

    private SalePayment mapResultSetToPayment(ResultSet rs) throws SQLException {
        SalePayment payment = new SalePayment();
        payment.setId(rs.getInt("id"));
        payment.setSaleId(rs.getInt("sale_id"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setAmount(rs.getDouble("amount"));

        String dateStr = rs.getString("payment_date");
        if (dateStr != null)
            payment.setPaymentDate(LocalDateTime.parse(dateStr, FORMATTER));

        return payment;
    }
}
