package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.ExpenseDAO;
import com.pos.system.models.Expense;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class FinanceControllerTest {

    @Mock
    private ExpenseDAO mockExpenseDAO;

    private AutoCloseable mocks;
    private FinanceController controller;

    @Start
    public void start(Stage stage) throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        App.setLocale("en");

        // Mock data
        Expense e1 = new Expense(1, "Supplies", 50.0, "Paper", LocalDateTime.now());
        when(mockExpenseDAO.getAllExpenses()).thenReturn(Arrays.asList(e1));

        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/finance.fxml"));
        fxmlLoader.setResources(App.getBundle());

        // Subclass FinanceController to override createExpenseDAO
        fxmlLoader.setControllerFactory(param -> {
            controller = new FinanceController() {
                @Override
                protected ExpenseDAO createExpenseDAO() {
                    return mockExpenseDAO;
                }
            };
            return controller;
        });

        VBox root = fxmlLoader.load();
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testFinanceLoadsDataOnStart(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        TableView<Expense> table = robot.lookup("#expenseTable").queryAs(TableView.class);
        assertEquals(1, table.getItems().size());
        assertEquals("Supplies", table.getItems().get(0).getCategory());
        assertEquals(50.0, table.getItems().get(0).getAmount());
    }

    @Test
    public void testAddExpense(FxRobot robot) throws SQLException {
        WaitForAsyncUtils.waitForFxEvents();

        TextField categoryField = robot.lookup("#categoryField").queryAs(TextField.class);
        TextField amountField = robot.lookup("#amountField").queryAs(TextField.class);
        TextField descField = robot.lookup("#descField").queryAs(TextField.class);

        robot.clickOn(categoryField).write("Utilities");
        robot.clickOn(amountField).write("100.0");
        robot.clickOn(descField).write("Internet");

        robot.clickOn(".btn-success");

        WaitForAsyncUtils.waitForFxEvents();

        // Verify DAO was called to add Expense
        verify(mockExpenseDAO, times(1)).addExpense(any(Expense.class));

        // It should also reload the table, calling getAllExpenses a second time
        verify(mockExpenseDAO, times(2)).getAllExpenses();
    }
}
