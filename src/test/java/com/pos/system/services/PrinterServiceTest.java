package com.pos.system.services;

import com.github.anastaciocintra.output.PrinterOutputStream;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class PrinterServiceTest {

    private PrinterService printerService;
    private ByteArrayOutputStream systemOutContent;

    @BeforeEach
    void setUp() {
        printerService = new PrinterService();
        systemOutContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOutContent));
    }

    @Test
    void testPrintReceipt_FallbackToConsole() {
        // Arrange
        Sale sale = new Sale(1, 1, 150.0, 50.0, LocalDateTime.now());
        List<SaleItem> items = new ArrayList<>();
        items.add(new SaleItem(1, 1, 101, "Test Item 1", 2, 50.0, 100.0));
        items.add(new SaleItem(2, 1, 102, "Test Item 2", 1, 50.0, 50.0));

        // Mock getDefaultPrintService to return null to force fallback
        try (MockedStatic<PrinterOutputStream> mockedPrinter = mockStatic(PrinterOutputStream.class)) {
            mockedPrinter.when(PrinterOutputStream::getDefaultPrintService).thenReturn(null);

            // Act
            printerService.printReceipt(sale, items, 200.0, 50.0);

            // Assert
            String output = systemOutContent.toString();
            assertTrue(output.contains("========= RECEIPT ========="));
            assertTrue(output.contains("Receipt No: 1"));
            assertTrue(output.contains("Test Item 1"));
            assertTrue(output.contains("Test Item 2"));
            assertTrue(output.contains("TOTAL:     $150.00"));
            assertTrue(output.contains("Tendered:  $200.00"));
            assertTrue(output.contains("Change:    $50.00"));
        }
    }

    @Test
    void testOpenCashDrawer_FallbackToConsole() {
        // Mock getDefaultPrintService to return null to force fallback
        try (MockedStatic<PrinterOutputStream> mockedPrinter = mockStatic(PrinterOutputStream.class)) {
            mockedPrinter.when(PrinterOutputStream::getDefaultPrintService).thenReturn(null);

            // Act
            printerService.openCashDrawer();

            // Assert
            String output = systemOutContent.toString();
            assertTrue(output.contains("--- VIRTUAL CASH DRAWER KICKED ---"));
        }
    }
}
