package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.AuditLogDAO;
import com.pos.system.models.AuditLog;
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
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AuditLogControllerTest {

    private MockedConstruction<AuditLogDAO> mockedAuditLogDAO;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Mock DAO
        mockedAuditLogDAO = mockConstruction(AuditLogDAO.class, (mock, context) -> {
            AuditLog log1 = new AuditLog(1, 1, "LOGIN", "User", "1", "User logged in", LocalDateTime.now());
            when(mock.getTotalCount()).thenReturn(1);
            when(mock.getPaginatedLogs(anyInt(), anyInt())).thenReturn(Arrays.asList(log1));
        });

        // Load FXML
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/audit_logs.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        fxmlLoader.getController();

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedAuditLogDAO != null)
            mockedAuditLogDAO.close();
    }

    @Test
    public void testAuditLogsLoaded(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();

        TableView<AuditLog> table = robot.lookup("#auditTable").queryTableView();
        assertEquals(1, table.getItems().size());
        assertEquals("LOGIN", table.getItems().get(0).getAction());
        assertEquals("User", table.getItems().get(0).getEntityName());

        // verify mocks
        assertTrue(mockedAuditLogDAO.constructed().size() >= 1);
        verify(mockedAuditLogDAO.constructed().get(0), atLeastOnce()).getTotalCount();
    }
}
