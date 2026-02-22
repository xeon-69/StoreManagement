package com.pos.system.services;

import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;
import com.pos.system.utils.NotificationUtils;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StoreMonitorService extends ScheduledService<Void> {
    private static final Logger logger = LoggerFactory.getLogger(StoreMonitorService.class);
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ShiftAnalytics analytics = new ShiftAnalytics();
    // Track products already notified so we only alert once per product
    private final Set<Integer> notifiedProductIds = new HashSet<>();

    public StoreMonitorService() {
        this.setPeriod(Duration.seconds(30)); // Check every 30 seconds
        this.setDelay(Duration.seconds(5)); // Start after 5 seconds
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                logger.debug("Store Monitor: Starting background check...");

                checkLowStock();
                calculateShiftAnalytics();

                return null;
            }
        };
    }

    private void checkLowStock() {
        try (ProductDAO productDAO = new ProductDAO()) {
            List<Product> products = productDAO.getAllProducts();
            for (Product p : products) {
                if (p.getStock() <= LOW_STOCK_THRESHOLD) {
                    // Only notify once per product
                    if (notifiedProductIds.add(p.getId())) {
                        Platform.runLater(() -> {
                            logger.warn("Low Stock Alert: {} ({} left)", p.getName(), p.getStock());
                            NotificationUtils.showWarning("Low Stock",
                                    "Product " + p.getName() + " is low on stock (" + p.getStock() + ")");
                        });
                    }
                } else {
                    // Stock recovered — allow future re-notification
                    notifiedProductIds.remove(p.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Monitor failed to check stock", e);
        }
    }

    private void calculateShiftAnalytics() {
        double total = analytics.getCurrentShiftTotal();
        logger.info("Store Monitor: Current Shift Total: ${}", total);
    }
}
