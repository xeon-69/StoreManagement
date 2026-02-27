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
import java.util.List;

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
            String dbPath = com.pos.system.utils.AppDataUtils.getDatabasePath();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");

            // SQLite specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // Pool settings for desktop app
            config.setMaximumPoolSize(10); // Optimal for SQLite with Hikari
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000); // 5s
            config.setLeakDetectionThreshold(30000);
            config.setPoolName("POS-HikariPool");

            this.dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized.");

            // Initialize WAL mode and optimizations
            try (Connection conn = dataSource.getConnection();
                    var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA mmap_size = 268435456"); // 256MB mmap for faster reads
                stmt.execute("PRAGMA cache_size = -64000"); // 64MB cache
                stmt.execute("PRAGMA busy_timeout = 5000");
                stmt.execute("PRAGMA foreign_keys = ON");

                initializeTables(conn);

                // Run heavy migrations/optimizations in background
                new Thread(() -> {
                    try (Connection bgConn = dataSource.getConnection()) {
                        migrateLegacyStock(bgConn);

                        // Normalize date format
                        try (var bgStmt = bgConn.createStatement()) {
                            bgStmt.executeUpdate(
                                    "UPDATE sales SET sale_date = REPLACE(sale_date, 'T', ' ') WHERE sale_date LIKE '%T%'");
                            logger.info("Background Date normalization completed.");
                        }
                    } catch (SQLException e) {
                        logger.error("Background migration failed", e);
                    }
                }, "DB-Background-Migration").start();
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

    public void performBackup(boolean isManual) {
        try {
            logger.info("Starting database backup...");
            File dbFile = new File(com.pos.system.utils.AppDataUtils.getDatabasePath());
            if (!dbFile.exists())
                return;

            Path backupDir = com.pos.system.utils.AppDataUtils.getBackupDir();
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String prefix = isManual ? "manual_" : "auto_";
            String baseFilename = "store_" + prefix + timestamp;
            Path tempSqliteFile = backupDir.resolve(baseFilename + ".db");
            Path finalZipFile = backupDir.resolve(baseFilename + ".zip");

            // 1. Safe SQLite Online Backup (writes to a temporary .db file first)
            try (Connection conn = dataSource.getConnection();
                    java.sql.Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("backup to '" + tempSqliteFile.toAbsolutePath().toString().replace("\\", "/") + "'");
            } catch (SQLException e) {
                logger.error("SQLite backup command failed", e);
                return;
            }

            // 2. Zip the resulting safe backup file
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(finalZipFile.toFile()));
                    java.io.FileInputStream fis = new java.io.FileInputStream(tempSqliteFile.toFile())) {

                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry("store.db");
                zos.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
                zos.closeEntry();
            }

            // 3. Delete the temporary uncompressed file
            Files.deleteIfExists(tempSqliteFile);
            logger.info("Database backup created and compressed at: {}", finalZipFile);

            // 4. Prune old backups
            if (isManual) {
                pruneBackupsByPrefix(backupDir, "store_manual_", 10);
            } else {
                pruneBackupsByPrefix(backupDir, "store_auto_", 30);
            }

        } catch (IOException e) {
            logger.error("Failed to perform the backup process", e);
        }
    }

    private void performBackup() {
        performBackup(false);
        close();
    }

    private void pruneBackupsByPrefix(Path backupDir, String prefix, int limit) {
        try {
            java.util.stream.Stream<Path> stream = Files.list(backupDir)
                    .filter(path -> path.getFileName().toString().startsWith(prefix)
                            && path.toString().endsWith(".zip"));

            List<Path> backups = stream.sorted((p1, p2) -> {
                try {
                    return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1)); // Newest first
                } catch (IOException e) {
                    return 0;
                }
            }).collect(java.util.stream.Collectors.toList());

            if (backups.size() > limit) {
                for (int i = limit; i < backups.size(); i++) {
                    Path toDelete = backups.get(i);
                    Files.deleteIfExists(toDelete);
                    logger.info("Pruned old backup file: {}", toDelete.getFileName());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to prune backups for prefix: " + prefix, e);
        }
    }

    public void createPreRestoreBackup() throws IOException {
        logger.info("Creating pre-restore safety backup...");
        File dbFile = new File(com.pos.system.utils.AppDataUtils.getDatabasePath());
        if (!dbFile.exists())
            return;

        Path backupDir = com.pos.system.utils.AppDataUtils.getBackupDir();
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path tempSqliteFile = backupDir.resolve("store_pre_restore_" + timestamp + ".db");
        Path finalZipFile = backupDir.resolve("store_pre_restore_" + timestamp + ".zip");

        // Because the pool is closed for restoration, we can safely just copy the file
        // directly
        Files.copy(dbFile.toPath(), tempSqliteFile, StandardCopyOption.REPLACE_EXISTING);

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(finalZipFile.toFile()));
                java.io.FileInputStream fis = new java.io.FileInputStream(tempSqliteFile.toFile())) {

            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry("store.db");
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
        Files.deleteIfExists(tempSqliteFile);
        logger.info("Pre-restore backup successfully created at: {}", finalZipFile);
    }

    public void closeForRestore() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void reinitializeAfterRestore() {
        initialize();
    }
}
