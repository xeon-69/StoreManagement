package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.UserDAO;
import com.pos.system.models.User;
import com.pos.system.services.SecurityService;
import com.pos.system.utils.SessionManager;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class UsersControllerTest {

    private UserDAO mockUserDAO;
    private SecurityService mockSecurityService;
    private Stage mainStage;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");
        mainStage = stage;
    }

    @BeforeEach
    public void setUp() throws Exception {
        mockUserDAO = mock(UserDAO.class);
        User u1 = new User(1, "admin", "hash", "ADMIN", false);
        User u2 = new User(2, "cashier1", "hash", "CASHIER", true);
        when(mockUserDAO.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        when(mockUserDAO.createUser(any(User.class))).thenReturn(true);
        when(mockUserDAO.updateUserRole(anyInt(), anyString())).thenReturn(true);

        mockSecurityService = mock(SecurityService.class);
        when(mockSecurityService.hashPassword(anyString())).thenReturn("hashedpwd");

        // Set up session for logging actions
        SessionManager.getInstance().setCurrentUser(u1);
    }

    private void loadFXMLWithFactory() throws Exception {
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/users.fxml"));
        fxmlLoader.setResources(App.getBundle());
        fxmlLoader.setControllerFactory(param -> {
            UsersController c = new UsersController();
            c.setUserDAO(mockUserDAO);
            c.setSecurityService(mockSecurityService);
            return c;
        });

        SplitPane root = fxmlLoader.load();
        fxmlLoader.getController();

        org.testfx.util.WaitForAsyncUtils.asyncFx(() -> {
            mainStage.setScene(new Scene(root, 800, 600));
            mainStage.show();
        }).get();
    }

    @Test
    public void testLoadUsers(FxRobot robot) throws Exception {
        loadFXMLWithFactory();
        WaitForAsyncUtils.waitForFxEvents();
        // Give time for Background Task to finish
        Thread.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        TableView<User> table = robot.lookup("#usersTable").queryTableView();
        assertEquals(2, table.getItems().size());
        assertEquals("admin", table.getItems().get(0).getUsername());
    }

    @Test
    public void testAddUser(FxRobot robot) throws Exception {
        loadFXMLWithFactory();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#usernameField").write("newcashier");
        robot.clickOn("#passwordField").write("pass123");
        robot.clickOn("#confirmPasswordField").write("pass123");
        robot.clickOn("#saveBtn");

        WaitForAsyncUtils.waitForFxEvents();
        Thread.sleep(500); // task to finish
        WaitForAsyncUtils.waitForFxEvents();

        verify(mockSecurityService, atLeastOnce()).hashPassword("pass123");
        verify(mockUserDAO, times(1)).createUser(any(User.class));
    }
}
