package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.ShiftDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.models.Shift;
import com.pos.system.models.User;
import com.pos.system.utils.SessionManager;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class ShiftControllerTest {

    private ShiftDAO mockShiftDAO;
    private SaleDAO mockSaleDAO;

    private ShiftController controller;
    private Stage mainStage;
    private User testUser;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");
        mainStage = stage;
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Mock DAOs
        mockShiftDAO = mock(ShiftDAO.class);
        when(mockShiftDAO.create(any(Shift.class))).thenAnswer(invocation -> {
            Shift s = invocation.getArgument(0);
            s.setId(1);
            return s;
        });
        doNothing().when(mockShiftDAO).update(any(Shift.class));

        mockSaleDAO = mock(SaleDAO.class);
        when(mockSaleDAO.getTotalSalesBetween(any(), any())).thenReturn(50.0);

        // Set up real SessionManager
        testUser = new User(1, "testuser", "password", "ADMIN");
        // We can't elegantly bypass getInstance(), but we can login
        // SessionManager login doesn't check DB, it's just state usually. Actually,
        // wait.
        // SessionManager.getInstance().login calls UserDAO? No, SessionManager usually
        // just takes the User object.
        // Wait, SessionManager.getInstance().setCurrentUser() requires reflection or we
        // check if it has a setter.
        // The real SessionManager has `login(User user)` or `setCurrentUser()`?
        // Let's assume it has setCurrentUser or we can just reflection inject, or use
        // what we know.
        // Let's use reflection to inject current user if there's no setter.
        try {
            java.lang.reflect.Field userField = SessionManager.class.getDeclaredField("currentUser");
            userField.setAccessible(true);
            userField.set(SessionManager.getInstance(), testUser);

            java.lang.reflect.Field shiftField = SessionManager.class.getDeclaredField("currentShift");
            shiftField.setAccessible(true);
            shiftField.set(SessionManager.getInstance(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        // No construction mocks to close
    }

    private void loadFXML() throws Exception {
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/shift_management.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();
        controller.setShiftDAO(mockShiftDAO);
        controller.setSaleDAO(mockSaleDAO);

        org.testfx.util.WaitForAsyncUtils.asyncFx(() -> {
            mainStage.setScene(new Scene(root, 600, 600));
            mainStage.show();
        }).get();
    }

    @Test
    public void testOpenShift(FxRobot robot) throws Exception {
        try {
            java.lang.reflect.Field shiftField = SessionManager.class.getDeclaredField("currentShift");
            shiftField.setAccessible(true);
            shiftField.set(SessionManager.getInstance(), null);
        } catch (Exception e) {
        }

        loadFXML();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#openingCashField").write("100.0");

        try {
            robot.clickOn("#openShiftBtn");
            WaitForAsyncUtils.waitForFxEvents();
        } catch (Exception e) {
            // Expected App.setRoot exception
        }

        // Verify shift was created
        verify(mockShiftDAO, times(1)).create(any(Shift.class));
    }

    @Test
    public void testCloseShift(FxRobot robot) throws Exception {
        Shift activeShift = new Shift(1, 1, LocalDateTime.now().minusHours(8), null, 100.0, null, null, "OPEN");
        try {
            java.lang.reflect.Field shiftField = SessionManager.class.getDeclaredField("currentShift");
            shiftField.setAccessible(true);
            shiftField.set(SessionManager.getInstance(), activeShift);
        } catch (Exception e) {
        }

        loadFXML();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#actualCashField").write("150.0");

        try {
            // App.setRoot will fail in test env
            robot.clickOn("#closeShiftBtn");
            WaitForAsyncUtils.waitForFxEvents();
        } catch (Exception e) {
            // Expected App.setRoot exception
        }

        verify(mockShiftDAO, times(1)).update(any(Shift.class));
        verify(mockSaleDAO, times(1)).getTotalSalesBetween(any(), any()); // Sales should be checked for expected cash
    }
}
