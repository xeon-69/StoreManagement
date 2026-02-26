package com.pos.system.dao;

import com.pos.system.models.AuditLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO extends BaseDAO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLogDAO(Connection connection) {
        super(connection);
    }

    public AuditLogDAO() throws SQLException {
        super();
    }

    public AuditLog create(AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_name, entity_id, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getUserId() != null) {
                stmt.setInt(1, log.getUserId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, log.getAction());
            stmt.setString(3, log.getEntityName());
            stmt.setString(4, log.getEntityId());
            stmt.setString(5, log.getDetails());
            stmt.setString(6, log.getCreatedAt() != null ? log.getCreatedAt().format(FORMATTER)
                    : LocalDateTime.now().format(FORMATTER));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
        }
        return log;
    }

    public List<AuditLog> getRecentLogs(int limit) throws SQLException {
        return getPaginatedLogs(limit, 0);
    }

    public List<AuditLog> searchLogs(String query) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE action LIKE ? OR entity_name LIKE ? OR details LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        }
        return list;
    }

    public List<AuditLog> getPaginatedLogs(int limit, int offset) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        }
        return list;
    }

    public int getTotalCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM audit_logs";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public void deleteLogsOlderThan(int days) throws SQLException {
        String sql = "DELETE FROM audit_logs WHERE created_at < datetime('now', '-' || ? || ' days')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, days);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                // logger? DAO doesn't have one usually, but we could add it
            }
        }
    }

    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE created_at BETWEEN ? AND ? ORDER BY created_at ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, start.format(FORMATTER));
            stmt.setString(2, end.format(FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        }
        return list;
    }

    private AuditLog mapResultSetToLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));

        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }

        log.setAction(rs.getString("action"));
        log.setEntityName(rs.getString("entity_name"));
        log.setEntityId(rs.getString("entity_id"));
        log.setDetails(rs.getString("details"));

        String dateStr = rs.getString("created_at");
        if (dateStr != null)
            log.setCreatedAt(LocalDateTime.parse(dateStr, FORMATTER));

        return log;
    }
}
