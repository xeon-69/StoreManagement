package com.pos.system.controllers;

import com.pos.system.dao.AuditLogDAO;
import com.pos.system.models.AuditLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.sql.SQLException;

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
    @FXML
    private TextField searchField;
    @FXML
    private Pagination pagination;

    private static final int ROWS_PER_PAGE = 20;
    private final ObservableList<AuditLog> logsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt() != null
                ? cellData.getValue().getCreatedAt().toString().replace("T", " ")
                : ""));

        userCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getUserId() != null ? String.valueOf(cellData.getValue().getUserId())
                        : com.pos.system.App.getBundle().getString("audit.system")));

        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));

        entityCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getEntityName() + " (" + cellData.getValue().getEntityId() + ")"));

        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));

        auditTable.setItems(logsList);

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    setupPagination();
                }
            });
        }

        setupPagination();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            setupPagination();
            return;
        }

        pagination.setPageCount(1);
        pagination.setPageFactory(pageIndex -> {
            logsList.clear();
            try (AuditLogDAO dao = new AuditLogDAO()) {
                logsList.addAll(dao.searchLogs(query.trim()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new VBox();
        });
    }

    private void setupPagination() {
        try (AuditLogDAO dao = new AuditLogDAO()) {
            int totalLogs = dao.getTotalCount();
            int pageCount = (int) Math.ceil((double) totalLogs / ROWS_PER_PAGE);
            if (pageCount == 0)
                pageCount = 1;

            pagination.setPageCount(pageCount);
            pagination.setPageFactory(pageIndex -> {
                loadLogs(pageIndex);
                return new VBox(); // Pagination requires returning a Node, but we use it as a controller
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadLogs(int pageIndex) {
        logsList.clear();
        int offset = pageIndex * ROWS_PER_PAGE;
        try (AuditLogDAO dao = new AuditLogDAO()) {
            logsList.addAll(dao.getPaginatedLogs(ROWS_PER_PAGE, offset));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
