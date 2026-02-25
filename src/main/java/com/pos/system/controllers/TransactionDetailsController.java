package com.pos.system.controllers;

import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.SalePayment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class TransactionDetailsController {

    @FXML
    private Label saleIdLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label taxLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label userLabel;

    @FXML
    private TableView<SaleItem> itemsTable;
    @FXML
    private TableColumn<SaleItem, String> itemCol;
    @FXML
    private TableColumn<SaleItem, Integer> qtyCol;
    @FXML
    private TableColumn<SaleItem, String> priceCol;
    @FXML
    private TableColumn<SaleItem, String> itemTotalCol;

    @FXML
    private TableView<SalePayment> paymentsTable;
    @FXML
    private TableColumn<SalePayment, String> methodCol;
    @FXML
    private TableColumn<SalePayment, String> amountCol;

    private Sale sale;

    @FXML
    public void initialize() {
        itemCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%,.2f", cellData.getValue().getPriceAtSale())));
        itemTotalCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%,.2f", cellData.getValue().getTotal())));

        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        amountCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%,.2f", cellData.getValue().getAmount())));
    }

    public void setSale(Sale sale) {
        this.sale = sale;
        populateData();
    }

    private void populateData() {
        if (sale == null)
            return;

        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        saleIdLabel.setText(String.format(b.getString("details.saleId"), sale.getId()));
        dateLabel.setText(String.format(b.getString("details.date"), sale.getSaleDate().toString().replace("T", " ")));
        String mmk = b.getString("common.mmk");
        subtotalLabel.setText(String.format("%,.2f %s", sale.getSubtotal(), mmk));
        taxLabel.setText(String.format("%,.2f %s", sale.getTaxAmount(), mmk));
        discountLabel.setText(String.format("%,.2f %s", sale.getDiscountAmount(), mmk));
        totalLabel.setText(String.format("%,.2f %s", sale.getTotalAmount(), mmk));
        userLabel.setText(String.format(com.pos.system.App.getBundle().getString("details.userId"), sale.getUserId()));

        try (SaleDAO saleDAO = new SaleDAO(); SalePaymentDAO paymentDAO = new SalePaymentDAO()) {
            List<SaleItem> items = saleDAO.getItemsBySaleId(sale.getId());
            itemsTable.setItems(FXCollections.observableArrayList(items));

            List<SalePayment> payments = paymentDAO.findBySaleId(sale.getId());
            paymentsTable.setItems(FXCollections.observableArrayList(payments));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void close() {
        ((Stage) saleIdLabel.getScene().getWindow()).close();
    }
}
