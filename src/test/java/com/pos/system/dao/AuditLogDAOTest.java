package com.pos.system.dao;

import com.pos.system.models.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLogDAOTest extends BaseDAOTest {

    private AuditLogDAO auditLogDAO;

    @BeforeEach
    public void setUp() throws SQLException {
        auditLogDAO = new AuditLogDAO(connection);
    }

    @Test
    public void testCreateAndGetAuditLogs() throws SQLException {
        // Arrange
        AuditLog log1 = new AuditLog();
        log1.setUserId(1); // Assuming user 1 exists (admin)
        log1.setAction("LOGIN");
        log1.setEntityName("User");
        log1.setEntityId("1");
        log1.setDetails("User logged in successfully");
        log1.setCreatedAt(LocalDateTime.now().minusHours(1));

        AuditLog log2 = new AuditLog();
        log2.setUserId(null); // System action demo
        log2.setAction("STARTUP");
        log2.setEntityName("System");
        log2.setEntityId("N/A");
        log2.setDetails("System started");
        log2.setCreatedAt(LocalDateTime.now());

        // Act
        auditLogDAO.create(log1);
        auditLogDAO.create(log2);

        List<AuditLog> recentLogs = auditLogDAO.getRecentLogs(10);

        // Assert
        assertEquals(2, recentLogs.size());

        // Since we order by created_at DESC, log2 should be first
        assertEquals("STARTUP", recentLogs.get(0).getAction());
        assertNull(recentLogs.get(0).getUserId());

        assertEquals("LOGIN", recentLogs.get(1).getAction());
        assertEquals(1, recentLogs.get(1).getUserId());
    }
}
