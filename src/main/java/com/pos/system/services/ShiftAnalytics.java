package com.pos.system.services;

import com.pos.system.dao.SaleDAO;
import com.pos.system.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDateTime;

public class ShiftAnalytics {
    private static final Logger logger = LoggerFactory.getLogger(ShiftAnalytics.class);
    private LocalDateTime shiftStartTime;

    public ShiftAnalytics() {
        this.shiftStartTime = LocalDateTime.now(); // Assume app start is shift start for now
    }

    public void startNewShift() {
        this.shiftStartTime = LocalDateTime.now();
    }

    public double getCurrentShiftTotal() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                SaleDAO saleDAO = new SaleDAO(conn)) {
            // Note: SaleDAO needs a getTotalSalesSince method.
            // If it doesn't exist yet, we might need to add it or this will fail
            // compilation.
            // Checking previous steps, "getTotalSalesSince" was mentioned.
            return saleDAO.getTotalSalesSince(shiftStartTime);

        } catch (Exception e) {
            logger.error("Failed to calculate shift analytics", e);
            return 0.0;
        }
    }
}
