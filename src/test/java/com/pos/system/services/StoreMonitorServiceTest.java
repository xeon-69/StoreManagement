package com.pos.system.services;

import com.pos.system.dao.AuditLogDAO;
import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import com.pos.system.utils.NotificationUtils;
import com.pos.system.App;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class StoreMonitorServiceTest {

    private MockedConstruction<ProductDAO> mockedProductDAO;
    private MockedConstruction<AuditLogDAO> mockedAuditLogDAO;
    private MockedStatic<Platform> mockedPlatform;
    private MockedStatic<NotificationUtils> mockedNotificationUtils;

    @BeforeEach
    public void setUp() {

        // Mock ProductDAO
        mockedProductDAO = mockConstruction(ProductDAO.class, (mock, context) -> {
            List<Product> products = new ArrayList<>();
            // One normal product
            products.add(new Product(1, "BC1", "Item 1", 1, "Cat", 10.0, 20.0, 50, null));
            // One low stock product (Threshold is 10)
            products.add(new Product(2, "BC2", "Item 2", 1, "Cat", 10.0, 20.0, 5, null));
            when(mock.getAllProducts()).thenReturn(products);
        });

        mockedAuditLogDAO = mockConstruction(AuditLogDAO.class);

        // Mock Platform.runLater to execute immediately
        mockedPlatform = mockStatic(Platform.class);
        mockedPlatform.when(() -> Platform.runLater(any(Runnable.class)))
                .thenAnswer((InvocationOnMock invocation) -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                });

        // Mock NotificationUtils to avoid UI popups
        mockedNotificationUtils = mockStatic(NotificationUtils.class);
        mockedNotificationUtils.when(() -> NotificationUtils.showWarning(anyString(), anyString()))
                .thenAnswer(invocation -> null);
    }

    @AfterEach
    public void tearDown() {
        if (mockedProductDAO != null)
            mockedProductDAO.close();
        if (mockedAuditLogDAO != null)
            mockedAuditLogDAO.close();
        if (mockedPlatform != null)
            mockedPlatform.close();
        if (mockedNotificationUtils != null)
            mockedNotificationUtils.close();
    }

    @Test
    public void testMonitorTaskExecution() throws Exception {
        StoreMonitorService service = new StoreMonitorService();

        // Access protected createTask method because we are in the same package
        Task<Void> task = service.createTask();

        // Manually invoke call() using reflection
        java.lang.reflect.Method callMethod = Task.class.getDeclaredMethod("call");
        callMethod.setAccessible(true);
        callMethod.invoke(task);

        // Verify that NotificationUtils.showWarning was called precisely once (for the
        // low stock item)
        String expectedTitle = App.getBundle().getString("monitor.lowStock.title");
        mockedNotificationUtils.verify(() -> NotificationUtils.showWarning(eq(expectedTitle), contains("Item 2")),
                times(1));
    }
}
