package com.pos.system.controllers;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.models.Category;
import com.pos.system.services.CategoryService;
import com.pos.system.utils.NotificationUtils;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.SQLException;

public class CategoryController {

    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> idCol;
    @FXML
    private TableColumn<Category, String> nameCol;
    @FXML
    private TableColumn<Category, String> descCol;
    @FXML
    private TableColumn<Category, Void> actionCol;
    @FXML
    private TextField searchField;

    private Category selectedCategory;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        setupActionColumn();

        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCategory = newVal;
        });

        loadCategories();

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> filterCategories(newValue));
        }
    }

    private void loadCategories() {
        try {
            CategoryService service = new CategoryService();
            categoryTable.setItems(FXCollections.observableArrayList(service.getAllCategories()));
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationUtils.showWarning("Error", "Failed to load categories.");
        }
    }

    private void filterCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            loadCategories();
            return;
        }

        String lowerCaseKeyword = keyword.toLowerCase();
        try {
            CategoryService service = new CategoryService();
            FilteredList<Category> filteredList = new FilteredList<>(
                    FXCollections.observableArrayList(service.getAllCategories()), category -> {
                        if (category.getName().toLowerCase().contains(lowerCaseKeyword))
                            return true;
                        if (category.getDescription() != null
                                && category.getDescription().toLowerCase().contains(lowerCaseKeyword))
                            return true;
                        return false;
                    });
            categoryTable.setItems(filteredList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<Category, Void>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                FontIcon editIcon = new FontIcon("fas-edit");
                editIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                editBtn.setGraphic(editIcon);
                editBtn.getStyleClass().add("btn-primary");
                editBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    openCategoryForm(category);
                });

                FontIcon deleteIcon = new FontIcon("fas-trash-alt");
                deleteIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });

                pane.setStyle("-fx-alignment: CENTER;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML
    private void handleAddCategory() {
        openCategoryForm(null);
    }

    private void openCategoryForm(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_category.fxml"));
            loader.setResources(com.pos.system.App.getBundle());

            Parent root = loader.load();

            AddCategoryController controller = loader.getController();
            controller.setCategoryToEdit(category);
            controller.setOnSaveCallback(this::loadCategories);

            Stage stage = new Stage();
            stage.setTitle(category == null ? "Add Category" : "Edit Category");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            NotificationUtils.showWarning("Error", "Could not load category form.");
        }
    }

    private void handleDeleteCategory(Category category) {
        boolean confirm = NotificationUtils.showConfirmation(
                com.pos.system.App.getBundle().getString("dialog.confirm"),
                "Delete category: " + category.getName() + "? This action cannot be undone.");

        if (confirm) {
            try {
                CategoryService service = new CategoryService();
                service.deleteCategory(category.getId());
                NotificationUtils.showInfo("Success", "Category deleted.");
                loadCategories();
            } catch (SQLException e) {
                e.printStackTrace();
                if (e.getMessage().contains("SQLITE_CONSTRAINT_FOREIGNKEY")) {
                    NotificationUtils.showWarning("Error", "Cannot delete category used by products.");
                } else {
                    NotificationUtils.showWarning("Error", "Failed to delete category.");
                }
            }
        }
    }
}
