package com.pos.system.controllers;

import com.pos.system.utils.PDFReportGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;

public class ReportsController {

    @FXML
    private Label statusLabel;

    @FXML
    private void exportInventory() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Inventory Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("inventory_report.pdf");

        File file = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (file != null) {
            PDFReportGenerator.generateInventoryReport(file.getAbsolutePath());
            statusLabel.setText("Report saved to: " + file.getAbsolutePath());
        }
    }
}
