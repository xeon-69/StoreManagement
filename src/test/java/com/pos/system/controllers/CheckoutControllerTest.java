package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.CashDrawerTransactionDAO;
import com.pos.system.models.SaleItem;
import com.pos.system.models.User;
import com.pos.system.models.Shift;
import com.pos.system.services.CheckoutService;
import com.pos.system.services.PrinterService;
import com.pos.system.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class CheckoutControllerTest {

    private MockedStatic<SessionManager> mockedSessionManager;
    private SessionManager mockSessionInstance;
    private MockedConstruction<CheckoutService> mockedCheckoutService;
    private MockedConstruction<PrinterService> mockedPrinterService;
    private MockedConstruction<CashDrawerTransactionDAO> mockedDrawerDAO;

    private CheckoutController controller;
    private boolean successCallbackTriggered = false;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Mock Session
        mockSessionInstance = mock(SessionManager.class);
        User mockUser = new User(1, "cashier", "pass", "CASHIER", false);
        Shift mockShift = new Shift(1, 1, java.time.LocalDateTime.now(), null, 100.0, null, null, "OPEN");
        when(mockSessionInstance.getCurrentUser()).thenReturn(mockUser);
        when(mockSessionInstance.getCurrentShift()).thenReturn(mockShift);

        mockedSessionManager = mockStatic(SessionManager.class);
        mockedSessionManager.when(SessionManager::getInstance).thenReturn(mockSessionInstance);

        // Mock Services
        mockedCheckoutService = mockConstruction(CheckoutService.class);
        mockedPrinterService = mockConstruction(PrinterService.class);
        mockedDrawerDAO = mockConstruction(CashDrawerTransactionDAO.class);

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/checkout_modal.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        ObservableList<SaleItem> cart = FXCollections.observableArrayList();
        SaleItem item1 = new SaleItem(1, 1, 1, "Apple", 5, 2.0, 1.0); // Total 10.0
        cart.add(item1);

        controller.setCheckoutData(cart, () -> successCallbackTriggered = true);

        stage.setScene(new Scene(root, 600, 800));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedSessionManager != null)
            mockedSessionManager.close();
        if (mockedCheckoutService != null)
            mockedCheckoutService.close();
        if (mockedPrinterService != null)
            mockedPrinterService.close();
        if (mockedDrawerDAO != null)
            mockedDrawerDAO.close();
    }

    @Test
    public void testCheckoutFlow(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();

        // Check Initial Load
        FxAssert.verifyThat("#subtotalLabel", LabeledMatchers.hasText("10.00"));
        FxAssert.verifyThat("#totalLabel", LabeledMatchers.hasText("10.00"));
        FxAssert.verifyThat("#remainingLabel", LabeledMatchers.hasText("10.00"));

        Button confirmBtn = robot.lookup("#confirmButton").queryButton();
        assertTrue(confirmBtn.isDisabled());

        // Add Payment
        TextField amountField = robot.lookup("#paymentAmountField").queryAs(TextField.class);
        robot.doubleClickOn(amountField).write("15.00");
        robot.clickOn(".btn-primary"); // The add payment button

        WaitForAsyncUtils.waitForFxEvents();

        // Check Updates
        FxAssert.verifyThat("#remainingLabel", LabeledMatchers.hasText("0.00"));
        FxAssert.verifyThat("#changeLabel", LabeledMatchers.hasText("5.00"));
        assertFalse(confirmBtn.isDisabled());

        // Click Confirm
        robot.clickOn(confirmBtn);

        // The background task needs time
        WaitForAsyncUtils.waitFor(2000, java.util.concurrent.TimeUnit.MILLISECONDS, () -> successCallbackTriggered);

        assertTrue(successCallbackTriggered, "Success callback should be executed");
    }
}
