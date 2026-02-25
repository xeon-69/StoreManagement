package com.pos.system.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerConcurrencyTest {

    private Path backupDir;
    private File testDbFile;

    @BeforeEach
    void setUp() throws IOException {
        backupDir = Path.of("backup");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        try (Stream<Path> stream = Files.walk(backupDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(backupDir);

        testDbFile = new File("store.db");
        if (!testDbFile.exists()) {
            testDbFile.createNewFile();
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> stream = Files.walk(backupDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void testBackupDuringConcurrentWrites() throws InterruptedException, IOException {
        DatabaseManager dbm = DatabaseManager.getInstance();
        assertNotNull(dbm);

        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        // Define a task that rapidly writes to the database
        Runnable writeTask = () -> {
            try {
                startLatch.await(); // wait for the signal
                try (Connection conn = dbm.getConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement stmt = conn
                            .prepareStatement("INSERT INTO categories (name, description) VALUES (?, ?)")) {
                        for (int i = 0; i < 50; i++) {
                            stmt.setString(1, "TestCat" + Thread.currentThread().getId() + "-" + i);
                            stmt.setString(2, "Desc");
                            stmt.executeUpdate();
                        }
                    }
                    conn.commit();
                } catch (SQLException e) {
                    // Ignore expected constraint violations or locks for this stress test
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        // Start 5 writers
        for (int i = 0; i < 5; i++) {
            executor.submit(writeTask);
        }

        startLatch.countDown(); // Let the writers loose

        // Immediately trigger a backup while writers are going wild
        dbm.performBackup(true);

        doneLatch.await(); // Wait for all writers to finish
        executor.shutdown();

        // Verify that the manual backup still created a zip file safely despite the
        // live writes
        long count = Files.list(backupDir)
                .filter(p -> p.getFileName().toString().startsWith("store_manual_") && p.toString().endsWith(".zip"))
                .count();
        assertEquals(1, count, "One manual backup should exist even when triggered under load");
    }
}
