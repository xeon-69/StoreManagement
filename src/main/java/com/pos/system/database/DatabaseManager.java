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
                migrateLegacyStock(conn);
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

    private void migrateLegacyStock(Connection conn) {
        try {
            // Check if batches table exists and is empty
            try (var checkStmt = conn.createStatement();
                    var rs = checkStmt.executeQuery("SELECT count(*) FROM batches")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Already migrated or has data
                    return;
                }
            } catch (Exception e) {
                // Table might not exist yet if schema failed, or just ignore
                return;
            }

            // Migrate positive stock products
            String selectProducts = "SELECT id, stock, cost_price FROM products WHERE stock > 0";
            String insertBatch = "INSERT INTO batches (product_id, batch_number, cost_price, remaining_quantity) VALUES (?, ?, ?, ?)";
            String insertTx = "INSERT INTO inventory_transactions (product_id, batch_id, quantity_change, transaction_type, reference_id) VALUES (?, ?, ?, ?, ?)";

            try (var pstmtSelect = conn.prepareStatement(selectProducts);
                    var rsProducts = pstmtSelect.executeQuery();
                    var pstmtBatch = conn.prepareStatement(insertBatch, java.sql.Statement.RETURN_GENERATED_KEYS);
                    var pstmtTx = conn.prepareStatement(insertTx)) {

                conn.setAutoCommit(false);
                int migrationCount = 0;

                while (rsProducts.next()) {
                    int productId = rsProducts.getInt("id");
                    int stock = rsProducts.getInt("stock");
                    double costPrice = rsProducts.getDouble("cost_price");

                    // 1. Create Batch
                    pstmtBatch.setInt(1, productId);
                    pstmtBatch.setString(2, "INIT-MIGRATE");
                    pstmtBatch.setDouble(3, costPrice);
                    pstmtBatch.setInt(4, stock);
                    pstmtBatch.executeUpdate();

                    int batchId = -1;
                    try (var keys = pstmtBatch.getGeneratedKeys()) {
                        if (keys.next())
                            batchId = keys.getInt(1);
                    }

                    // 2. Create Transaction
                    pstmtTx.setInt(1, productId);
                    pstmtTx.setInt(2, batchId);
                    pstmtTx.setInt(3, stock);
                    pstmtTx.setString(4, "ADJUSTMENT");
                    pstmtTx.setString(5, "SYSTEM-MIGRATION");
                    pstmtTx.executeUpdate();

                    migrationCount++;
                }
                conn.commit();
                if (migrationCount > 0) {
                    logger.info("Successfully migrated {} legacy stock items to inventory ledger.", migrationCount);
                }
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to migrate legacy stock", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            logger.error("Legacy stock migration check failed", e);
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
