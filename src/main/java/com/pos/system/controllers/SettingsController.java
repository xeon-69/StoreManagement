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
    private javafx.scene.control.Label backupStorageLabel;
    @FXML
    private javafx.scene.control.Label backupWarningLabel;
    @FXML
    private javafx.scene.control.TableView<BackupFile> backupTable;
    @FXML
    private javafx.scene.control.TableColumn<BackupFile, String> colBackupName;
    @FXML
    private javafx.scene.control.TableColumn<BackupFile, String> colBackupDate;
    @FXML
    private javafx.scene.control.TableColumn<BackupFile, String> colBackupType;
    @FXML
    private javafx.scene.control.TableColumn<BackupFile, String> colBackupSize;
    @FXML
    private javafx.scene.control.Button restoreButton;

    @FXML
    public void initialize() {
        loadSettings();
        setupBackupTable();
        loadBackups();
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

    // --- Backup Management ---
    public static class BackupFile {
        private final String name;
        private final String date;
        private final String type;
        private final String size;
        private final java.nio.file.Path path;

        public BackupFile(String name, String date, String type, String size, java.nio.file.Path path) {
            this.name = name;
            this.date = date;
            this.type = type;
            this.size = size;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public String getDate() {
            return date;
        }

        public String getType() {
            return type;
        }

        public String getSize() {
            return size;
        }

        public java.nio.file.Path getPath() {
            return path;
        }
    }

    private void setupBackupTable() {
        colBackupName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        colBackupDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        colBackupType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        colBackupSize.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("size"));

        backupTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            restoreButton.setDisable(newSel == null);
        });
    }

    private void loadBackups() {
        Task<java.util.List<BackupFile>> loadBackupsTask = new Task<>() {
            @Override
            protected java.util.List<BackupFile> call() throws Exception {
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                java.nio.file.Path backupDir = java.nio.file.Path.of("backup");
                if (!java.nio.file.Files.exists(backupDir))
                    return java.util.Collections.emptyList();

                return java.nio.file.Files.list(backupDir)
                        .filter(p -> p.toString().endsWith(".zip"))
                        .map(p -> {
                            try {
                                String name = p.getFileName().toString();
                                java.nio.file.attribute.BasicFileAttributes attr = java.nio.file.Files.readAttributes(p,
                                        java.nio.file.attribute.BasicFileAttributes.class);
                                String date = java.time.LocalDateTime
                                        .ofInstant(attr.lastModifiedTime().toInstant(),
                                                java.time.ZoneId.systemDefault())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                                String typeKey = "settings.backup.auto";
                                if (name.contains("manual"))
                                    typeKey = "settings.backup.manual";
                                if (name.contains("pre_restore"))
                                    typeKey = "settings.backup.preRestore";
                                String type = b.getString(typeKey);

                                double sizeMB = attr.size() / (1024.0 * 1024.0);
                                String sizeStr = String.format("%.2f MB", sizeMB);
                                return new BackupFile(name, date, type, sizeStr, p);
                            } catch (java.io.IOException e) {
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .sorted((b1, b2) -> b2.getDate().compareTo(b1.getDate())) // Newest first
                        .collect(java.util.stream.Collectors.toList());
            }
        };

        loadBackupsTask.setOnSucceeded(e -> {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            java.util.List<BackupFile> backups = loadBackupsTask.getValue();
            backupTable.setItems(javafx.collections.FXCollections.observableArrayList(backups));

            // Calculate total storage
            try {
                java.nio.file.Path backupDir = java.nio.file.Path.of("backup");
                if (java.nio.file.Files.exists(backupDir)) {
                    long totalBytes = java.nio.file.Files.walk(backupDir)
                            .filter(java.nio.file.Files::isRegularFile)
                            .mapToLong(p -> {
                                try {
                                    return java.nio.file.Files.size(p);
                                } catch (java.io.IOException ex) {
                                    return 0L;
                                }
                            })
                            .sum();

                    double totalMB = totalBytes / (1024.0 * 1024.0);

                    // Basic disk space check (assuming the root of the backup dir)
                    long usableSpace = backupDir.toFile().getUsableSpace();
                    long totalSpace = backupDir.toFile().getTotalSpace();
                    double percentUsed = ((double) (totalSpace - usableSpace) / totalSpace) * 100;

                    backupStorageLabel
                            .setText(String.format(b.getString("settings.backup.storage"), totalMB, percentUsed));

                    if (percentUsed >= 80.0) {
                        backupWarningLabel.setVisible(true);
                        backupWarningLabel.setManaged(true);
                    } else {
                        backupWarningLabel.setVisible(false);
                        backupWarningLabel.setManaged(false);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        new Thread(loadBackupsTask).start();
    }

    @FXML
    private void handleCreateManualBackup() {
        Task<Void> backupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                com.pos.system.database.DatabaseManager.getInstance().performBackup(true);
                return null;
            }
        };

        backupTask.setOnSucceeded(e -> {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showInfo(
                    b.getString("settings.backup.create.successTitle"),
                    b.getString("settings.backup.create.successMsg"));
            loadBackups();
        });

        backupTask.setOnFailed(e -> {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(
                    b.getString("settings.backup.create.failTitle"),
                    b.getString("settings.backup.create.failMsg"));
        });

        new Thread(backupTask).start();
    }

    @FXML
    private void handleRestoreBackup() {
        BackupFile selected = backupTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        java.util.ResourceBundle b = com.pos.system.App.getBundle();

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(b.getString("settings.backup.restore.confirmTitle"));
        alert.setHeaderText(b.getString("settings.backup.restore.confirmHeader"));
        alert.setContentText(String.format(b.getString("settings.backup.restore.confirmContent"), selected.getName(),
                selected.getDate()));

        // Optional: Apply custom CSS to the default Alert if needed to match the app
        // theme
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {

            // Show a progress dialog while restoring
            javafx.scene.control.Alert progressDialog = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            progressDialog.setTitle(b.getString("settings.backup.restore.progressTitle"));
            progressDialog.setHeaderText(b.getString("settings.backup.restore.progressHeader"));
            progressDialog.setContentText(
                    String.format(b.getString("settings.backup.restore.progressContent"), selected.getName()));
            progressDialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK).setDisable(true);
            progressDialog.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/css/styles.css").toExternalForm());
            progressDialog.getDialogPane().getStyleClass().add("custom-alert");
            progressDialog.show();

            Task<Void> restoreTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    com.pos.system.database.DatabaseManager dbm = com.pos.system.database.DatabaseManager.getInstance();

                    // 1. Close active connections
                    dbm.closeForRestore();

                    try {
                        // 2. Create Safety Copy
                        dbm.createPreRestoreBackup();

                        // 3. Extract the selected zip backup and replace store.db
                        java.io.File storeDb = new java.io.File("store.db");
                        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                                new java.io.FileInputStream(selected.getPath().toFile()))) {
                            java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
                            if (zipEntry != null) {
                                // Extract and overwrite
                                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(storeDb)) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                            }
                            zis.closeEntry();
                        }
                    } finally {
                        // 4. Restart database connection pool
                        dbm.reinitializeAfterRestore();
                    }
                    return null;
                }
            };

            restoreTask.setOnSucceeded(e -> {
                progressDialog.close();
                NotificationUtils.showInfo(
                        b.getString("settings.backup.restore.successTitle"),
                        b.getString("settings.backup.restore.successMsg"));
                loadBackups(); // Refresh list to show the new pre-restore backup
            });

            restoreTask.setOnFailed(e -> {
                progressDialog.close();
                e.getSource().getException().printStackTrace();
                NotificationUtils.showError(
                        b.getString("settings.backup.restore.failTitle"),
                        b.getString("settings.backup.restore.failMsg"));
                // Try to reconnect anyway just in case it failed before breaking things
                com.pos.system.database.DatabaseManager.getInstance().reinitializeAfterRestore();
            });

            new Thread(restoreTask).start();
        }
    }
}
