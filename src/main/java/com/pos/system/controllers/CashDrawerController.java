package com.pos.system.controllers;

import com.pos.system.dao.CashDrawerTransactionDAO;
import com.pos.system.models.CashDrawerTransaction;
import com.pos.system.services.PrinterService;
import com.pos.system.utils.NotificationUtils;
import com.pos.system.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class CashDrawerController {

    @FXML
    private TableView<CashDrawerTransaction> drawerTable;
    @FXML
    private TableColumn<CashDrawerTransaction, Integer> idCol;
    @FXML
    private TableColumn<CashDrawerTransaction, String> dateCol;
    @FXML
    private TableColumn<CashDrawerTransaction, Integer> shiftCol;
    @FXML
    private TableColumn<CashDrawerTransaction, Integer> userCol;
    @FXML
    private TableColumn<CashDrawerTransaction, String> typeCol;
    @FXML
    private TableColumn<CashDrawerTransaction, String> amountCol;
    @FXML
    private TableColumn<CashDrawerTransaction, String> descCol;

    private final ObservableList<CashDrawerTransaction> trxList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        dateCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getTransactionDate() != null
                        ? cellData.getValue().getTransactionDate().toString().replace("T", " ")
                        : ""));

        shiftCol.setCellValueFactory(new PropertyValueFactory<>("shiftId"));
        userCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("transactionType"));

        amountCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%,.2f", cellData.getValue().getAmount())));

        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        drawerTable.setItems(trxList);
        loadTransactions();
    }

    private void loadTransactions() {
        trxList.clear();
        try (CashDrawerTransactionDAO dao = new CashDrawerTransactionDAO()) {
            com.pos.system.models.Shift currentShift = SessionManager.getInstance().getCurrentShift();
            if (currentShift != null) {
                trxList.addAll(dao.findByShiftId(currentShift.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenDrawer() {
        PrinterService printerService = new PrinterService();
        printerService.openCashDrawer();
        NotificationUtils.showSuccess("Cash Drawer", "Cash drawer command sent.");
    }
}
