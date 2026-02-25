package com.pos.system.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerBackupTest {

    private Path backupDir;
    private File testDbFile;

    @BeforeEach
    void setUp() throws IOException {
        backupDir = Path.of("backup");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        // Clean up any existing backups
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
        // Clean up backups after test
        try (Stream<Path> stream = Files.walk(backupDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void testManualBackupSucceedsAndIgnoresPruning() throws IOException {
        DatabaseManager dbm = DatabaseManager.getInstance();

        // Ensure connection is initialized
        assertNotNull(dbm);

        // Perform manual backup
        dbm.performBackup(true);

        // Verify a manual zip file was created
        long count = Files.list(backupDir)
                .filter(p -> p.getFileName().toString().startsWith("store_manual_") && p.toString().endsWith(".zip"))
                .count();
        assertEquals(1, count, "One manual backup should exist");
    }

    @Test
    void testAutoBackupAndPruningLogic() throws IOException, InterruptedException {
        DatabaseManager dbm = DatabaseManager.getInstance();

        // Simulate creating 35 old auto backups
        for (int i = 0; i < 35; i++) {
            Path fakeBackup = backupDir
                    .resolve("store_auto_2000-01-" + String.format("%02d", (i % 28) + 1) + "_12-00" + i + ".zip");
            Files.writeString(fakeBackup, "fake data");
            // Set last modified time sequentially to mimic aging
            Files.setLastModifiedTime(fakeBackup,
                    java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - (35 - i) * 1000));
        }

        // Create 1 manual backup to ensure it survives
        Path manualBackup = backupDir.resolve("store_manual_2000-01-01_12-00.zip");
        Files.writeString(manualBackup, "fake manual data");

        // Perform an auto backup (this should trigger pruning)
        dbm.performBackup(false);

        // Pruning keeps 30 auto backups. We had 35 + 1 new = 36. So 6 should be
        // deleted.
        long autoCount = Files.list(backupDir).filter(p -> p.getFileName().toString().startsWith("store_auto_"))
                .count();
        assertEquals(30, autoCount, "Only 30 auto backups should remain after pruning");

        long manualCount = Files.list(backupDir).filter(p -> p.getFileName().toString().startsWith("store_manual_"))
                .count();
        assertEquals(1, manualCount, "Manual backups should never be pruned");
    }
}
