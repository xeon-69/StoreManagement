package com.pos.system.controllers;

import com.pos.system.dao.SettingsDAO;
import com.pos.system.utils.NotificationUtils;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.Map;

public class SettingsController {

    @FXML
    private TextField storeNameField;
    @FXML
    private TextField currencyField;
    @FXML
    private TextField taxRateField;
    @FXML
    private TextField printerField;

    @FXML
    public void initialize() {
        loadSettings();
    }

    /**
     * Factory method for SettingsDAO to allow overriding in tests.
     */
    protected SettingsDAO getSettingsDAO() throws SQLException {
        return new SettingsDAO();
    }

    private void loadSettings() {
        Task<Map<String, String>> loadTask = new Task<>() {
            @Override
            protected Map<String, String> call() throws Exception {
                try (SettingsDAO dao = getSettingsDAO()) {
                    return dao.getAllSettings();
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            Map<String, String> settings = loadTask.getValue();
            if (settings != null) {
                storeNameField.setText(settings.getOrDefault("store_name", "My Store"));
                currencyField.setText(settings.getOrDefault("currency_symbol", "MMK"));
                taxRateField.setText(settings.getOrDefault("tax_rate", "0"));
                printerField.setText(settings.getOrDefault("printer_id", ""));
            }
        });

        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            NotificationUtils.showError("Database Error", "Failed to load settings.");
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleSaveSettings() {
        String storeName = storeNameField.getText().trim();
        String currency = currencyField.getText().trim();
        String taxRate = taxRateField.getText().trim();
        String printer = printerField.getText().trim();

        if (storeName.isEmpty()) {
            NotificationUtils.showError("Validation Error", "Store name cannot be empty.");
            return;
        }

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (SettingsDAO dao = getSettingsDAO()) {
                    boolean success = true;
                    success &= dao.updateSetting("store_name", storeName);
                    success &= dao.updateSetting("currency_symbol", currency);
                    success &= dao.updateSetting("tax_rate", taxRate);
                    success &= dao.updateSetting("printer_id", printer);
                    return success;
                }
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue()) {
                NotificationUtils.showInfo("Success", "Settings saved successfully.");
            } else {
                NotificationUtils.showError("Error", "Failed to save some settings.");
            }
        });

        saveTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            NotificationUtils.showError("Database Error", "Failed to save settings.");
        });

        new Thread(saveTask).start();
    }
}
