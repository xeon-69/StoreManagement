package com.pos.system.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseDAOTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Connection connection;

    @BeforeEach
    public void setUpDatabase() throws SQLException {
        // Use an in-memory SQLite database
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        connection.setAutoCommit(false); // Enable transactions for testing if needed, or leave true depending on DAO
                                         // usage.
                                         // Most DAO methods use their own transaction blocks or just execute.
        connection.setAutoCommit(true);
        initializeSchema();
    }

    private void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {
            InputStream inputStream = getClass().getResourceAsStream("/schema.sql");
            if (inputStream == null) {
                throw new RuntimeException("schema.sql not found in test or main resources!");
            }

            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] statements = sql.split(";");

            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    try {
                        stmt.execute(statement);
                    } catch (Exception e) {
                        logger.warn("Statement failed (expected for ALTER TABLE on new schema): {} - {}",
                                statement.trim(), e.getMessage());
                    }
                }
            }
            logger.info("In-memory database schema initialized.");
        } catch (Exception e) {
            logger.error("Error creating test tables", e);
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDownDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
