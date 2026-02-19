package com.pos.system.dao;

import com.pos.system.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    // Helper helpers

}
