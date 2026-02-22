package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.SalePayment;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class TransactionDetailsControllerTest {

    private MockedConstruction<SaleDAO> mockedSaleDAO;
    private MockedConstruction<SalePaymentDAO> mockedSalePaymentDAO;
    private TransactionDetailsController controller;
    private Sale dummySale;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        dummySale = new Sale(99, 1, 1, 100.0, 10.0, 5.0, 105.0, 25.0, LocalDateTime.now());

        // Mock DAOs
        mockedSaleDAO = mockConstruction(SaleDAO.class, (mock, context) -> {
            SaleItem item1 = new SaleItem(1, 99, 1, "Test Product", 2, 50.0, 25.0);
            when(mock.getItemsBySaleId(99)).thenReturn(Arrays.asList(item1));
        });

        mockedSalePaymentDAO = mockConstruction(SalePaymentDAO.class, (mock, context) -> {
            SalePayment sp = new SalePayment(1, 99, "CASH", 105.0, null);
            when(mock.findBySaleId(99)).thenReturn(Arrays.asList(sp));
        });

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/transaction_details_modal.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        Platform.runLater(() -> controller.setSale(dummySale));

        stage.setScene(new Scene(root, 650, 650));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedSaleDAO != null)
            mockedSaleDAO.close();
        if (mockedSalePaymentDAO != null)
            mockedSalePaymentDAO.close();
    }

    @Test
    public void testTransactionDetailsLoaded(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        FxAssert.verifyThat("#saleIdLabel", LabeledMatchers.hasText("Sale ID: 99"));
        FxAssert.verifyThat("#subtotalLabel", LabeledMatchers.hasText("100.00"));
        FxAssert.verifyThat("#totalLabel", LabeledMatchers.hasText("105.00"));

        TableView<SaleItem> itemsTable = robot.lookup("#itemsTable").queryTableView();
        assertEquals(1, itemsTable.getItems().size());
        assertEquals("Test Product", itemsTable.getItems().get(0).getProductName());

        TableView<SalePayment> paymentsTable = robot.lookup("#paymentsTable").queryTableView();
        assertEquals(1, paymentsTable.getItems().size());
        assertEquals("CASH", paymentsTable.getItems().get(0).getPaymentMethod());

        // Verify mocks
        assertEquals(1, mockedSaleDAO.constructed().size());
        assertEquals(1, mockedSalePaymentDAO.constructed().size());
    }
}
