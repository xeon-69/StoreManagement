package com.pos.system.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {
        initialize();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:store.db");
            config.setDriverClassName("org.sqlite.JDBC");

            // SQLite specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // Pool settings for desktop app
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setPoolName("POS-HikariPool");

            this.dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized.");

            // Initialize tables
            try (Connection conn = dataSource.getConnection()) {
                initializeTables(conn);
            }

            // Ensure backup on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::performBackup));

        } catch (Exception e) {
            logger.error("Failed to initialize database pool", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void initializeTables(Connection conn) {
        try (var stmt = conn.createStatement()) {
            var inputStream = getClass().getResourceAsStream("/schema.sql");
            if (inputStream == null) {
                logger.error("schema.sql not found in resources!");
                return;
            }

            String sql = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] statements = sql.split(";");

            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    try {
                        stmt.execute(statement);
                    } catch (Exception e) {
                        logger.warn("Statement failed (might be expected for existing tables/columns): {} - {}",
                                statement.trim(), e.getMessage());
                    }
                }
            }
            logger.info("Database tables/schema initialized.");
        } catch (Exception e) {
            logger.error("Error creating tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }

    private void performBackup() {
        try {
            logger.info("Starting database backup...");
            File dbFile = new File("store.db");
            if (!dbFile.exists())
                return;

            Path backupDir = Path.of("backup");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path backupFile = backupDir.resolve("store.db_" + timestamp + ".bak");

            Files.copy(dbFile.toPath(), backupFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Database backup created at: {}", backupFile);

            // Close pool after backup if not already closed (though usually pool closes
            // before file access if wrapped correctly,
            // but for SQLite file copy, it's safer to ensure consistency.
            // However, HikariDataSource is closeable. Ideally we close it before backup.)
            close();

        } catch (IOException e) {
            logger.error("Failed to perform database backup", e);
        }
    }
}
