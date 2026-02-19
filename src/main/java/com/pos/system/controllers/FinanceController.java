package com.pos.system.controllers;

import com.pos.system.dao.ExpenseDAO;
import com.pos.system.models.Expense;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class FinanceController {

    @FXML
    private TextField categoryField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField descField;

    @FXML
    private TableView<Expense> expenseTable;
    @FXML
    private TableColumn<Expense, LocalDateTime> dateCol;
    @FXML
    private TableColumn<Expense, String> categoryCol;
    @FXML
    private TableColumn<Expense, String> descCol;
    @FXML
    private TableColumn<Expense, Double> amountCol;

    @FXML
    public void initialize() {
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        loadExpenses();
    }

    private void loadExpenses() {
        try (ExpenseDAO expenseDAO = new ExpenseDAO()) {
            expenseTable.setItems(FXCollections.observableArrayList(expenseDAO.getAllExpenses()));
        } catch (SQLException e) {
            e.printStackTrace();
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

            try (ExpenseDAO expenseDAO = new ExpenseDAO()) {
                Expense expense = new Expense(0, category, amount, desc, LocalDateTime.now());
                expenseDAO.addExpense(expense);
            }

            categoryField.clear();
            amountField.clear();
            descField.clear();

            loadExpenses();

        } catch (NumberFormatException e) {
            // Show alert
            System.err.println("Invalid number");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
