package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.models.Product;
import com.pos.system.services.StockAdjustmentService;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AdjustStockControllerTest {

    private StockAdjustmentService mockAdjustmentService;
    private AdjustStockController controller;
    private Stage mainStage;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");
        mainStage = stage;
    }

    @BeforeEach
    public void setUp() {
        mockAdjustmentService = mock(StockAdjustmentService.class);
    }

    private void loadFXML() throws Exception {
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/adjust_stock.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        controller.setStockAdjustmentService(mockAdjustmentService);

        Product p = new Product(1, "123", "Test Product", 1, "Category", 10.0, 20.0, 100, null);
        controller.setProductContext(p);

        org.testfx.util.WaitForAsyncUtils.asyncFx(() -> {
            mainStage.setScene(new Scene(root, 400, 300));
            mainStage.show();
        }).get();
    }

    @Test
    public void testAdjustStockInvalid(FxRobot robot) throws Exception {
        loadFXML();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#qtyField").write("invalid");
        robot.clickOn("#saveBtn");

        WaitForAsyncUtils.waitForFxEvents();
        Thread.sleep(300);
        WaitForAsyncUtils.waitForFxEvents();

        // Verify the adjustment service was never called due to invalid input
        verify(mockAdjustmentService, never()).adjustStock(anyInt(), anyInt(), anyString(), any());
    }

    @Test
    public void testAdjustStockValid(FxRobot robot) throws Exception {
        loadFXML();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#qtyField").write("10");
        robot.clickOn("#reasonArea").write("Test Reason");
        robot.clickOn("#saveBtn");

        WaitForAsyncUtils.waitForFxEvents();

        verify(mockAdjustmentService, times(1)).adjustStock(eq(1), eq(10), eq("Test Reason"), any());
    }
}
