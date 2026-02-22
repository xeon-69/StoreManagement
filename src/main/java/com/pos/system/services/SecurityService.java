package com.pos.system.services;

import com.pos.system.dao.AuditLogDAO;
import com.pos.system.models.AuditLog;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class SecurityService {
    private AuditLogDAO auditLogDAO;

    public SecurityService() throws SQLException {
        this.auditLogDAO = new AuditLogDAO();
    }

    public SecurityService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    public String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(10));
    }

    public boolean verifyPassword(String plaintext, String hashed) {
        if (hashed == null || !hashed.startsWith("$2a$")) {
            // Fallback for legacy plain text passwords during migration
            return plaintext != null && plaintext.equals(hashed);
        }
        return BCrypt.checkpw(plaintext, hashed);
    }

    public void logAction(Integer userId, String action, String entityName, String entityId, String details) {
        try {
            AuditLog log = new AuditLog(0, userId, action, entityName, entityId, details, LocalDateTime.now());
            auditLogDAO.create(log);
        } catch (SQLException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}
