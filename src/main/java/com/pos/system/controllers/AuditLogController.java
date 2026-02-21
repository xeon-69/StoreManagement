package com.pos.system.controllers;

import com.pos.system.dao.AuditLogDAO;
import com.pos.system.models.AuditLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuditLogController {

    @FXML
    private TableView<AuditLog> auditTable;
    @FXML
    private TableColumn<AuditLog, Integer> idCol;
    @FXML
    private TableColumn<AuditLog, String> dateCol;
    @FXML
    private TableColumn<AuditLog, String> userCol;
    @FXML
    private TableColumn<AuditLog, String> actionCol;
    @FXML
    private TableColumn<AuditLog, String> entityCol;
    @FXML
    private TableColumn<AuditLog, String> detailsCol;

    private final ObservableList<AuditLog> logsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt() != null
                ? cellData.getValue().getCreatedAt().toString().replace("T", " ")
                : ""));

        userCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getUserId() != null ? String.valueOf(cellData.getValue().getUserId()) : "SYSTEM"));

        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));

        entityCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getEntityName() + " (" + cellData.getValue().getEntityId() + ")"));

        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));

        auditTable.setItems(logsList);
        loadLogs();
    }

    private void loadLogs() {
        logsList.clear();
        try (AuditLogDAO dao = new AuditLogDAO()) {
            logsList.addAll(dao.getRecentLogs(500)); // Load latest 500 for now
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
