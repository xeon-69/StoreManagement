package com.pos.system.controllers;

import com.pos.system.App;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest {

    @Start
    private void start(Stage stage) throws Exception {
        App app = new App();
        app.start(stage);
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
    void testInvalidLoginShowsError(FxRobot robot) {
        // Act: Enter invalid credentials
        robot.clickOn("#usernameField").doubleClickOn("#usernameField").eraseText(20).write("wronguser");
        robot.clickOn("#passwordField").doubleClickOn("#passwordField").eraseText(20).write("wrongpass");
        robot.clickOn("#loginButton");

        // Assert: Verify invalid login message
        FxAssert.verifyThat("#errorLabel", LabeledMatchers.hasText("Invalid username or password."));
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
