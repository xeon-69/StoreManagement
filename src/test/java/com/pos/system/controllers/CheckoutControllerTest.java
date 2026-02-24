package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.models.SaleItem;
import com.pos.system.models.Sale;
import com.pos.system.models.SalePayment;
import com.pos.system.models.User;
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

    private CheckoutController controller;
    private boolean successCallbackTriggered = false;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Mock Session
        mockSessionInstance = mock(SessionManager.class);
        User mockUser = new User(1, "cashier", "pass", "CASHIER", false);
        when(mockSessionInstance.getCurrentUser()).thenReturn(mockUser);

        mockedSessionManager = mockStatic(SessionManager.class);
        mockedSessionManager.when(SessionManager::getInstance).thenReturn(mockSessionInstance);

        // Mock Services
        mockedCheckoutService = mockConstruction(CheckoutService.class, (mock, context) -> {
            doNothing().when(mock).processCheckoutWithPayments(any(), any(), any());
        });
        mockedPrinterService = mockConstruction(PrinterService.class);

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
        // Fill payment
        robot.clickOn("#paymentAmountField");
        robot.doubleClickOn("#paymentAmountField").eraseText(10).write("15");
        robot.clickOn("#addPaymentButton");

        WaitForAsyncUtils.waitForFxEvents();

        // Check if payment was added
        String changeText = robot.lookup("#changeLabel").queryAs(javafx.scene.control.Label.class).getText();
        String remText = robot.lookup("#remainingLabel").queryAs(javafx.scene.control.Label.class).getText();
        if ("0.00".equals(changeText) && !"0.00".equals(remText)) {
            System.err.println("DEBUG: Payment not registered. Remaining: " + remText + ". Error Label: "
                    + robot.lookup("#errorLabel").queryAs(javafx.scene.control.Label.class).getText());
        }

        // Check Updates
        FxAssert.verifyThat("#remainingLabel", LabeledMatchers.hasText("0.00"));
        FxAssert.verifyThat("#changeLabel", LabeledMatchers.hasText("5.00"));
        assertFalse(confirmBtn.isDisabled());

        // Click Confirm
        robot.clickOn(confirmBtn);

        // The background task needs time
        WaitForAsyncUtils.waitFor(30, java.util.concurrent.TimeUnit.SECONDS, () -> successCallbackTriggered);

        if (!successCallbackTriggered) {
            System.err.println("DEBUG: Checkout callback never triggered. Task status?");
        }
        assertTrue(successCallbackTriggered, "Success callback should be executed");
    }
}
