package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.CategoryDAO;
import javafx.scene.Scene;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AddCategoryControllerTest {

    private MockedConstruction<CategoryDAO> mockedCategoryDAO;
    private AddCategoryController controller;
    private boolean saveTriggered = false;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Mock DAO
        mockedCategoryDAO = mockConstruction(CategoryDAO.class);

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/add_category.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();
        controller.setOnSaveCallback(() -> saveTriggered = true);

        stage.setScene(new Scene(root, 500, 400));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedCategoryDAO != null)
            mockedCategoryDAO.close();
    }

    @Test
    public void testAddNewCategory(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();

        // Fill form
        robot.clickOn("#nameField").write("Test Category");
        robot.clickOn("#descriptionArea").write("Test Description");

        // Click Save (.btn-success is the save button)
        robot.clickOn(".btn-success");

        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(saveTriggered, "onSaveCallback should be called");

        // Verify mocks
        assertEquals(1, mockedCategoryDAO.constructed().size());
        verify(mockedCategoryDAO.constructed().get(0), times(1)).addCategory(any());
    }
}
