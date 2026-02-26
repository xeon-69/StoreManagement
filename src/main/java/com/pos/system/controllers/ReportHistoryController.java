package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.utils.AppDataUtils;
import com.pos.system.utils.NotificationUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReportHistoryController {

    @FXML
    private TableView<File> reportsTable;
    @FXML
    private TableColumn<File, String> nameCol;
    @FXML
    private TableColumn<File, LocalDateTime> dateCol;
    @FXML
    private TableColumn<File, Void> actionCol;
    @FXML
    private Pagination pagination;

    private static final int ROWS_PER_PAGE = 30;
    private List<File> allFiles = new java.util.ArrayList<>();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        dateCol.setCellValueFactory(cellData -> {
            try {
                BasicFileAttributes attrs = Files.readAttributes(cellData.getValue().toPath(),
                        BasicFileAttributes.class);
                return new SimpleObjectProperty<>(
                        LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));
            } catch (IOException e) {
                return new SimpleObjectProperty<>(null);
            }
        });

        dateCol.setCellFactory(column -> new TableCell<File, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DATE_FMT));
                }
            }
        });

        setupActionColumn();
        loadFiles();

        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updatePage(newIndex.intValue());
        });
    }

    private void loadFiles() {
        File reportsDir = AppDataUtils.getReportsDir();
        File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".csv") || name.endsWith(".xlsx"));

        if (files != null) {
            allFiles = Arrays.stream(files)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed())
                    .collect(Collectors.toList());
        } else {
            allFiles = new java.util.ArrayList<>();
        }

        int pageCount = (int) Math.ceil((double) allFiles.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        updatePage(0);
    }

    private void updatePage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allFiles.size());

        if (allFiles.isEmpty()) {
            reportsTable.setItems(FXCollections.observableArrayList());
        } else {
            reportsTable.setItems(FXCollections.observableArrayList(allFiles.subList(fromIndex, toIndex)));
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<File, Void>() {
            private final Button openBtn = new Button();
            private final Button folderBtn = new Button();
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, openBtn, folderBtn);

            {
                ResourceBundle b = App.getBundle();
                openBtn.setText(b.getString("reports.history.openFile"));
                openBtn.getStyleClass().add("btn-primary");
                openBtn.setOnAction(event -> handleOpenFile(getTableView().getItems().get(getIndex())));

                folderBtn.setText(b.getString("reports.history.openFolder"));
                folderBtn.getStyleClass().add("btn-secondary");
                folderBtn.setOnAction(event -> handleOpenFolder(getTableView().getItems().get(getIndex())));

                pane.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void handleOpenFile(File file) {
        if (Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException e) {
                    javafx.application.Platform.runLater(() -> NotificationUtils.showError(
                            App.getBundle().getString("dialog.errorTitle"),
                            String.format(App.getBundle().getString("reports.history.openFile.error"),
                                    e.getMessage())));
                }
            }).start();
        }
    }

    private void handleOpenFolder(File file) {
        if (Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(file.getParentFile().toURI());
                } catch (IOException e) {
                    javafx.application.Platform.runLater(() -> NotificationUtils.showError(
                            App.getBundle().getString("dialog.errorTitle"),
                            String.format(App.getBundle().getString("reports.history.openFolder.error"),
                                    e.getMessage())));
                }
            }).start();
        }
    }

    @FXML
    private void handleRefresh() {
        loadFiles();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) reportsTable.getScene().getWindow();
        stage.close();
    }
}
