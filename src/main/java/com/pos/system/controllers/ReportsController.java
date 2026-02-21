package com.pos.system.controllers;

import com.pos.system.dao.SaleDAO;
import com.pos.system.database.DatabaseManager;
import com.pos.system.models.Sale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReportsController {

    @FXML
    private Label todaySalesLabel;
    @FXML
    private Label todayProfitLabel;
    @FXML
    private Label totalSalesLabel;

    @FXML
    private ComboBox<String> dateRangeComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TableView<Sale> recentSalesTable;
    @FXML
    private TableColumn<Sale, Integer> idCol;
    @FXML
    private TableColumn<Sale, String> detailsCol;
    @FXML
    private TableColumn<Sale, Double> amountCol;
    @FXML
    private TableColumn<Sale, Double> profitCol;
    @FXML
    private TableColumn<Sale, LocalDateTime> dateCol;

    private SaleDAO saleDAO;

    @FXML
    public void initialize() {
        try {
            saleDAO = new SaleDAO(DatabaseManager.getInstance().getConnection());

            // Setup ComboBox
            dateRangeComboBox.setItems(FXCollections.observableArrayList(
                    com.pos.system.App.getBundle().getString("reports.filter.today"),
                    com.pos.system.App.getBundle().getString("reports.filter.thisWeek"),
                    com.pos.system.App.getBundle().getString("reports.filter.thisMonth"),
                    com.pos.system.App.getBundle().getString("reports.filter.allTime"),
                    com.pos.system.App.getBundle().getString("reports.filter.custom")));

            dateRangeComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                int index = newVal.intValue();
                boolean isCustom = (index == 4);
                startDatePicker.setVisible(isCustom);
                startDatePicker.setManaged(isCustom);
                endDatePicker.setVisible(isCustom);
                endDatePicker.setManaged(isCustom);

                if (!isCustom && index >= 0) {
                    handleFilter(); // Auto filter for presets
                } else if (isCustom && startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                    handleFilter(); // Auto filter if custom dates are already selected
                }
            });

            // Re-eval dates strictly within bounds
            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && endDatePicker.getValue() != null) {
                    if (newVal.isAfter(endDatePicker.getValue())) {
                        startDatePicker.setValue(endDatePicker.getValue());
                    } else {
                        handleFilter();
                    }
                }
            });

            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && startDatePicker.getValue() != null) {
                    if (newVal.isBefore(startDatePicker.getValue())) {
                        endDatePicker.setValue(startDatePicker.getValue());
                    } else {
                        handleFilter();
                    }
                }
            });

            // Format Table
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            detailsCol.setCellValueFactory(new PropertyValueFactory<>("transactionDetails"));
            amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
            profitCol.setCellValueFactory(new PropertyValueFactory<>("totalProfit"));

            // Format Date
            dateCol.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
            dateCol.setCellFactory(column -> new javafx.scene.control.TableCell<Sale, LocalDateTime>() {
                private final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                        .ofPattern("dd-MM-yyyy HH:mm:ss");

                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.format(formatter));
                    }
                }
            });

            // Default triggers initial handleFilter() inherently
            dateRangeComboBox.getSelectionModel().select(0);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFilter() {
        int index = dateRangeComboBox.getSelectionModel().getSelectedIndex();
        LocalDateTime start, end;
        LocalDateTime now = LocalDateTime.now();

        if (index < 0)
            return;

        switch (index) {
            case 0: // Today
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case 1: // This Week
                LocalDate today = now.toLocalDate();
                start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .atStartOfDay();
                end = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(23,
                        59, 59);
                break;
            case 2: // This Month
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            case 3: // All Time
                start = LocalDateTime.of(2000, 1, 1, 0, 0);
                end = now;
                break;
            case 4: // Custom
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    return;
                }
                if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
                    com.pos.system.utils.NotificationUtils.showError(
                            "Invalid Date Range",
                            "Start date cannot be after end date.");
                    return;
                }
                start = startDatePicker.getValue().atStartOfDay();
                end = endDatePicker.getValue().atTime(23, 59, 59);
                break;
            default:
                return;
        }

        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;

        Task<Void> filterTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                double todaySales = saleDAO.getTotalSalesBetween(finalStart, finalEnd);
                double todayProfit = saleDAO.getTotalProfitBetween(finalStart, finalEnd);
                double totalSales = saleDAO.getTotalSalesBetween(LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now());

                java.util.List<Sale> salesData = saleDAO.getSalesBetween(finalStart, finalEnd);

                Platform.runLater(() -> {
                    todaySalesLabel.setText(String.format("%.2f MMK", todaySales));
                    todayProfitLabel.setText(String.format("%.2f MMK", todayProfit));
                    totalSalesLabel.setText(String.format("%.2f MMK", totalSales));
                    recentSalesTable.setItems(FXCollections.observableArrayList(salesData));
                });
                return null;
            }
        };

        filterTask.setOnFailed(e -> e.getSource().getException().printStackTrace());
        new Thread(filterTask).start();
    }
}
