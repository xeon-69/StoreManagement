package com.pos.system.controllers;

import com.pos.system.dao.UserDAO;
import com.pos.system.models.User;
import com.pos.system.services.SecurityService;
import com.pos.system.utils.NotificationUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import javafx.concurrent.Task;
import javafx.application.Platform;

public class UsersController {

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, Integer> idCol;
    @FXML
    private TableColumn<User, String> usernameCol;
    @FXML
    private TableColumn<User, String> roleCol;
    @FXML
    private TableColumn<User, String> forceChangeCol;
    @FXML
    private TableColumn<User, Void> actionCol;

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private VBox passwordContainer;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private CheckBox forceChangeCheckBox;
    @FXML
    private Button saveBtn;

    // For Dependency Injection in Tests only
    private UserDAO injectedUserDAO;
    private SecurityService securityService;
    private User selectedUserForEdit = null;

    public void setUserDAO(UserDAO userDAO) {
        this.injectedUserDAO = userDAO;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    @FXML
    public void initialize() {
        if (securityService == null) {
            try {
                securityService = new SecurityService();
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }

        roleComboBox.setItems(FXCollections.observableArrayList("CASHIER", "MANAGER", "ADMIN"));
        roleComboBox.getSelectionModel().selectFirst();

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        forceChangeCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().isForcePasswordChange() ? "Yes" : "No"));

        setupActionColumn();

        loadUsers();
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<User, Void>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final Button changePasswordBtn = new Button();
            private final HBox pane = new HBox(8, editBtn, changePasswordBtn, deleteBtn);

            {
                org.kordamp.ikonli.javafx.FontIcon editIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-edit");
                editIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                editBtn.setGraphic(editIcon);
                editBtn.getStyleClass().add("btn-primary");
                editBtn.setOnAction(event -> handleEditRequest(getTableView().getItems().get(getIndex())));

                org.kordamp.ikonli.javafx.FontIcon keyIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-key");
                keyIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                changePasswordBtn.setGraphic(keyIcon);
                changePasswordBtn.getStyleClass().add("btn-secondary");
                changePasswordBtn
                        .setOnAction(event -> handlePasswordChangeRequest(getTableView().getItems().get(getIndex())));

                org.kordamp.ikonli.javafx.FontIcon delIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-trash");
                delIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                deleteBtn.setGraphic(delIcon);
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setOnAction(event -> handleDeleteRequest(getTableView().getItems().get(getIndex())));

                pane.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadUsers() {
        Task<List<User>> loadTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                if (injectedUserDAO != null) {
                    return injectedUserDAO.getAllUsers();
                }
                try (UserDAO dao = new UserDAO()) {
                    return dao.getAllUsers();
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            ObservableList<User> observableUsers = FXCollections.observableArrayList(loadTask.getValue());
            usersTable.setItems(observableUsers);
        });

        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            NotificationUtils.showError("Database Error", "Failed to load users.");
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String role = roleComboBox.getValue();
        boolean forceChange = forceChangeCheckBox.isSelected();

        if (username.isEmpty()) {
            NotificationUtils.showError("Validation Error", "Username cannot be empty.");
            return;
        }

        if (selectedUserForEdit == null) {
            // Add New User
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (password.isEmpty() || !password.equals(confirmPassword)) {
                NotificationUtils.showError("Validation Error", "Passwords do not match or are empty.");
                return;
            }

            String hashedPw = securityService.hashPassword(password);
            User newUser = new User(0, username, hashedPw, role, forceChange);

            Task<Boolean> saveTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    if (injectedUserDAO != null) {
                        return injectedUserDAO.createUser(newUser);
                    }
                    try (UserDAO dao = new UserDAO()) {
                        return dao.createUser(newUser);
                    }
                }
            };

            saveTask.setOnSucceeded(e -> {
                if (saveTask.getValue()) {
                    NotificationUtils.showInfo("Success", "User added successfully.");
                    handleClearForm();
                    loadUsers();
                } else {
                    NotificationUtils.showError("Save Failed", "Could not insert user, username might be taken.");
                }
            });

            new Thread(saveTask).start();

        } else {
            // Edit Existing User Role/ForceChange Flags
            Task<Boolean> updateTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    if (injectedUserDAO != null) {
                        return injectedUserDAO.updateUserRole(selectedUserForEdit.getId(), role);
                    }
                    try (UserDAO dao = new UserDAO()) {
                        return dao.updateUserRole(selectedUserForEdit.getId(), role);
                    }
                }
            };

            updateTask.setOnSucceeded(e -> {
                if (updateTask.getValue()) {
                    NotificationUtils.showInfo("Success", "User role updated successfully.");
                    handleClearForm();
                    loadUsers();
                } else {
                    NotificationUtils.showError("Update Failed", "Could not update user.");
                }
            });

            new Thread(updateTask).start();
        }
    }

    private void handleEditRequest(User user) {
        selectedUserForEdit = user;
        formTitleLabel.setText(com.pos.system.App.getBundle().getString("users.edit"));
        usernameField.setText(user.getUsername());
        usernameField.setDisable(true); // Cannot change username easily
        roleComboBox.getSelectionModel().select(user.getRole());
        forceChangeCheckBox.setSelected(user.isForcePasswordChange());
        passwordContainer.setVisible(false);
        passwordContainer.setManaged(false);
    }

    private void handlePasswordChangeRequest(User user) {
        // Simple prompt or dialog using Dialog<String>
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(com.pos.system.App.getBundle().getString("users.changePassword"));
        dialog.setHeaderText("Set a new password for " + user.getUsername());

        ButtonType saveButtonType = new ButtonType(com.pos.system.App.getBundle().getString("dialog.save"),
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        PasswordField pwd = new PasswordField();
        pwd.setPromptText("New Password");
        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText("Confirm Password");
        CheckBox forceChangeBox = new CheckBox("Force change on next login");
        forceChangeBox.setSelected(true);

        VBox vbox = new VBox(10, pwd, confirmPwd, forceChangeBox);
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);

        javafx.scene.Node saveBtnNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveBtnNode.setDisable(true);

        confirmPwd.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean valid = !pwd.getText().isEmpty() && pwd.getText().equals(newValue);
            saveBtnNode.setDisable(!valid);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return pwd.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPassword -> {
            String hashed = securityService.hashPassword(newPassword);
            Task<Boolean> pTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    if (injectedUserDAO != null) {
                        return injectedUserDAO.updateUserPassword(user.getId(), hashed, forceChangeBox.isSelected());
                    }
                    try (UserDAO dao = new UserDAO()) {
                        return dao.updateUserPassword(user.getId(), hashed, forceChangeBox.isSelected());
                    }
                }
            };
            pTask.setOnSucceeded(e -> {
                if (pTask.getValue()) {
                    NotificationUtils.showInfo("Success", "Password updated for " + user.getUsername());
                    loadUsers();
                } else {
                    NotificationUtils.showError("Error", "Could not update password");
                }
            });
            new Thread(pTask).start();
        });
    }

    private void handleDeleteRequest(User user) {
        if (user.getUsername().equals("admin")) {
            NotificationUtils.showError("Action Denied", "Cannot delete the default admin user.");
            return;
        }

        boolean confirm = NotificationUtils.showConfirmation(
                com.pos.system.App.getBundle().getString("dialog.confirm"),
                "Are you sure you want to delete user " + user.getUsername() + "?");

        if (confirm) {
            Task<Boolean> delTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    if (injectedUserDAO != null) {
                        return injectedUserDAO.deleteUser(user.getId());
                    }
                    try (UserDAO dao = new UserDAO()) {
                        return dao.deleteUser(user.getId());
                    }
                }
            };
            delTask.setOnSucceeded(e -> {
                if (delTask.getValue()) {
                    loadUsers();
                } else {
                    NotificationUtils.showError("Delete Failed", "Could not delete user.");
                }
            });
            new Thread(delTask).start();
        }
    }

    @FXML
    private void handleClearForm() {
        selectedUserForEdit = null;
        formTitleLabel.setText(com.pos.system.App.getBundle().getString("users.add"));
        usernameField.clear();
        usernameField.setDisable(false);
        passwordField.clear();
        confirmPasswordField.clear();
        passwordContainer.setVisible(true);
        passwordContainer.setManaged(true);
        forceChangeCheckBox.setSelected(false);
        roleComboBox.getSelectionModel().selectFirst();
    }
}
