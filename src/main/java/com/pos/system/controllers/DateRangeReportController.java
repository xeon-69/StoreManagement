package com.pos.system.controllers;

import com.pos.system.services.ReportingService;
import com.pos.system.utils.NotificationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;

public class DateRangeReportController {

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        // Restrict end date to be after or on start date
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newVal);
            }
        });

        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && startDatePicker.getValue() != null && newVal.isBefore(startDatePicker.getValue())) {
                startDatePicker.setValue(newVal);
            }
        });

        // Add visual restriction in calendar UI
        startDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (endDatePicker.getValue() != null && date.isAfter(endDatePicker.getValue())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #e0e0e0;");
                }
            }
        });

        endDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (startDatePicker.getValue() != null && date.isBefore(startDatePicker.getValue())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #e0e0e0;");
                }
            }
        });
    }

    @FXML
    private void handleGenerate() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            NotificationUtils.showError("report.notify.invalidDates", "report.notify.selectDates");
            return;
        }

        try {
            ReportingService reportingService = new ReportingService();
            File csvFile = reportingService.generateRangeReportCSV(start, end);
            File excelFile = reportingService.generateRangeReportExcel(start, end);

            String msg = String.format(
                    com.pos.system.App.getBundle().getString("report.notify.savedTo") + "\n" +
                            com.pos.system.App.getBundle().getString("report.notify.csv") + "\n" +
                            com.pos.system.App.getBundle().getString("report.notify.excel") + "\n\n" +
                            com.pos.system.App.getBundle().getString("report.notify.folder"),
                    csvFile.getName(), excelFile.getName(), csvFile.getParent());

            NotificationUtils.showSuccess("report.notify.generated", msg);

            handleCancel(); // Close modal
        } catch (Exception e) {
            String errorMsg = String.format(
                    com.pos.system.App.getBundle().getString("report.notify.failMsg"),
                    e.getMessage());
            NotificationUtils.showError("report.notify.failed", errorMsg);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) startDatePicker.getScene().getWindow()).close();
    }
}
