package com.pos.system.utils;

import com.pos.system.dao.SettingsDAO;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsManager {
    private static SettingsManager instance;
    private final Map<String, String> settingsCache = new ConcurrentHashMap<>();

    private SettingsManager() {
        refreshSettings();
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    public void refreshSettings() {
        try (SettingsDAO dao = new SettingsDAO()) {
            Map<String, String> settings = dao.getAllSettings();
            settingsCache.clear();
            settingsCache.putAll(settings);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getSetting(String key, String defaultValue) {
        return settingsCache.getOrDefault(key, defaultValue);
    }

    public String getCurrencySymbol() {
        return com.pos.system.App.getBundle().getString("common.mmk");
    }

    public double getTaxRate() {
        try {
            return Double.parseDouble(getSetting("tax_rate", "0"));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public String getStoreName() {
        return getSetting("store_name", "General Store");
    }
}
