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

    @FXML
    public void initialize() {
        loadSettings();
    }

    private void loadSettings() {
        Task<Map<String, String>> loadTask = new Task<>() {
            @Override
            protected Map<String, String> call() throws Exception {
                try (SettingsDAO dao = new SettingsDAO()) {
                    return dao.getAllSettings();
                }
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
            protected Boolean call() throws Exception {
                try (SettingsDAO dao = new SettingsDAO()) {
                    boolean s1 = dao.updateSetting("store_name", storeName);
                    boolean s2 = dao.updateSetting("currency_symbol", currency);
                    boolean s3 = dao.updateSetting("tax_rate", taxRate);
                    boolean s4 = dao.updateSetting("printer_id", printer);
                    return s1 && s2 && s3 && s4;
                }
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
