package com.pos.system.services;

import com.pos.system.models.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StoreEventsTest {

    @Test
    public void testLowStockEventCreation() {
        // Arrange
        Product product = new Product(1, "BC", "Test Product", 1, "Cat", 10.0, 20.0, 5, null);

        // Act
        StoreEvents.LowStockEvent event = new StoreEvents.LowStockEvent(product);

        // Assert
        assertSame(product, event.getProduct());
        assertEquals(5, event.getCurrentStock());
        assertEquals(StoreEvents.LowStockEvent.LOW_STOCK, event.getEventType());
    }
}
