package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.ExpenseDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.models.Expense;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.pos.system.utils.SessionManager;
import com.pos.system.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class FinanceControllerTest {

    @Mock
    private ExpenseDAO mockExpenseDAO;

    @Mock
    private SaleDAO mockSaleDAO;

    private AutoCloseable mocks;
    private FinanceController controller;

    @Start
    public void start(Stage stage) throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        App.setLocale("en");

        // Mock data
        Expense e1 = new Expense(1, "Supplies", 50.0, "Paper", LocalDateTime.now());
        when(mockExpenseDAO.getExpensesCountBetween(any(), any())).thenReturn(1);
        when(mockExpenseDAO.getPaginatedExpensesBetween(any(), any(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(e1));
        when(mockExpenseDAO.getTotalExpensesBetween(any(), any())).thenReturn(50.0);
        when(mockSaleDAO.getTotalSalesBetween(any(), any())).thenReturn(100.0);

        // Set Session
        User admin = new User(1, "admin", "password", "ADMIN");
        SessionManager.getInstance().setCurrentUser(admin);

        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/finance.fxml"));
        fxmlLoader.setResources(App.getBundle());

        // Subclass FinanceController to override createExpenseDAO
        fxmlLoader.setControllerFactory(param -> {
            controller = new FinanceController() {
                @Override
                protected ExpenseDAO createExpenseDAO() {
                    return mockExpenseDAO;
                }

                @Override
                protected SaleDAO createSaleDAO() {
                    return mockSaleDAO;
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
        SessionManager.getInstance().logout();
    }

    @Test
    public void testFinanceLoadsDataOnStart(FxRobot robot) throws Exception {
        // Wait for async load (increased timeout)
        WaitForAsyncUtils.waitFor(10, java.util.concurrent.TimeUnit.SECONDS, () -> {
            TableView<Expense> table = robot.lookup("#expenseTable").queryAs(TableView.class);
            return table.getItems().size() == 1;
        });

        @SuppressWarnings("unchecked")
        TableView<Expense> table = robot.lookup("#expenseTable").queryAs(TableView.class);
        assertEquals(1, table.getItems().size());
        assertEquals("Supplies", table.getItems().get(0).getCategory());
        assertEquals(50.0, table.getItems().get(0).getAmount());
    }

    @Test
    public void testAddExpense(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();

        TextField categoryField = robot.lookup("#categoryField").queryAs(TextField.class);
        TextField amountField = robot.lookup("#amountField").queryAs(TextField.class);
        TextField descField = robot.lookup("#descField").queryAs(TextField.class);

        robot.clickOn(categoryField).write("Utilities");
        robot.clickOn(amountField).write("100.0");
        robot.clickOn(descField).write("Internet");

        robot.clickOn(".btn-success");

        // Wait for async background work (increased timeout)
        WaitForAsyncUtils.waitFor(10, java.util.concurrent.TimeUnit.SECONDS, () -> {
            try {
                verify(mockExpenseDAO, atLeastOnce()).addExpense(any(Expense.class));
                return true;
            } catch (Throwable t) {
                return false;
            }
        });

        WaitForAsyncUtils.waitForFxEvents();

        // Verify DAO was called to add Expense
        verify(mockExpenseDAO, times(1)).addExpense(any(Expense.class));
    }
}
