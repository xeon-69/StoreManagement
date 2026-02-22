package com.pos.system.services;

import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.database.DatabaseManager;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.SalePayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportingService {
    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a comprehensive Daily Z-Report as a CSV file in the reports/
     * directory.
     * Includes full transaction details: items, payments, discounts, change, etc.
     *
     * @return the File object of the generated report
     */
    public File generateDailyZReport() throws SQLException, IOException {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            SaleDAO saleDAO = new SaleDAO(conn);
            SalePaymentDAO paymentDAO = new SalePaymentDAO(conn);

            double totalSales = saleDAO.getTotalSalesBetween(startOfDay, endOfDay);
            double totalProfit = saleDAO.getTotalProfitBetween(startOfDay, endOfDay);
            List<Sale> todaySales = saleDAO.getSalesBetween(startOfDay, endOfDay);

            // Create reports directory
            File reportsDir = new File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File reportFile = new File(reportsDir, "z-report-" + dateStr + ".csv");

            try (PrintWriter w = new PrintWriter(new FileWriter(reportFile))) {
                // ── Report Header ──
                w.println("========================================");
                w.println("Z-REPORT — " + dateStr);
                w.println("Generated: " + LocalDateTime.now().format(DT_FMT));
                w.println("========================================");
                w.println();

                // ── Daily Summary ──
                w.println("DAILY SUMMARY");
                w.println("Total Revenue," + fmt(totalSales));
                w.println("Total Profit," + fmt(totalProfit));
                w.println("Transaction Count," + todaySales.size());
                w.println();

                // ── Per-transaction details ──
                for (Sale sale : todaySales) {
                    List<SaleItem> items = saleDAO.getItemsBySaleId(sale.getId());
                    List<SalePayment> payments = paymentDAO.findBySaleId(sale.getId());

                    double totalPaid = payments.stream().mapToDouble(SalePayment::getAmount).sum();
                    double change = totalPaid - sale.getTotalAmount();

                    w.println("----------------------------------------");
                    w.printf("TRANSACTION #%d%n", sale.getId());
                    w.println("Date/Time," + (sale.getSaleDate() != null ? sale.getSaleDate().format(DT_FMT) : ""));
                    w.println("User ID," + sale.getUserId());
                    if (sale.getShiftId() != null) {
                        w.println("Shift ID," + sale.getShiftId());
                    }
                    w.println();

                    // Items
                    w.println("ITEMS");
                    w.println("Product,Qty,Unit Price,Cost,Item Discount,Item Tax,Line Total");
                    for (SaleItem item : items) {
                        w.printf("%s,%d,%s,%s,%s,%s,%s%n",
                                escapeCsv(item.getProductName()),
                                item.getQuantity(),
                                fmt(item.getPriceAtSale()),
                                fmt(item.getCostAtSale()),
                                fmt(item.getDiscountAmount()),
                                fmt(item.getTaxAmount()),
                                fmt(item.getTotal()));
                    }
                    w.println();

                    // Sale totals
                    w.println("SALE TOTALS");
                    w.println("Subtotal," + fmt(sale.getSubtotal()));
                    w.println("Discount," + fmt(sale.getDiscountAmount()));
                    w.println("Tax," + fmt(sale.getTaxAmount()));
                    w.println("Grand Total," + fmt(sale.getTotalAmount()));
                    w.println("Profit," + fmt(sale.getTotalProfit()));
                    w.println();

                    // Payments
                    w.println("PAYMENTS");
                    w.println("Method,Amount");
                    for (SalePayment p : payments) {
                        w.printf("%s,%s%n", escapeCsv(p.getPaymentMethod()), fmt(p.getAmount()));
                    }
                    w.println("Total Paid," + fmt(totalPaid));
                    if (change > 0) {
                        w.println("Change," + fmt(change));
                    }
                    w.println();
                }

                w.println("========================================");
                w.println("END OF Z-REPORT");
                w.println("========================================");
            }

            logger.info("Z-Report generated: {}", reportFile.getAbsolutePath());
            return reportFile;
        }
    }

    private static String fmt(double value) {
        return String.format("%,.2f", value);
    }

    private static String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
