package com.pos.system.controllers;

import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.models.SalePayment;
import com.pos.system.models.Sale;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.pos.system.utils.NotificationUtils;

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
    private TableColumn<Sale, Double> subtotalCol;
    @FXML
    private TableColumn<Sale, Double> discountCol;
    @FXML
    private TableColumn<Sale, Double> taxCol;
    @FXML
    private TableColumn<Sale, Double> amountCol;
    @FXML
    private TableColumn<Sale, Double> profitCol;
    @FXML
    private TableColumn<Sale, String> paymentCol;
    @FXML
    private TableColumn<Sale, LocalDateTime> dateCol;
    @FXML
    private TableColumn<Sale, Void> actionCol;

    @FXML
    public void initialize() {
        try {
            // Setup ComboBox items
            dateRangeComboBox.setItems(FXCollections.observableArrayList(
                    com.pos.system.App.getBundle().getString("reports.filter.today"),
                    com.pos.system.App.getBundle().getString("reports.filter.thisWeek"),
                    com.pos.system.App.getBundle().getString("reports.filter.thisMonth"),
                    com.pos.system.App.getBundle().getString("reports.filter.allTime"),
                    com.pos.system.App.getBundle().getString("reports.filter.custom")));

            // Listen for filter changes
            dateRangeComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                int index = newVal.intValue();
                boolean isCustom = (index == 4);
                startDatePicker.setVisible(isCustom);
                startDatePicker.setManaged(isCustom);
                endDatePicker.setVisible(isCustom);
                endDatePicker.setManaged(isCustom);

                if (!isCustom && index >= 0) {
                    handleFilter();
                } else if (isCustom && startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                    handleFilter();
                }
            });

            // Date picker constraints — disable invalid dates in the calendar UI
            final LocalDate today = LocalDate.now();

            startDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    // Disable future dates and dates after end date
                    LocalDate maxDate = endDatePicker.getValue() != null
                            ? (endDatePicker.getValue().isBefore(today) ? endDatePicker.getValue() : today)
                            : today;
                    if (date.isAfter(maxDate)) {
                        setDisable(true);
                        setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            });

            endDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    // Disable future dates and dates before start date
                    LocalDate minDate = startDatePicker.getValue();
                    if (date.isAfter(today) || (minDate != null && date.isBefore(minDate))) {
                        setDisable(true);
                        setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            });

            // Re-filter when date values change
            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && endDatePicker.getValue() != null) {
                    handleFilter();
                }
            });

            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && startDatePicker.getValue() != null) {
                    handleFilter();
                }
            });

            // Setup table columns
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            detailsCol.setCellValueFactory(new PropertyValueFactory<>("transactionDetails"));
            subtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
            discountCol.setCellValueFactory(new PropertyValueFactory<>("discountAmount"));
            taxCol.setCellValueFactory(new PropertyValueFactory<>("taxAmount"));
            amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
            profitCol.setCellValueFactory(new PropertyValueFactory<>("totalProfit"));
            paymentCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethods"));

            formatCurrencyColumn(subtotalCol);
            formatCurrencyColumn(discountCol);
            formatCurrencyColumn(taxCol);
            formatCurrencyColumn(amountCol);
            formatCurrencyColumn(profitCol);

            // Date column formatter
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

            setupActionColumn();

            // Select default filter (triggers handleFilter via listener)
            dateRangeComboBox.getSelectionModel().select(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<Sale, Void>() {
            private final javafx.scene.control.Button detailsBtn = new javafx.scene.control.Button("Details");

            {
                detailsBtn.getStyleClass().add("btn-primary");
                detailsBtn.setOnAction(event -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleViewDetails(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(detailsBtn);
                    pane.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });
    }

    private void handleViewDetails(Sale sale) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/transaction_details_modal.fxml"));
            loader.setResources(com.pos.system.App.getBundle());
            javafx.scene.Parent root = loader.load();

            TransactionDetailsController controller = loader.getController();
            controller.setSale(sale);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Transaction Details");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            NotificationUtils.showError("Error", "Could not load transaction details.");
        }
    }

    /**
     * Runs the filter query SYNCHRONOUSLY on the JavaFX thread.
     * SQLite queries on a local DB are fast enough for this.
     * This eliminates all race conditions from background threads.
     */
    @FXML
    private void handleFilter() {
        int index = dateRangeComboBox.getSelectionModel().getSelectedIndex();
        if (index < 0)
            return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end;

        switch (index) {
            case 0: // Today
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case 1: // This Week
                LocalDate today = now.toLocalDate();
                start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .atStartOfDay();
                end = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                        .atTime(23, 59, 59);
                break;
            case 2: // This Month
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                        .atTime(23, 59, 59);
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
                    NotificationUtils.showError("Invalid Date Range", "Start date cannot be after end date.");
                    return;
                }
                start = startDatePicker.getValue().atStartOfDay();
                end = endDatePicker.getValue().atTime(23, 59, 59);
                break;
            default:
                return;
        }

        // Query synchronously — fast on local SQLite
        try (SaleDAO saleDAO = new SaleDAO(); SalePaymentDAO paymentDAO = new SalePaymentDAO()) {
            double filteredSales = saleDAO.getTotalSalesBetween(start, end);
            double filteredProfit = saleDAO.getTotalProfitBetween(start, end);
            double totalSales = saleDAO.getTotalSalesBetween(
                    LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now());

            List<Sale> salesData = saleDAO.getSalesBetween(start, end);

            // Populate payment summary for each sale
            for (Sale sale : salesData) {
                try {
                    List<SalePayment> payments = paymentDAO.findBySaleId(sale.getId());
                    if (!payments.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (SalePayment p : payments) {
                            if (sb.length() > 0)
                                sb.append(", ");
                            sb.append(p.getPaymentMethod())
                                    .append(" (").append(String.format("%,.0f", p.getAmount())).append(")");
                        }
                        double totalPaid = payments.stream().mapToDouble(SalePayment::getAmount).sum();
                        double change = totalPaid - sale.getTotalAmount();
                        if (change > 0) {
                            sb.append(" | Change: ").append(String.format("%,.0f", change));
                        }
                        sale.setPaymentMethods(sb.toString());
                    }
                } catch (Exception ignored) {
                }
            }

            // Update UI directly (we're already on the FX thread)
            todaySalesLabel.setText(String.format("%,.2f MMK", filteredSales));
            todayProfitLabel.setText(String.format("%,.2f MMK", filteredProfit));
            totalSalesLabel.setText(String.format("%,.2f MMK", totalSales));
            recentSalesTable.setItems(FXCollections.observableArrayList(salesData));

        } catch (Exception e) {
            e.printStackTrace();
            NotificationUtils.showError("Error", "Failed to load sales data.");
        }
    }

    @FXML
    private void handleGenerateZReport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/date_range_report_modal.fxml"));
            loader.setResources(com.pos.system.App.getBundle()); // Added missing resource bundle
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(com.pos.system.App.getBundle().getString("report.modal.title")); // Localize title
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            NotificationUtils.showError("UI Error", "Could not open date range selector: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void formatCurrencyColumn(TableColumn<Sale, Double> column) {
        column.setCellFactory(col -> new javafx.scene.control.TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.2f", item));
                }
            }
        });
    }
}
