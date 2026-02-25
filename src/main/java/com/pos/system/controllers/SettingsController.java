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
                taxRateField.setText(settings.getOrDefault("tax_rate", "0"));
                printerField.setText(settings.getOrDefault("printer_id", ""));
            }
        });

        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.dbError"), b.getString("settings.load.fail"));
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleSaveSettings() {
        String storeName = storeNameField.getText().trim();
        String taxRate = taxRateField.getText().trim();
        String printer = printerField.getText().trim();

        if (storeName.isEmpty()) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.validationError"), b.getString("settings.storeName.empty"));
            return;
        }

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (SettingsDAO dao = getSettingsDAO()) {
                    boolean success = true;
                    success &= dao.updateSetting("store_name", storeName);
                    success &= dao.updateSetting("tax_rate", taxRate);
                    success &= dao.updateSetting("printer_id", printer);
                    return success;
                }
            }
        };

        saveTask.setOnSucceeded(e -> {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            if (saveTask.getValue()) {
                com.pos.system.utils.SettingsManager.getInstance().refreshSettings();
                NotificationUtils.showInfo(b.getString("dialog.successTitle"), b.getString("settings.save.success"));
            } else {
                NotificationUtils.showError(b.getString("dialog.errorTitle"), b.getString("settings.save.fail"));
            }
        });

        saveTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.dbError"), b.getString("settings.save.fail"));
        });

        new Thread(saveTask).start();
    }
}
