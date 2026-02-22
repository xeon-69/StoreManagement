package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.models.User;
import com.pos.system.utils.SessionManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class DashboardControllerTest {

    private MockedStatic<SessionManager> mockedSessionManager;
    private SessionManager mockSessionInstance;
    private User mockUser;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        mockUser = new User(1, "testcashier", "pass", "CASHIER", false);

        mockSessionInstance = mock(SessionManager.class);
        when(mockSessionInstance.isLoggedIn()).thenReturn(true);
        when(mockSessionInstance.getCurrentUser()).thenReturn(mockUser);

        // Mock Singleton SessionManager
        mockedSessionManager = mockStatic(SessionManager.class);
        mockedSessionManager.when(SessionManager::getInstance).thenReturn(mockSessionInstance);

        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        fxmlLoader.setResources(App.getBundle());

        BorderPane root = fxmlLoader.load();
        stage.setScene(new Scene(root, 1024, 768));
        stage.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mockedSessionManager != null) {
            mockedSessionManager.close();
        }
    }

    @Test
    public void testCashierUserOptionsHidden(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        FxAssert.verifyThat("#currentUserLabel", LabeledMatchers.hasText("User: testcashier"));

        // Ensure Cashier does NOT see restricted menus
        Button auditBtn = robot.lookup("#navAuditBtn").queryButton();
        Button usersBtn = robot.lookup("#navUsersBtn").queryButton();
        Button settingsBtn = robot.lookup("#navSettingsBtn").queryButton();

        assertFalse(auditBtn.isVisible());
        assertFalse(usersBtn.isVisible());
        assertFalse(settingsBtn.isVisible());
    }
}
