package com.pos.system.services;

import com.pos.system.dao.SaleDAO;
import com.pos.system.database.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ShiftAnalyticsTest {

    private MockedStatic<DatabaseManager> mockedDatabaseManager;
    private DatabaseManager mockDbManager;
    private Connection mockConnection;
    private MockedConstruction<SaleDAO> mockedSaleDAO;
    private SaleDAO mockSaleDAO;

    @BeforeEach
    public void setUp() throws Exception {
        mockDbManager = mock(DatabaseManager.class);
        mockConnection = mock(Connection.class);

        when(mockDbManager.getConnection()).thenReturn(mockConnection);

        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);

        // Mock SaleDAO constructor
        mockedSaleDAO = mockConstruction(SaleDAO.class, (mock, context) -> {
            when(mock.getTotalSalesBetween(any(), any())).thenReturn(150.50);
            this.mockSaleDAO = mock;
        });
    }

    @AfterEach
    public void tearDown() {
        mockedDatabaseManager.close();
        mockedSaleDAO.close();
    }

    @Test
    public void testGetCurrentShiftTotal() {
        // Arrange
        ShiftAnalytics analytics = new ShiftAnalytics();

        // Act
        double total = analytics.getCurrentShiftTotal();

        // Assert
        assertEquals(150.50, total, 0.001);
    }
}
