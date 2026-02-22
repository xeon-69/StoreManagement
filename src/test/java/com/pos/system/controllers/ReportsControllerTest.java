package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.SaleDAO;
import com.pos.system.models.Sale;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
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
public class ReportsControllerTest {

    private MockedConstruction<SaleDAO> mockedSaleDAO;
    private ReportsController controller;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Mock DAO
        mockedSaleDAO = mockConstruction(SaleDAO.class, (mock, context) -> {
            Sale s1 = new Sale(1, 1, 1, 100.0, 0.0, 0.0, 100.0, 20.0, LocalDateTime.now());
            when(mock.getTotalSalesBetween(any(), any())).thenReturn(100.0);
            when(mock.getTotalProfitBetween(any(), any())).thenReturn(20.0);
            when(mock.getSalesBetween(any(), any())).thenReturn(Arrays.asList(s1));
        });

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/reports.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedSaleDAO != null)
            mockedSaleDAO.close();
    }

    @Test
    public void testReportsLoaded(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents(); // Let the initial handleFilter() run

        FxAssert.verifyThat("#todaySalesLabel", LabeledMatchers.hasText("100.00 MMK"));
        FxAssert.verifyThat("#todayProfitLabel", LabeledMatchers.hasText("20.00 MMK"));
        FxAssert.verifyThat("#totalSalesLabel", LabeledMatchers.hasText("100.00 MMK"));

        TableView<Sale> table = robot.lookup("#recentSalesTable").queryTableView();
        assertEquals(1, table.getItems().size());
        assertEquals(100.0, table.getItems().get(0).getTotalAmount());

        // Verify mock
        assertEquals(1, mockedSaleDAO.constructed().size());
    }
}
