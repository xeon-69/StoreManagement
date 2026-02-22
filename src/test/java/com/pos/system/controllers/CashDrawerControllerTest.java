package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.CashDrawerTransactionDAO;
import com.pos.system.models.CashDrawerTransaction;
import com.pos.system.models.Shift;
import com.pos.system.utils.SessionManager;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class CashDrawerControllerTest {

    private MockedConstruction<CashDrawerTransactionDAO> mockedDrawerDAO;
    private MockedStatic<SessionManager> mockedSessionManager;
    private SessionManager mockSession;
    private CashDrawerController controller;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // mock session
        mockSession = mock(SessionManager.class);
        Shift activeShift = new Shift();
        activeShift.setId(99);
        when(mockSession.getCurrentShift()).thenReturn(activeShift);

        mockedSessionManager = mockStatic(SessionManager.class);
        mockedSessionManager.when(SessionManager::getInstance).thenReturn(mockSession);

        // Mock DAO
        mockedDrawerDAO = mockConstruction(CashDrawerTransactionDAO.class, (mock, context) -> {
            CashDrawerTransaction trx = new CashDrawerTransaction(1, 99, 1, 100.0, "IN", "Test In",
                    LocalDateTime.now());
            when(mock.findByShiftId(99)).thenReturn(Arrays.asList(trx));
        });

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/cash_drawer.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedDrawerDAO != null)
            mockedDrawerDAO.close();
        if (mockedSessionManager != null)
            mockedSessionManager.close();
    }

    @Test
    public void testTransactionsLoaded(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();

        TableView<CashDrawerTransaction> table = robot.lookup("#drawerTable").queryTableView();
        assertEquals(1, table.getItems().size());
        assertEquals("IN", table.getItems().get(0).getTransactionType());
        assertEquals(100.0, table.getItems().get(0).getAmount());

        // Verify mock
        assertEquals(1, mockedDrawerDAO.constructed().size());
        verify(mockedDrawerDAO.constructed().get(0), times(1)).findByShiftId(99);
    }
}
