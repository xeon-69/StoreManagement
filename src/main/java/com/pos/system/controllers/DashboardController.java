package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import java.util.Locale;

import java.io.IOException;

import javafx.scene.control.Button;

public class DashboardController {

    private static String currentActiveView = null;
    // View cache to avoid re-creating controllers + connections on every page
    // switch
    private final java.util.Map<String, Parent> viewCache = new java.util.HashMap<>();

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label currentUserLabel;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private Button navAuditBtn;

    @FXML
    private Button navUsersBtn;

    @FXML
    private Button navSettingsBtn;

    @FXML
    private Label shiftTotalLabel;

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().isLoggedIn()) {
            currentUserLabel.setText(
                    getString("nav.user", "User: ") + SessionManager.getInstance().getCurrentUser().getUsername());

            // Only admin users can see Audit Logs and Users Management
            if (!SessionManager.getInstance().getCurrentUser().isAdmin()) {
                navAuditBtn.setVisible(false);
                navAuditBtn.setManaged(false);
                navUsersBtn.setVisible(false);
                navUsersBtn.setManaged(false);
                navSettingsBtn.setVisible(false);
                navSettingsBtn.setManaged(false);
            }
        }

        languageComboBox.getItems().addAll("English", "မြန်မာစာ", "中文");

        Locale current = com.pos.system.App.getBundle().getLocale();
        if ("my".equals(current.getLanguage())) {
            languageComboBox.setValue("မြန်မာစာ");
        } else if ("zh".equals(current.getLanguage())) {
            languageComboBox.setValue("中文");
        } else {
            languageComboBox.setValue("English");
        }

        languageComboBox.setOnAction(e -> {
            String selected = languageComboBox.getValue();
            if ("English".equals(selected)) {
                com.pos.system.App.setLocale("en");
            } else if ("မြန်မာစာ".equals(selected)) {
                com.pos.system.App.setLocale("my");
            } else if ("中文".equals(selected)) {
                com.pos.system.App.setLocale("zh");
            }
        });

        if (currentActiveView != null) {
            loadView(currentActiveView);
        }

        // Shift Analytics - show current shift total if shift is active
        if (SessionManager.getInstance().getCurrentShift() != null) {
            try {
                com.pos.system.services.ShiftAnalytics analytics = new com.pos.system.services.ShiftAnalytics();
                double shiftTotal = analytics.getCurrentShiftTotal();
                shiftTotalLabel.setText(getString("dashboard.shiftTotal", "Current Shift Sales: ")
                        + String.format("%,.2f MMK", shiftTotal));
                shiftTotalLabel.setVisible(true);
                shiftTotalLabel.setManaged(true);
            } catch (Exception e) {
                // Silently fail if analytics can't be loaded
            }
        }
    }

    private String getString(String key, String defaultVal) {
        try {
            return com.pos.system.App.getBundle().getString(key);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @FXML
    private void showPOS() {
        loadView("pos");
    }

    @FXML
    private void showInventory() {
        loadView("inventory");
    }

    @FXML
    private void showCategories() {
        loadView("categories");
    }

    @FXML
    private void showFinance() {
        loadView("finance");
    }

    @FXML
    private void showReports() {
        loadView("reports");
    }

    @FXML
    private void showCashDrawer() {
        loadView("cash_drawer");
    }

    @FXML
    private void showAuditLogs() {
        loadView("audit_logs");
    }

    @FXML
    private void showUsers() {
        loadView("users");
    }

    @FXML
    private void showSettings() {
        loadView("settings");
    }

    @FXML
    private void logout() throws IOException {
        SessionManager.getInstance().logout();
        currentActiveView = null;
        viewCache.clear(); // Clear cached views on logout
        App.setRoot("login");
    }

    private void loadView(String fxml) {
        currentActiveView = fxml;

        // Pages with live data should always reload fresh
        boolean shouldCache = !fxml.equals("reports") && !fxml.equals("inventory")
                && !fxml.equals("cash_drawer") && !fxml.equals("pos") && !fxml.equals("finance");

        if (shouldCache) {
            Parent cached = viewCache.get(fxml);
            if (cached != null) {
                mainPane.setCenter(cached);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
            loader.setResources(App.getBundle());
            Parent view = loader.load();
            if (shouldCache) {
                viewCache.put(fxml, view);
            }
            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Failed to load view: " + fxml);
            mainPane.setCenter(errorLabel);
        }
    }
}
