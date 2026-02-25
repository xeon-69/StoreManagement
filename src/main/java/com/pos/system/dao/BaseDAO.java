package com.pos.system.dao;

import com.pos.system.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.pos.system.models.AuditLog;
import com.pos.system.models.User;
import com.pos.system.utils.SessionManager;

public abstract class BaseDAO implements AutoCloseable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Connection connection;
    private boolean connectionOwned = false;

    // For transaction management, pass existing connection
    public BaseDAO(Connection connection) {
        this.connection = connection;
        this.connectionOwned = false;
    }

    // Default constructor uses a new connection from pool
    public BaseDAO() throws SQLException {
        this.connection = DatabaseManager.getInstance().getConnection();
        this.connectionOwned = true;
    }

    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("Error closing ResultSet", e);
            }
        }
    }

    protected void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.error("Error closing Statement", e);
            }
        }
    }

    @Override
    public void close() {
        if (connectionOwned && connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error("Error closing connection in DAO", e);
            }
        }
    }

    protected void logAudit(String action, String entityName, String entityId, String details) {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            AuditLog log = new AuditLog();
            if (currentUser != null) {
                log.setUserId(currentUser.getId());
            }
            log.setAction(action);
            log.setEntityName(entityName);
            log.setEntityId(entityId);
            log.setDetails(details);

            try (AuditLogDAO auditLogDAO = new AuditLogDAO(this.connection)) {
                auditLogDAO.create(log);
            }
        } catch (Exception e) {
            logger.error("Failed to write audit log: " + action + " on " + entityName, e);
        }
    }
}
