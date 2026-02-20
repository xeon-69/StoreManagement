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
    private TableView<Sale> recentSalesTable;
    @FXML
    private TableColumn<Sale, Integer> idCol;
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
            loadStats();
            loadRecentSales();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStats() {
        try {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime startOfTime = LocalDateTime.of(2000, 1, 1, 0, 0);

            double todaySales = saleDAO.getTotalSalesSince(startOfDay);
            double todayProfit = saleDAO.getTotalProfitSince(startOfDay);
            double totalSales = saleDAO.getTotalSalesSince(startOfTime);

            todaySalesLabel.setText(String.format("%.2f MMK", todaySales));
            todayProfitLabel.setText(String.format("%.2f MMK", todayProfit));
            totalSalesLabel.setText(String.format("%.2f MMK", totalSales));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRecentSales() {
        try {
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
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

            ObservableList<Sale> sales = FXCollections.observableArrayList(saleDAO.getRecentSales(50));
            recentSalesTable.setItems(sales);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
