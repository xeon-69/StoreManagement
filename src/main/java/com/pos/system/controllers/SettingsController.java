package com.pos.system.controllers;

import com.pos.system.dao.SettingsDAO;
import com.pos.system.utils.NotificationUtils;
import javafx.application.Platform;
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

    private SettingsDAO settingsDAO;

    @FXML
    public void initialize() {
        try {
            settingsDAO = new SettingsDAO();
            loadSettings();
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationUtils.showError("Database Error", "Could not initialize Settings.");
        }
    }

    private void loadSettings() {
        Task<Map<String, String>> loadTask = new Task<>() {
            @Override
            protected Map<String, String> call() {
                return settingsDAO.getAllSettings();
            }
        };

        loadTask.setOnSucceeded(e -> {
            Map<String, String> settings = loadTask.getValue();
            storeNameField.setText(settings.getOrDefault("store_name", "My Store"));
            currencyField.setText(settings.getOrDefault("currency_symbol", "MMK"));
            taxRateField.setText(settings.getOrDefault("tax_rate", "0"));
            printerField.setText(settings.getOrDefault("printer_id", ""));
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

        try {
            Double.parseDouble(taxRate); // Validate tax rate is a number
        } catch (NumberFormatException e) {
            NotificationUtils.showError("Validation Error", "Tax Rate must be a valid number.");
            return;
        }

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                boolean s1 = settingsDAO.updateSetting("store_name", storeName);
                boolean s2 = settingsDAO.updateSetting("currency_symbol", currency);
                boolean s3 = settingsDAO.updateSetting("tax_rate", taxRate);
                boolean s4 = settingsDAO.updateSetting("printer_id", printer);
                return s1 && s2 && s3 && s4;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue()) {
                NotificationUtils.showInfo("Success", "Settings saved successfully.");
                // Update global App settings if necessary, e.g. SessionManager context
            } else {
                NotificationUtils.showError("Save Failed", "Could not save all settings.");
            }
        });

        saveTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            NotificationUtils.showError("System Error", "An error occurred while saving settings.");
        });

        new Thread(saveTask).start();
    }
}
