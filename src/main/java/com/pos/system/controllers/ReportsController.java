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
import javafx.scene.chart.*;
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
    private javafx.scene.layout.HBox analysisFiltersBox;
    @FXML
    private ComboBox<String> analysisYearComboBox;
    @FXML
    private ComboBox<String> analysisViewComboBox;
    @FXML
    private Label analysisYearLabel;
    @FXML
    private Label analysisViewLabel;

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
    private TableColumn<Sale, Double> changeCol;
    @FXML
    private TableColumn<Sale, Void> actionCol;
    @FXML
    private Pagination pagination;

    @FXML
    private LineChart<String, Number> revenueLineChart;
    @FXML
    private BarChart<String, Number> categoryBarChart;
    @FXML
    private PieChart paymentPieChart;
    @FXML
    private StackedBarChart<String, Number> profitStackedBarChart;
    @FXML
    private javafx.scene.control.ScrollPane revenueScrollPane;
    @FXML
    private javafx.scene.control.ScrollPane profitScrollPane;

    private static final int ROWS_PER_PAGE = 30;
    private List<Sale> allFilteredSales = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        try {
            // Setup ComboBox items
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            dateRangeComboBox.setItems(FXCollections.observableArrayList(
                    b.getString("reports.filter.today"),
                    b.getString("reports.filter.thisWeek"),
                    b.getString("reports.filter.thisMonth"),
                    b.getString("reports.filter.thisYear"),
                    b.getString("reports.filter.allTime"),
                    b.getString("reports.filter.custom")));

            analysisViewComboBox.setItems(FXCollections.observableArrayList(
                    b.getString("reports.analysis.daily"),
                    b.getString("reports.analysis.monthly")));
            analysisViewComboBox.getSelectionModel().selectFirst();

            // Populate year combo box (from 2020 to current year)
            int currentYear = LocalDate.now().getYear();
            List<String> years = new java.util.ArrayList<>();
            years.add(b.getString("reports.filter.allTime")); // Default "All"
            for (int y = currentYear; y >= 2020; y--) {
                years.add(String.valueOf(y));
            }
            analysisYearComboBox.setItems(FXCollections.observableArrayList(years));
            analysisYearComboBox.getSelectionModel().selectFirst();

            // Listen for filter changes
            dateRangeComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                int index = newVal.intValue();
                boolean isCustom = (index == 5); // 5 is Custom now
                startDatePicker.setVisible(isCustom);
                startDatePicker.setManaged(isCustom);
                endDatePicker.setVisible(isCustom);
                endDatePicker.setManaged(isCustom);

                // Toggle analysis filters
                boolean showAnalysisFilters = (index == 3 || index == 4); // "This Year" or "All Time"
                analysisFiltersBox.setVisible(showAnalysisFilters);
                analysisFiltersBox.setManaged(showAnalysisFilters);

                boolean showYearCombo = (index == 4); // "All Time"
                analysisYearLabel.setVisible(showYearCombo);
                analysisYearLabel.setManaged(showYearCombo);
                analysisYearComboBox.setVisible(showYearCombo);
                analysisYearComboBox.setManaged(showYearCombo);

                if (!isCustom && index >= 0) {
                    handleFilter();
                } else if (isCustom && startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                    handleFilter();
                }
            });

            // Listeners for analysis specific combos
            analysisViewComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null)
                    handleFilter(); // Re-render charts with new grouping
            });

            analysisYearComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null)
                    handleFilter(); // Re-render charts with new year filter
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
            changeCol.setCellValueFactory(new PropertyValueFactory<>("change"));
            paymentCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethods"));

            formatCurrencyColumn(subtotalCol);
            formatCurrencyColumn(discountCol);
            formatCurrencyColumn(taxCol);
            formatCurrencyColumn(amountCol);
            formatCurrencyColumn(profitCol);
            formatCurrencyColumn(changeCol);

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

            // Pagination setup
            pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
                updatePage(newIndex.intValue());
            });

            // Select default filter (triggers handleFilter via listener)
            dateRangeComboBox.getSelectionModel().select(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<Sale, Void>() {
            private final javafx.scene.control.Button detailsBtn = new javafx.scene.control.Button(
                    com.pos.system.App.getBundle().getString("reports.details.btn"));

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
            stage.setTitle(com.pos.system.App.getBundle().getString("reports.details.title"));
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            NotificationUtils.showError(com.pos.system.App.getBundle().getString("dialog.error"),
                    com.pos.system.App.getBundle().getString("reports.details.loadError"));
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
                end = now;
                break;
            case 1: // This Week
                start = now.toLocalDate().minusDays(6).atStartOfDay();
                end = now;
                break;
            case 2: // This Month
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now;
                break;
            case 3: // This Year
                start = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                end = now;
                break;
            case 4: // All Time
                start = LocalDateTime.of(2000, 1, 1, 0, 0);
                end = now;
                break;
            case 5: // Custom
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    return;
                }
                if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
                    java.util.ResourceBundle b = com.pos.system.App.getBundle();
                    NotificationUtils.showError(b.getString("reports.filter.invalidRange"),
                            b.getString("reports.filter.invalidRangeMsg"));
                    return;
                }
                start = startDatePicker.getValue().atStartOfDay();
                if (!endDatePicker.getValue().isBefore(now.toLocalDate())) {
                    end = now;
                } else {
                    end = endDatePicker.getValue().atTime(23, 59, 59);
                }
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
                        sale.setChange(Math.max(0, change));
                        sale.setPaymentMethods(sb.toString());
                    }
                } catch (Exception ignored) {
                }
            }

            // Update UI directly (we're already on the FX thread)
            String mmk = com.pos.system.App.getBundle().getString("common.mmk");
            todaySalesLabel.setText(String.format("%,.2f %s", filteredSales, mmk));
            todayProfitLabel.setText(String.format("%,.2f %s", filteredProfit, mmk));
            totalSalesLabel.setText(String.format("%,.2f %s", totalSales, mmk));

            this.allFilteredSales = salesData;
            int pageCount = (int) Math.ceil((double) allFilteredSales.size() / ROWS_PER_PAGE);
            pagination.setPageCount(Math.max(1, pageCount));
            pagination.setCurrentPageIndex(0);
            updatePage(0);
            updateCharts(salesData, start, end);

        } catch (Exception e) {
            e.printStackTrace();
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.error"), b.getString("reports.load.error"));
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
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.uiError"),
                    String.format(b.getString("reports.modal.uiError"), e.getMessage()));
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

    private void updatePage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allFilteredSales.size());

        if (fromIndex >= allFilteredSales.size() && !allFilteredSales.isEmpty()) {
            recentSalesTable.setItems(FXCollections.observableArrayList());
        } else if (allFilteredSales.isEmpty()) {
            recentSalesTable.setItems(FXCollections.observableArrayList());
        } else {
            recentSalesTable.setItems(FXCollections.observableArrayList(allFilteredSales.subList(fromIndex, toIndex)));
        }
    }

    private void updateCharts(List<Sale> data, LocalDateTime start, LocalDateTime end) {
        if (data == null)
            return;

        java.util.ResourceBundle b = com.pos.system.App.getBundle();

        // Determine grouping strategy based on filter
        int filterIndex = dateRangeComboBox.getSelectionModel().getSelectedIndex();
        int viewIndex = analysisViewComboBox.getSelectionModel().getSelectedIndex();
        boolean groupMonthly = false;
        boolean groupHourly = (filterIndex == 0); // Today -> Hourly

        if (filterIndex == 3 || filterIndex == 4) { // This Year or All Time
            groupMonthly = (viewIndex == 1); // User selected "Monthly"
        }

        // Filter by selected year if "All Time" is selected
        LocalDateTime effectiveStart = start;
        LocalDateTime effectiveEnd = end;
        if (filterIndex == 4) {
            String selectedYear = analysisYearComboBox.getValue();
            if (selectedYear != null && !selectedYear.equals(b.getString("reports.filter.allTime"))) {
                int y = Integer.parseInt(selectedYear);
                data = data.stream().filter(s -> s.getSaleDate().getYear() == y)
                        .collect(java.util.stream.Collectors.toList());

                effectiveStart = LocalDateTime.of(y, 1, 1, 0, 0);
                LocalDateTime yearEnd = LocalDateTime.of(y, 12, 31, 23, 59, 59);
                if (yearEnd.isAfter(LocalDateTime.now())) {
                    effectiveEnd = LocalDateTime.now();
                } else {
                    effectiveEnd = yearEnd;
                }
            }
        }

        // Formatter for grouping
        java.time.format.DateTimeFormatter formatter;
        if (groupHourly) {
            formatter = java.time.format.DateTimeFormatter.ofPattern("HH:00");
        } else if (groupMonthly) {
            formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
        } else {
            formatter = java.time.format.DateTimeFormatter.ofPattern("MMM-d", java.util.Locale.ENGLISH);
        }

        // 1. Revenue & Profit Trend (Line Chart)
        revenueLineChart.getData().clear();
        XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
        revSeries.setName(b.getString("reports.chart.revenue"));
        XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
        profitSeries.setName(b.getString("reports.chart.profit"));

        // Use a LinkedHashMap to preserve order of sales list (chronological)
        java.util.Map<String, Double> orderPreservingRev = new java.util.LinkedHashMap<>();
        java.util.Map<String, Double> orderPreservingProfit = new java.util.LinkedHashMap<>();

        // Pre-populate expected time intervals to ensure chronological order and
        // complete axes
        boolean isAllTimeNoYear = (filterIndex == 4 && (analysisYearComboBox.getValue() == null
                || analysisYearComboBox.getValue().equals(b.getString("reports.filter.allTime"))));

        if (!isAllTimeNoYear) {
            if (groupHourly) {
                int loopEnd = effectiveEnd.toLocalDate().isEqual(effectiveStart.toLocalDate()) ? effectiveEnd.getHour()
                        : 23;
                if (!effectiveEnd.toLocalDate().isEqual(LocalDateTime.now().toLocalDate()) && filterIndex == 5) {
                    loopEnd = 23;
                } else if (filterIndex == 0) { // Today
                    loopEnd = effectiveEnd.getHour();
                }
                for (int i = 0; i <= loopEnd; i++) {
                    String key = String.format("%02d:00", i);
                    orderPreservingRev.put(key, 0.0);
                    orderPreservingProfit.put(key, 0.0);
                }
            } else if (groupMonthly) {
                LocalDateTime current = effectiveStart.withDayOfMonth(1);
                while (!current.isAfter(effectiveEnd) || (current.getYear() == effectiveEnd.getYear()
                        && current.getMonthValue() == effectiveEnd.getMonthValue())) {
                    String key = current.format(formatter);
                    orderPreservingRev.put(key, 0.0);
                    orderPreservingProfit.put(key, 0.0);
                    if (current.getYear() == effectiveEnd.getYear()
                            && current.getMonthValue() == effectiveEnd.getMonthValue())
                        break;
                    current = current.plusMonths(1);
                }
            } else { // Daily
                LocalDateTime current = effectiveStart.toLocalDate().atStartOfDay();
                while (!current.isAfter(effectiveEnd)) {
                    String key = current.format(formatter);
                    orderPreservingRev.put(key, 0.0);
                    orderPreservingProfit.put(key, 0.0);
                    if (current.toLocalDate().isEqual(effectiveEnd.toLocalDate()))
                        break;
                    current = current.plusDays(1);
                }
            }
        } else {
            // For general 'All time', gather sorting keys from data naturally, but sort
            java.util.Map<String, Double> treeRev = new java.util.TreeMap<>();
            java.util.Map<String, Double> treeProfit = new java.util.TreeMap<>();
            for (Sale s : data) {
                String key = s.getSaleDate().format(formatter);
                treeRev.put(key, 0.0);
                treeProfit.put(key, 0.0);
            }
            orderPreservingRev.putAll(treeRev);
            orderPreservingProfit.putAll(treeProfit);
        }

        // Apply actual data overrides
        for (Sale s : data) {
            String key = s.getSaleDate().format(formatter);
            orderPreservingRev.put(key, orderPreservingRev.getOrDefault(key, 0.0) + s.getTotalAmount());
            orderPreservingProfit.put(key, orderPreservingProfit.getOrDefault(key, 0.0) + s.getTotalProfit());
        }

        for (String key : orderPreservingRev.keySet()) {
            revSeries.getData().add(new XYChart.Data<>(key, orderPreservingRev.get(key)));
            profitSeries.getData().add(new XYChart.Data<>(key, orderPreservingProfit.get(key)));
        }
        revenueLineChart.getData().add(revSeries);
        revenueLineChart.getData().add(profitSeries);

        // Dynamically adjust chart width based on the number of data points to enable
        // horizontal scrolling
        double requiredWidth = Math.max(800.0, orderPreservingRev.size() * 60.0);
        revenueLineChart.setMinWidth(requiredWidth);
        profitStackedBarChart.setMinWidth(requiredWidth);

        String mmk = b.getString("common.mmk");
        for (XYChart.Data<String, Number> dataNode : revSeries.getData()) {
            installTooltip(dataNode, "%s\nRevenue: %,.2f " + mmk);
        }
        for (XYChart.Data<String, Number> dataNode : profitSeries.getData()) {
            installTooltip(dataNode, "%s\nProfit: %,.2f " + mmk);
        }

        // 2. Sales by Category (Bar Chart)
        categoryBarChart.getData().clear();
        XYChart.Series<String, Number> catSeries = new XYChart.Series<>();
        catSeries.setName(b.getString("reports.chart.revenue"));

        java.util.Map<String, Double> revByCat = new java.util.TreeMap<>();

        try (com.pos.system.dao.SaleDAO saleDAO = new com.pos.system.dao.SaleDAO()) {
            for (Sale s : data) {
                List<com.pos.system.models.SaleItem> items = saleDAO.getItemsBySaleId(s.getId());
                for (com.pos.system.models.SaleItem item : items) {
                    String cat = item.getCategoryName();
                    if (cat == null)
                        cat = "Uncategorized";
                    revByCat.put(cat, revByCat.getOrDefault(cat, 0.0) + (item.getQuantity() * item.getPriceAtSale()));
                }
            }
        } catch (Exception ignored) {
        }

        for (java.util.Map.Entry<String, Double> entry : revByCat.entrySet()) {
            catSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        categoryBarChart.getData().add(catSeries);

        for (XYChart.Data<String, Number> dataNode : catSeries.getData()) {
            installTooltip(dataNode, "%s\nSales: %,.2f " + mmk);
        }

        // 3. Payment Methods (Pie Chart)
        paymentPieChart.getData().clear();
        for (Sale s : data) {
            String pMethods = s.getPaymentMethods();
            if (pMethods == null || pMethods.isEmpty())
                continue;

            String[] methods = pMethods.split(",");
            for (String methodStr : methods) {
                // p.getPaymentMethod() + " (" + String.format("%,.0f", p.getAmount()) + ")"
                int startParen = methodStr.lastIndexOf('(');
                if (startParen != -1) {
                    String method = methodStr.substring(0, startParen).trim();
                    try {
                        String amtStr = methodStr.substring(startParen + 1, methodStr.length() - 1)
                                .replaceAll("[,\\s]", "");
                        double amt = Double.parseDouble(amtStr);
                        // accumulate
                        PieChart.Data existingData = null;
                        for (PieChart.Data dp : paymentPieChart.getData()) {
                            if (dp.getName().equals(method)) {
                                existingData = dp;
                                break;
                            }
                        }
                        if (existingData != null) {
                            existingData.setPieValue(existingData.getPieValue() + amt);
                        } else {
                            paymentPieChart.getData().add(new PieChart.Data(method, amt));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (PieChart.Data d : paymentPieChart.getData()) {
            installTooltip(d, "%s\nAmount: %,.2f " + mmk);
        }

        // 4. Profit Structure (Stacked Bar)
        profitStackedBarChart.getData().clear();
        XYChart.Series<String, Number> costSeries = new XYChart.Series<>();
        costSeries.setName(b.getString("report.excel.pl.cogs"));
        XYChart.Series<String, Number> netProfitSeries = new XYChart.Series<>();
        netProfitSeries.setName(b.getString("reports.chart.profit"));

        for (String key : orderPreservingRev.keySet()) {
            double r = orderPreservingRev.get(key);
            double p = orderPreservingProfit.get(key);
            costSeries.getData().add(new XYChart.Data<>(key, r - p));
            netProfitSeries.getData().add(new XYChart.Data<>(key, p));
        }
        profitStackedBarChart.getData().add(costSeries);
        profitStackedBarChart.getData().add(netProfitSeries);

        for (XYChart.Data<String, Number> dataNode : costSeries.getData()) {
            installTooltip(dataNode, "%s\nCOGS: %,.2f " + mmk);
        }
        for (XYChart.Data<String, Number> dataNode : netProfitSeries.getData()) {
            installTooltip(dataNode, "%s\nNet Profit: %,.2f " + mmk);
        }

        // Auto-scroll the charts to the end (newest data)
        javafx.application.Platform.runLater(() -> {
            if (revenueScrollPane != null) {
                revenueScrollPane.layout();
                revenueScrollPane.setHvalue(1.0);
            }
            if (profitScrollPane != null) {
                profitScrollPane.layout();
                profitScrollPane.setHvalue(1.0);
            }
        });
    }

    private void installTooltip(XYChart.Data<String, Number> dataNode, String formatStr) {
        String tooltipText = String.format(formatStr, dataNode.getXValue(), dataNode.getYValue().doubleValue());
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");

        // Add hover effects
        javafx.scene.Node node = dataNode.getNode();
        if (node != null) {
            Tooltip.install(node, tooltip);
            addHoverAnimation(node);
        } else {
            dataNode.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, tooltip);
                    addHoverAnimation(newNode);
                }
            });
        }
    }

    private void installTooltip(PieChart.Data dataNode, String formatStr) {
        String tooltipText = String.format(formatStr, dataNode.getName(), dataNode.getPieValue());
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");

        javafx.scene.Node node = dataNode.getNode();
        if (node != null) {
            Tooltip.install(node, tooltip);
            addHoverAnimation(node);
        } else {
            dataNode.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, tooltip);
                    addHoverAnimation(newNode);
                }
            });
        }
    }

    private void addHoverAnimation(javafx.scene.Node node) {
        // Transition for scaling
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(150), node);
        scaleIn.setToX(1.15);
        scaleIn.setToY(1.15);

        javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(150), node);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        node.setOnMouseEntered(e -> {
            node.setCursor(javafx.scene.Cursor.HAND);
            scaleIn.playFromStart();
            node.setOpacity(0.8);
        });

        node.setOnMouseExited(e -> {
            node.setCursor(javafx.scene.Cursor.DEFAULT);
            scaleOut.playFromStart();
            node.setOpacity(1.0);
        });
    }
}
