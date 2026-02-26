package com.pos.system.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private static final Properties properties = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        File file = new File(AppDataUtils.getConfigPath());
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }

    private static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(AppDataUtils.getConfigPath())) {
            properties.store(fos, "Application Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getLanguage() {
        return getProperty("language", "en");
    }

    public static void setLanguage(String language) {
        setProperty("language", language);
    }
}
