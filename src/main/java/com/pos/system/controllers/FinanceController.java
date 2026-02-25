package com.pos.system.controllers;

import com.pos.system.dao.ExpenseDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.models.Expense;
import com.pos.system.utils.NotificationUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

public class FinanceController {

    // Summary labels
    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalExpensesLabel;
    @FXML
    private Label netProfitLabel;

    // Date filter controls
    @FXML
    private ComboBox<String> dateRangeComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    // Add expense form
    @FXML
    private TextField categoryField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField descField;

    // Expense table
    @FXML
    private TableView<Expense> expenseTable;
    @FXML
    private TableColumn<Expense, Integer> idCol;
    @FXML
    private TableColumn<Expense, LocalDateTime> dateCol;
    @FXML
    private TableColumn<Expense, String> categoryCol;
    @FXML
    private TableColumn<Expense, String> descCol;
    @FXML
    private TableColumn<Expense, Double> amountCol;
    @FXML
    private TableColumn<Expense, Void> actionCol;

    @FXML
    public void initialize() {
        try {
            // Setup date filter ComboBox
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
                    handleFilter();
                } else if (isCustom && startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                    handleFilter();
                }
            });

            // Date picker constraints — no future dates
            final LocalDate today = LocalDate.now();

            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate maxDate = endDatePicker.getValue() != null
                            ? (endDatePicker.getValue().isBefore(today) ? endDatePicker.getValue() : today)
                            : today;
                    if (date.isAfter(maxDate)) {
                        setDisable(true);
                        setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            });

            endDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate minDate = startDatePicker.getValue();
                    if (date.isAfter(today) || (minDate != null && date.isBefore(minDate))) {
                        setDisable(true);
                        setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            });

            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && endDatePicker.getValue() != null)
                    handleFilter();
            });

            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && startDatePicker.getValue() != null)
                    handleFilter();
            });

            // Setup table columns
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
            descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
            amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

            // Format amount column
            amountCol.setCellFactory(col -> new TableCell<Expense, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%,.2f", item));
                }
            });

            // Format date column
            dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            dateCol.setCellFactory(column -> new TableCell<Expense, LocalDateTime>() {
                private final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                        .ofPattern("dd-MM-yyyy HH:mm:ss");

                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.format(formatter));
                }
            });

            // Action column with Delete button
            setupActionColumn();

            // Default to "All Time"
            dateRangeComboBox.getSelectionModel().select(3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<Expense, Void>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleDeleteExpense(expense);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(deleteBtn);
                    pane.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });
    }

    private void handleDeleteExpense(Expense expense) {
        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(b.getString("finance.delete.title"));
        confirm.setHeaderText(String.format(b.getString("finance.delete.header"), expense.getCategory()));
        confirm.setContentText(String.format(b.getString("finance.delete.content"), expense.getAmount())
                + "\n" + b.getString("dialog.confirm"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (ExpenseDAO dao = createExpenseDAO()) {
                dao.deleteExpense(expense.getId());
                handleFilter(); // Refresh
                NotificationUtils.showSuccess(b.getString("finance.delete.success"),
                        b.getString("finance.delete.successMsg"));
            } catch (SQLException e) {
                e.printStackTrace();
                NotificationUtils.showError(b.getString("dialog.error"), b.getString("finance.delete.errorMsg"));
            }
        }
    }

    protected ExpenseDAO createExpenseDAO() throws SQLException {
        return new ExpenseDAO();
    }

    /**
     * Synchronous filter — queries income (sales) and expenses for the selected
     * date range.
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
                LocalDate td = now.toLocalDate();
                start = td.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                end = td.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59);
                break;
            case 2: // This Month
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            case 3: // All Time
                start = LocalDateTime.of(2000, 1, 1, 0, 0);
                end = now;
                break;
            case 4: // Custom
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null)
                    return;
                start = startDatePicker.getValue().atStartOfDay();
                end = endDatePicker.getValue().atTime(23, 59, 59);
                break;
            default:
                return;
        }

        try (SaleDAO saleDAO = new SaleDAO();
                ExpenseDAO expenseDAO = createExpenseDAO()) {

            double totalIncome = saleDAO.getTotalSalesBetween(start, end);
            double totalExpenses = expenseDAO.getTotalExpensesBetween(start, end);
            double netProfit = totalIncome - totalExpenses;

            List<Expense> expenses = expenseDAO.getExpensesBetween(start, end);

            // Update UI
            String mmk = com.pos.system.App.getBundle().getString("common.mmk");
            totalIncomeLabel.setText(String.format("%,.2f %s", totalIncome, mmk));
            totalExpensesLabel.setText(String.format("%,.2f %s", totalExpenses, mmk));
            netProfitLabel.setText(String.format("%,.2f %s", netProfit, mmk));

            // Color-code net profit
            if (netProfit >= 0) {
                netProfitLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 22px; -fx-font-weight: bold;");
            } else {
                netProfitLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 22px; -fx-font-weight: bold;");
            }

            expenseTable.setItems(FXCollections.observableArrayList(expenses));

        } catch (Exception e) {
            e.printStackTrace();
            NotificationUtils.showError(com.pos.system.App.getBundle().getString("dialog.error"),
                    com.pos.system.App.getBundle().getString("finance.load.error"));
        }
    }

    @FXML
    private void handleAddExpense() {
        try {
            String category = categoryField.getText();
            double amount = Double.parseDouble(amountField.getText());
            String desc = descField.getText();

            if (category.isEmpty())
                return;

            try (ExpenseDAO expenseDAO = createExpenseDAO()) {
                Expense expense = new Expense(0, category, amount, desc, LocalDateTime.now());
                expenseDAO.addExpense(expense);
            }

            categoryField.clear();
            amountField.clear();
            descField.clear();

            handleFilter(); // Refresh with current filter

        } catch (NumberFormatException e) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showWarning(b.getString("finance.input.error"),
                    b.getString("finance.input.invalidAmount"));
        } catch (SQLException e) {
            e.printStackTrace();
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("finance.dbError"), b.getString("finance.add.error"));
        }
    }
}
