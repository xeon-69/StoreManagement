package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.models.Category;
import com.pos.system.services.CategoryService;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class CategoryControllerTest {

    private MockedConstruction<CategoryService> mockedCategoryService;

    @Start
    public void start(Stage stage) throws Exception {
        // Setup resource bundle to avoid loadFXML exceptions
        App.setLocale("en");

        // Mock Service before controller initialization
        mockedCategoryService = mockConstruction(CategoryService.class, (mock, context) -> {
            when(mock.getAllCategories()).thenReturn(Arrays.asList(
                    new Category(1, "Electronics", "Devices"),
                    new Category(2, "Books", "Reading")));
        });
        // Use FXMLLoader programmatically to load just the Categories view
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/categories.fxml"));
        fxmlLoader.setResources(App.getBundle());
        VBox root = fxmlLoader.load();

        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedCategoryService != null) {
            mockedCategoryService.close();
        }
    }

    @Test
    void testCategoriesLoadedInTable(FxRobot robot) {
        // Assert: The table should have 2 rows loaded from our mocked DAO
        TableView<Category> table = robot.lookup("#categoryTable").queryAs(TableView.class);
        assertEquals(2, table.getItems().size());

        // Assert specific row content
        boolean foundElectronics = table.getItems().stream()
                .anyMatch(c -> c.getId() == 1 && "Electronics".equals(c.getName()));

        org.junit.jupiter.api.Assertions.assertTrue(foundElectronics, "Table should contain the Electronics category");
    }

    @Test
    void testSearchFilter(FxRobot robot) {
        // Act: Search for 'Books'
        robot.clickOn("#searchField").write("Books");

        // Assert: Table should only show 1 matching result
        TableView<Category> table = robot.lookup("#categoryTable").queryAs(TableView.class);
        assertEquals(1, table.getItems().size());

        boolean foundBooks = table.getItems().stream()
                .anyMatch(c -> c.getId() == 2 && "Books".equals(c.getName()));
        org.junit.jupiter.api.Assertions.assertTrue(foundBooks, "Table should contain the Books category");
    }
}
