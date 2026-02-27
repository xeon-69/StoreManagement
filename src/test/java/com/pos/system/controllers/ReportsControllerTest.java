package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.models.Sale;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class ReportsControllerTest {

    @Mock
    private SaleDAO mockSaleDAO;

    @Mock
    private SalePaymentDAO mockPaymentDAO;

    private AutoCloseable mocks;
    private ReportsController controller;

    @Start
    public void start(Stage stage) throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        App.setLocale("en");

        // Mock DAO
        Sale s1 = new Sale(1, 1, 100.0, 10.0, LocalDateTime.now());
        when(mockSaleDAO.getTotalSalesBetween(any(), any())).thenReturn(100.0);
        when(mockSaleDAO.getTotalProfitBetween(any(), any())).thenReturn(20.0);
        when(mockSaleDAO.getSalesBetween(any(), any())).thenReturn(Arrays.asList(s1));
        when(mockPaymentDAO.findBySaleIds(any())).thenReturn(new java.util.ArrayList<>());

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/reports.fxml"));
        fxmlLoader.setResources(App.getBundle());

        // Override DAO creation
        fxmlLoader.setControllerFactory(param -> {
            controller = new ReportsController() {
                @Override
                protected SaleDAO createSaleDAO() {
                    return mockSaleDAO;
                }

                @Override
                protected SalePaymentDAO createPaymentDAO() {
                    return mockPaymentDAO;
                }
            };
            return controller;
        });

        VBox root = fxmlLoader.load();
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testReportsLoaded(FxRobot robot) throws Exception {
        // Wait for background Task to finish (increased timeout)
        WaitForAsyncUtils.waitFor(10, java.util.concurrent.TimeUnit.SECONDS, () -> {
            TableView<Sale> table = robot.lookup("#recentSalesTable").queryTableView();
            return table.getItems().size() == 1;
        });

        WaitForAsyncUtils.waitForFxEvents();

        FxAssert.verifyThat("#todaySalesLabel", LabeledMatchers.hasText("100.00 MMK"));
        FxAssert.verifyThat("#todayProfitLabel", LabeledMatchers.hasText("20.00 MMK"));
        FxAssert.verifyThat("#totalSalesLabel", LabeledMatchers.hasText("100.00 MMK"));

        TableView<Sale> table = robot.lookup("#recentSalesTable").queryTableView();
        assertEquals(1, table.getItems().size());
        assertEquals(100.0, table.getItems().get(0).getTotalAmount());
    }
}
