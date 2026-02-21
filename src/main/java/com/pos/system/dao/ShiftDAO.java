package com.pos.system.dao;

import com.pos.system.models.Shift;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ShiftDAO extends BaseDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ShiftDAO(Connection connection) {
        super(connection);
    }

    public ShiftDAO() throws SQLException {
        super();
    }

    public Shift create(Shift shift) throws SQLException {
        String sql = "INSERT INTO shifts (user_id, start_time, opening_cash, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, shift.getUserId());
            stmt.setString(2, shift.getStartTime() != null ? shift.getStartTime().format(FORMATTER)
                    : LocalDateTime.now().format(FORMATTER));
            stmt.setDouble(3, shift.getOpeningCash());
            stmt.setString(4, shift.getStatus() != null ? shift.getStatus() : "OPEN");
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    shift.setId(rs.getInt(1));
                }
            }
        }
        return shift;
    }

    public void update(Shift shift) throws SQLException {
        String sql = "UPDATE shifts SET end_time = ?, expected_closing_cash = ?, actual_closing_cash = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, shift.getEndTime() != null ? shift.getEndTime().format(FORMATTER) : null);
            if (shift.getExpectedClosingCash() != null) {
                stmt.setDouble(2, shift.getExpectedClosingCash());
            } else {
                stmt.setNull(2, Types.REAL);
            }
            if (shift.getActualClosingCash() != null) {
                stmt.setDouble(3, shift.getActualClosingCash());
            } else {
                stmt.setNull(3, Types.REAL);
            }
            stmt.setString(4, shift.getStatus());
            stmt.setInt(5, shift.getId());
            stmt.executeUpdate();
        }
    }

    public Shift findOpenShiftByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM shifts WHERE user_id = ? AND status = 'OPEN' ORDER BY start_time DESC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToShift(rs);
                }
            }
        }
        return null;
    }

    public Shift findById(int id) throws SQLException {
        String sql = "SELECT * FROM shifts WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToShift(rs);
                }
            }
        }
        return null;
    }

    private Shift mapResultSetToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift();
        shift.setId(rs.getInt("id"));
        shift.setUserId(rs.getInt("user_id"));

        String startStr = rs.getString("start_time");
        if (startStr != null)
            shift.setStartTime(LocalDateTime.parse(startStr, FORMATTER));

        String endStr = rs.getString("end_time");
        if (endStr != null)
            shift.setEndTime(LocalDateTime.parse(endStr, FORMATTER));

        shift.setOpeningCash(rs.getDouble("opening_cash"));

        double expected = rs.getDouble("expected_closing_cash");
        if (!rs.wasNull())
            shift.setExpectedClosingCash(expected);

        double actual = rs.getDouble("actual_closing_cash");
        if (!rs.wasNull())
            shift.setActualClosingCash(actual);

        shift.setStatus(rs.getString("status"));
        return shift;
    }
}
