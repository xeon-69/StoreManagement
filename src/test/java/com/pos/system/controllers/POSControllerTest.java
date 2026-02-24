package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.models.Product;
import com.pos.system.viewmodels.ProductCatalogViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class POSControllerTest {

    @Mock
    private ProductCatalogViewModel mockViewModel;

    private AutoCloseable mocks;
    private POSController controller;

    @Start
    public void start(Stage stage) throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        App.setLocale("en");

        // Prepare Mock Data
        Product p1 = new Product(1, "123", "Apple", 1, "Fruits", 100.0, 150.0, 50, new byte[0]);
        Product p2 = new Product(2, "456", "Banana", 1, "Fruits", 50.0, 80.0, 100, new byte[0]);

        when(mockViewModel.getFilteredProducts()).thenReturn(FXCollections.observableArrayList(p1, p2));
        doNothing().when(mockViewModel).loadProducts();

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pos.fxml"));
        fxmlLoader.setResources(App.getBundle());

        // Set Controller Factory to inject our mock
        fxmlLoader.setControllerFactory(param -> {
            controller = new POSController();
            controller.setCatalogViewModel(mockViewModel);
            return controller;
        });

        javafx.scene.Parent root = fxmlLoader.load();
        stage.setScene(new Scene(root, 1024, 768));
        stage.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testProductsLoadedAndTotalUpdates(FxRobot robot) {
        // Assert: We should see "Apple" and "Banana" in the UI (rendered as Labels
        // inside GridCell)
        FxAssert.verifyThat("Apple", LabeledMatchers.hasText("Apple"));
        FxAssert.verifyThat("Banana", LabeledMatchers.hasText("Banana"));

        WaitForAsyncUtils.waitForFxEvents(); // Wait for GridView cells to stamp

        // Act: Click the "+" button for Apple to add it to the cart
        javafx.scene.Node btn = robot.lookup("#add-btn-1").query();
        robot.clickOn(btn);

        // Assert: The total label should update
        WaitForAsyncUtils.waitForFxEvents(); // Wait for JavaFX thread to update
        Label totalLabel = robot.lookup("#totalLabel").queryAs(Label.class);
        assertEquals("150.00 MMK", totalLabel.getText());

        // Assert: Cart table should have 1 item
        TableView<?> cartTable = robot.lookup("#cartTable").queryAs(TableView.class);
        assertEquals(1, cartTable.getItems().size());
    }

    @Test
    void testSearchFunctionality(FxRobot robot) {
        // Act: Type in search field
        robot.clickOn("#searchField").write("Ap");

        // Wait for debounce timer (300ms + margin)
        WaitForAsyncUtils.sleep(500, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Assert: The ViewModel's search method was called
        verify(mockViewModel, atLeastOnce()).search(contains("Ap"));
    }
}
