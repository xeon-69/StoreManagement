package com.pos.system.services;

import com.pos.system.models.Product;
import javafx.event.Event;
import javafx.event.EventType;

public class StoreEvents {

    public static class LowStockEvent extends Event {
        public static final EventType<LowStockEvent> LOW_STOCK = new EventType<>(Event.ANY, "LOW_STOCK");

        private final Product product;
        private final int currentStock;

        public LowStockEvent(Product product) {
            super(LOW_STOCK);
            this.product = product;
            this.currentStock = product.getStock();
        }

        public Product getProduct() {
            return product;
        }

        public int getCurrentStock() {
            return currentStock;
        }
    }
}
