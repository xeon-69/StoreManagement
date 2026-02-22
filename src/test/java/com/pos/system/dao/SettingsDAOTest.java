package com.pos.system.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsDAOTest extends BaseDAOTest {

    private SettingsDAO settingsDAO;

    @BeforeEach
    public void setUp() throws SQLException {
        settingsDAO = new SettingsDAO(connection);
    }

    @Test
    public void testUpdateAndGetSetting() {
        // Arrange
        String key = "store_name";
        String value = "My Super Store";

        // Act
        boolean updated = settingsDAO.updateSetting(key, value);
        String retrievedValue = settingsDAO.getSetting(key, "Default Store");

        // Assert
        assertTrue(updated);
        assertEquals(value, retrievedValue);
    }

    @Test
    public void testGetSettingWithDefault() {
        // Act
        String retrievedValue = settingsDAO.getSetting("non_existent_key", "FallbackValue");

        // Assert
        assertEquals("FallbackValue", retrievedValue);
    }

    @Test
    public void testGetAllSettings() {
        // Arrange
        settingsDAO.updateSetting("tax_rate", "0.08");
        settingsDAO.updateSetting("currency", "USD");

        // Act
        Map<String, String> allSettings = settingsDAO.getAllSettings();

        // Assert
        assertTrue(allSettings.size() >= 2);
        assertEquals("0.08", allSettings.get("tax_rate"));
        assertEquals("USD", allSettings.get("currency"));
    }

    @Test
    public void testUpdateExistingSetting() {
        // Arrange
        settingsDAO.updateSetting("theme", "light");

        // Act
        boolean updated = settingsDAO.updateSetting("theme", "dark");
        String finalValue = settingsDAO.getSetting("theme", "");

        // Assert
        assertTrue(updated);
        assertEquals("dark", finalValue);
    }
}
