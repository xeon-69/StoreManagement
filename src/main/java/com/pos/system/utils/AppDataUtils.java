package com.pos.system.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppDataUtils {

    private static final String APP_NAME = "StoreManager";

    /**
     * Returns the base directory for application data.
     * On Windows: %APPDATA%\StoreManager
     * On other OS: ~/.StoreManager
     */
    public static Path getAppDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        Path baseDir;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                baseDir = Paths.get(appData).resolve(APP_NAME);
            } else {
                baseDir = Paths.get(System.getProperty("user.home")).resolve("." + APP_NAME);
            }
        } else {
            baseDir = Paths.get(System.getProperty("user.home")).resolve("." + APP_NAME);
        }

        try {
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
        } catch (IOException e) {
            // Fallback to current directory if AppData is inaccessible
            return Paths.get(".");
        }

        return baseDir;
    }

    public static String getDatabasePath() {
        return getAppDataDir().resolve("store.db").toAbsolutePath().toString();
    }

    public static String getConfigPath() {
        return getAppDataDir().resolve("config.properties").toAbsolutePath().toString();
    }

    public static Path getBackupDir() {
        Path backupDir = getAppDataDir().resolve("backup");
        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
        } catch (IOException e) {
            return getAppDataDir();
        }
        return backupDir;
    }

    public static File getReportsDir() {
        Path reportsDir = getAppDataDir().resolve("reports");
        try {
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
        } catch (IOException e) {
            return getAppDataDir().toFile();
        }
        return reportsDir.toFile();
    }
}
