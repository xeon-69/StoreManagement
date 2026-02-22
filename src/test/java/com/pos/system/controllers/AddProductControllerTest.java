package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.CategoryDAO;
import com.pos.system.dao.ProductDAO;
import com.pos.system.database.DatabaseManager;
import com.pos.system.models.Category;
import com.pos.system.services.InventoryService;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
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

import java.sql.Connection;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AddProductControllerTest {

    private MockedConstruction<ProductDAO> mockedProductDAO;
    private MockedConstruction<CategoryDAO> mockedCategoryDAO;
    private MockedConstruction<InventoryService> mockedInventoryService;
    private MockedStatic<DatabaseManager> mockedDatabaseManager;

    private AddProductController controller;
    private boolean saveTriggered = false;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Mock DAOs and Services
        mockedProductDAO = mockConstruction(ProductDAO.class, (mock, context) -> {
            // Need a dummy product for the "newlyCreated" logic
            com.pos.system.models.Product dummy = new com.pos.system.models.Product(99, "12345", "Test", 1, "Cat", 10.0,
                    20.0, 0, null);
            when(mock.getProductByBarcode(anyString())).thenReturn(dummy);
        });

        mockedCategoryDAO = mockConstruction(CategoryDAO.class, (mock, context) -> {
            Category c1 = new Category(1, "Electronics", "Devices");
            Category c2 = new Category(2, "Grocery", "Food");
            when(mock.getAllCategories()).thenReturn(Arrays.asList(c1, c2));
        });

        mockedInventoryService = mockConstruction(InventoryService.class);

        // Mock DB Manager for InventoryService connection
        DatabaseManager mockDb = mock(DatabaseManager.class);
        when(mockDb.getConnection()).thenReturn(mock(Connection.class));
        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getInstance).thenReturn(mockDb);

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/add_product.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();
        controller.setOnSaveCallback(() -> saveTriggered = true);

        stage.setScene(new Scene(root, 600, 800));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedProductDAO != null)
            mockedProductDAO.close();
        if (mockedCategoryDAO != null)
            mockedCategoryDAO.close();
        if (mockedInventoryService != null)
            mockedInventoryService.close();
        if (mockedDatabaseManager != null)
            mockedDatabaseManager.close();
    }

    @Test
    public void testAddNewProduct(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();

        // Fill form
        robot.clickOn("#barcodeField").write("12345");
        robot.clickOn("#nameField").write("New Item");

        ComboBox<Category> categoryCombo = robot.lookup("#categoryComboBox").queryAs(ComboBox.class);
        robot.interact(() -> categoryCombo.getSelectionModel().select(0)); // Electronics

        robot.clickOn("#costPriceField").write("10.50");
        robot.clickOn("#sellingPriceField").write("20.00");

        TextField stockField = robot.lookup("#stockField").queryAs(TextField.class);
        robot.doubleClickOn(stockField).write("50");

        // Click Save (.btn-success is the save button)
        robot.clickOn(".btn-success");

        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(saveTriggered, "onSaveCallback should be called");

        // Verify mocks
        assertEquals(1, mockedProductDAO.constructed().size());
        verify(mockedProductDAO.constructed().get(0), times(1)).addProduct(any());

        assertEquals(1, mockedInventoryService.constructed().size()); // Because stock > 0
        verify(mockedInventoryService.constructed().get(0), times(1)).addStock(any(), eq(99), eq(50), eq(10.0),
                isNull(), eq("INITIAL-ADD"), isNull());
    }
}
