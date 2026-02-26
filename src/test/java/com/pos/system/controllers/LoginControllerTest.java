package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.models.User;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;
import org.mockito.MockedConstruction;
import com.pos.system.dao.UserDAO;
import com.pos.system.services.SecurityService;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;

@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest {

    private MockedConstruction<UserDAO> mockedUserDAO;
    private MockedConstruction<SecurityService> mockedSecurityService;

    @Start
    private void start(Stage stage) throws Exception {
        mockedUserDAO = mockConstruction(UserDAO.class, (mock, context) -> {
            User admin = new User(1, "admin", "hashed_admin", "ADMIN");
            when(mock.getUserByUsername("admin")).thenReturn(admin);
            when(mock.getUserByUsername(argThat(s -> !"admin".equals(s)))).thenReturn(null);
        });
        mockedSecurityService = mockConstruction(SecurityService.class, (mock, context) -> {
            when(mock.verifyPassword(eq("admin"), eq("hashed_admin"))).thenReturn(true);
            when(mock.verifyPassword(anyString(), argThat(s -> !"hashed_admin".equals(s)))).thenReturn(false);
            when(mock.verifyPassword(argThat(s -> !"admin".equals(s)), anyString())).thenReturn(false);
        });

        App app = new App();
        app.start(stage);
    }

    @AfterEach
    public void tearDown() {
        if (mockedUserDAO != null)
            mockedUserDAO.close();
        if (mockedSecurityService != null)
            mockedSecurityService.close();
    }

    @Test
    void testEmptyFieldsShowError(FxRobot robot) {
        // Assert initial state: empty fields and click login
        robot.clickOn("#usernameField"); // Focus on username
        robot.clickOn("#loginButton");

        // Verify the error label shows empty fields message (using default English
        // bundle)
        FxAssert.verifyThat("#errorLabel", LabeledMatchers.hasText("Please enter both username and password."));
    }

    @Test
    void testInvalidLoginShowsError(FxRobot robot) throws Exception {
        // Act: Enter invalid credentials
        robot.clickOn("#usernameField").doubleClickOn("#usernameField").eraseText(20).write("wronguser");
        robot.clickOn("#passwordField").doubleClickOn("#passwordField").eraseText(20).write("wrongpass");
        robot.clickOn("#loginButton");

        // Wait for UI to update with error message - wait for ANY text first
        WaitForAsyncUtils.waitFor(10, java.util.concurrent.TimeUnit.SECONDS, WaitForAsyncUtils.asyncFx(() -> {
            Label label = robot.lookup("#errorLabel").queryAs(Label.class);
            String text = label.getText();
            return text != null && !text.trim().isEmpty();
        }));

        // Assert: Verify invalid login message
        String errorText = robot.lookup("#errorLabel").queryAs(Label.class).getText();
        System.err.println("DEBUG: Error label text: '" + errorText + "'");
        assertTrue(errorText.contains("Invalid username or password."),
                "Expected invalid login message but got: " + errorText);
    }

    @Test
    void testValidLogin(FxRobot robot) {
        // Act: Enter valid credentials
        robot.clickOn("#usernameField").doubleClickOn("#usernameField").eraseText(20).write("admin");
        robot.clickOn("#passwordField").doubleClickOn("#passwordField").eraseText(20).write("admin");
        robot.clickOn("#loginButton");

        // Assert: We should have navigated away from the login screen.
        // Waiting briefly as the scene change might take a moment
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert: We should have navigated to the dashboard screen
        FxAssert.verifyThat(robot.window(0).getScene().getRoot(), javafx.scene.Node::isVisible);
    }
}
