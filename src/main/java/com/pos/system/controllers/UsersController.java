package com.pos.system.controllers;

import com.pos.system.dao.UserDAO;
import com.pos.system.models.User;
import com.pos.system.services.SecurityService;
import com.pos.system.utils.NotificationUtils;
import com.pos.system.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import javafx.concurrent.Task;

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
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.dbError"), b.getString("users.load.error"));
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String role = roleComboBox.getValue();
        boolean forceChange = forceChangeCheckBox.isSelected();

        if (username.isEmpty()) {
            java.util.ResourceBundle b = com.pos.system.App.getBundle();
            NotificationUtils.showError(b.getString("dialog.validationError"), b.getString("users.username.empty"));
            return;
        }

        if (selectedUserForEdit == null) {
            // Add New User
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (password.isEmpty() || !password.equals(confirmPassword)) {
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                NotificationUtils.showError(b.getString("dialog.validationError"),
                        b.getString("users.password.mismatch"));
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
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                if (saveTask.getValue()) {
                    NotificationUtils.showInfo(b.getString("dialog.successTitle"), b.getString("users.add.success"));
                    handleClearForm();
                    loadUsers();

                    if (securityService != null) {
                        securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                                "CREATE_USER", "User", "N/A", "Username: " + username + ", Role: " + role);
                    }
                } else {
                    NotificationUtils.showError(b.getString("dialog.errorTitle"), b.getString("users.add.fail"));
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
                java.util.ResourceBundle b = com.pos.system.App.getBundle();
                if (updateTask.getValue()) {
                    NotificationUtils.showInfo(b.getString("dialog.successTitle"), b.getString("users.update.success"));
                    handleClearForm();
                    loadUsers();

                    if (securityService != null) {
                        securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                                "UPDATE_USER_ROLE", "User", String.valueOf(selectedUserForEdit.getId()),
                                "Username: " + selectedUserForEdit.getUsername() + ", New Role: " + role);
                    }
                } else {
                    NotificationUtils.showError(b.getString("dialog.errorTitle"), b.getString("users.update.fail"));
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
        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(b.getString("users.changePassword"));
        dialog.setHeaderText(String.format(b.getString("users.changePassword.header"), user.getUsername()));

        ButtonType saveButtonType = new ButtonType(b.getString("dialog.save"),
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        PasswordField pwd = new PasswordField();
        pwd.setPromptText(b.getString("users.password.prompt"));
        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText(b.getString("users.password.confirmPrompt"));
        CheckBox forceChangeBox = new CheckBox(b.getString("users.forceChange.label"));
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
                java.util.ResourceBundle b_inner = com.pos.system.App.getBundle();
                if (pTask.getValue()) {
                    NotificationUtils.showInfo(b_inner.getString("dialog.successTitle"),
                            String.format(b_inner.getString("users.password.updated"), user.getUsername()));
                    loadUsers();

                    if (securityService != null) {
                        securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                                "CHANGE_USER_PASSWORD", "User", String.valueOf(user.getId()),
                                "Username: " + user.getUsername());
                    }
                } else {
                    NotificationUtils.showError(b_inner.getString("dialog.errorTitle"),
                            b_inner.getString("users.password.updateFail"));
                }
            });
            new Thread(pTask).start();
        });
    }

    private void handleDeleteRequest(User user) {
        java.util.ResourceBundle b = com.pos.system.App.getBundle();
        if (user.getUsername().equals("admin")) {
            NotificationUtils.showError(b.getString("dialog.uiError"), b.getString("users.delete.adminDeny"));
            return;
        }

        boolean confirm = NotificationUtils.showConfirmation(
                b.getString("dialog.confirm"),
                String.format(b.getString("users.delete.confirm"), user.getUsername()));

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

                    if (securityService != null) {
                        securityService.logAction(SessionManager.getInstance().getCurrentUser().getId(),
                                "DELETE_USER", "User", String.valueOf(user.getId()), "Username: " + user.getUsername());
                    }
                } else {
                    java.util.ResourceBundle b_inner = com.pos.system.App.getBundle();
                    NotificationUtils.showError(b_inner.getString("dialog.errorTitle"),
                            b_inner.getString("users.delete.fail"));
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
