package com.pos.system.controllers;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.models.Category;
import com.pos.system.utils.NotificationUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class CategoryController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> idCol;
    @FXML
    private TableColumn<Category, String> nameCol;
    @FXML
    private TableColumn<Category, String> descCol;

    private Category selectedCategory;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedCategory = newVal;
                nameField.setText(newVal.getName());
                descriptionArea.setText(newVal.getDescription());
            }
        });

        loadCategories();
    }

    private void loadCategories() {
        try (CategoryDAO dao = new CategoryDAO()) {
            categoryTable.setItems(FXCollections.observableArrayList(dao.getAllCategories()));
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationUtils.showWarning("Error", "Failed to load categories.");
        }
    }

    @FXML
    private void handleAdd() {
        String name = nameField.getText();
        if (name.isEmpty()) {
            NotificationUtils.showWarning("Input Error", "Name is required.");
            return;
        }

        try (CategoryDAO dao = new CategoryDAO()) {
            Category category = new Category(0, name, descriptionArea.getText());
            dao.addCategory(category);
            NotificationUtils.showInfo("Success", "Category added.");
            handleClear();
            loadCategories();
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationUtils.showWarning("Error", "Failed to add category.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedCategory == null) {
            NotificationUtils.showWarning("Selection Error", "No category selected.");
            return;
        }

        try (CategoryDAO dao = new CategoryDAO()) {
            selectedCategory.setName(nameField.getText());
            selectedCategory.setDescription(descriptionArea.getText());
            dao.updateCategory(selectedCategory);
            NotificationUtils.showInfo("Success", "Category updated.");
            handleClear();
            loadCategories();
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationUtils.showWarning("Error", "Failed to update category.");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedCategory == null) {
            NotificationUtils.showWarning("Selection Error", "No category selected.");
            return;
        }

        try (CategoryDAO dao = new CategoryDAO()) {
            dao.deleteCategory(selectedCategory.getId());
            NotificationUtils.showInfo("Success", "Category deleted.");
            handleClear();
            loadCategories();
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationUtils.showWarning("Error", "Failed to delete category.");
        }
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        descriptionArea.clear();
        categoryTable.getSelectionModel().clearSelection();
        selectedCategory = null;
    }
}
