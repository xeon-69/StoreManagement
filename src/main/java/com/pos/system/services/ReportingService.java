package com.pos.system.services;

import com.pos.system.App;
import com.pos.system.dao.AuditLogDAO;
import com.pos.system.dao.ExpenseDAO;
import com.pos.system.dao.InventoryTransactionDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.database.DatabaseManager;
import com.pos.system.models.AuditLog;
import com.pos.system.models.Expense;
import com.pos.system.models.InventoryTransaction;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.SalePayment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportingService {
    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private enum AggregationLevel {
        AUTO, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    /**
     * Generates a comprehensive Daily Z-Report as a CSV file in the reports/
     * directory.
     * Includes full transaction details: items, payments, discounts, change, etc.
     *
     * @return the File object of the generated report
     */
    public File generateDailyZReportCSV() throws SQLException, IOException {
        return generateRangeReportCSV(LocalDate.now(), LocalDate.now());
    }

    public File generateDailyZReportExcel() throws SQLException, IOException {
        return generateRangeReportExcel(LocalDate.now(), LocalDate.now());
    }

    public File generateRangeReportCSV(LocalDate start, LocalDate end) throws SQLException, IOException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                SaleDAO saleDAO = new SaleDAO(conn);
                SalePaymentDAO paymentDAO = new SalePaymentDAO(conn);
                InventoryTransactionDAO txDAO = new InventoryTransactionDAO(conn);
                AuditLogDAO auditLogDAO = new AuditLogDAO(conn);
                ExpenseDAO expenseDAO = new ExpenseDAO(conn)) {

            File reportsDir = new File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            String dateStr = start.equals(end) ? start.toString() : start.toString() + "_to_" + end.toString();
            File reportFile = new File(reportsDir, "report-" + dateStr + ".csv");

            try (PrintWriter w = new PrintWriter(new FileWriter(reportFile))) {
                java.util.ResourceBundle b = App.getBundle();
                w.println(b.getString("app.title"));
                w.println(String.format(b.getString("report.csv.range"), start, end));
                w.println(String.format(b.getString("report.csv.generated"), LocalDateTime.now().format(DT_FMT)));
                w.println();

                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    LocalDateTime startOfDay = date.atStartOfDay();
                    LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

                    double totalSales = saleDAO.getTotalSalesBetween(startOfDay, endOfDay);
                    double totalProfit = saleDAO.getTotalProfitBetween(startOfDay, endOfDay);
                    List<Sale> todaySales = saleDAO.getSalesBetween(startOfDay, endOfDay);

                    w.println("========================================");
                    w.println(String.format(b.getString("report.csv.date"), date));
                    w.println("========================================");
                    w.println(b.getString("report.excel.summaryTitle"));
                    w.println(b.getString("report.excel.totalRevenue") + "," + fmt(totalSales));
                    w.println(b.getString("report.excel.totalProfit") + "," + fmt(totalProfit));
                    w.println(b.getString("report.excel.transactionCount") + "," + todaySales.size());
                    w.println();

                    for (Sale sale : todaySales) {
                        List<SalePayment> payments = paymentDAO.findBySaleId(sale.getId());
                        StringBuilder payInfo = new StringBuilder();
                        double totalPaid = 0;
                        for (SalePayment p : payments) {
                            if (payInfo.length() > 0)
                                payInfo.append(" | ");
                            payInfo.append(p.getPaymentMethod()).append(": ").append(fmt(p.getAmount()));
                            totalPaid += p.getAmount();
                        }
                        double change = totalPaid - sale.getTotalAmount();

                        w.println(String.format(b.getString("report.csv.transaction"), sale.getId()) + ","
                                + (sale.getSaleDate() != null ? sale.getSaleDate().format(DT_FMT) : "")
                                + "," + b.getString("report.excel.total") + ": " + fmt(sale.getTotalAmount())
                                + "," + b.getString("report.excel.payMethods") + ": [" + payInfo.toString() + "]"
                                + "," + b.getString("report.excel.totalPaid") + ": " + fmt(totalPaid)
                                + "," + b.getString("report.excel.change") + ": " + fmt(Math.max(0, change)));
                    }
                    w.println();
                }

                // Inventory Activity Section
                List<InventoryTransaction> allTx = txDAO.getTransactionsBetween(start.atStartOfDay(),
                        end.atTime(LocalTime.MAX));
                List<AuditLog> allLogs = auditLogDAO.getLogsByDateRange(start.atStartOfDay(),
                        end.atTime(LocalTime.MAX));

                w.println("========================================");
                w.println(b.getString("report.excel.invActivity").toUpperCase());
                w.println("========================================");
                w.println(b.getString("report.excel.invTime") + "," + b.getString("report.excel.invProduct") + ","
                        + b.getString("report.excel.invType") + "," + b.getString("report.excel.invQtyChange") + ","
                        + b.getString("report.excel.invAction") + "," + b.getString("report.excel.invDetails"));

                for (InventoryTransaction tx : allTx) {
                    w.println(tx.getCreatedAt().format(DT_FMT) + "," + tx.getProductId() + ","
                            + b.getString("transaction.type." + tx.getTransactionType()) + ","
                            + tx.getQuantityChange() + "," + tx.getTransactionType() + "," + tx.getReferenceId());
                }
                for (AuditLog log : allLogs) {
                    if (log.getAction().contains("Price") || log.getAction().contains("Cost")) {
                        w.println(log.getCreatedAt().format(DT_FMT) + "," + log.getEntityId() + ","
                                + b.getString("transaction.type.VALUE_CHANGE") + ",0,"
                                + log.getAction() + "," + log.getDetails());
                    }
                }
            }

            logger.info("Range Report CSV generated: {}", reportFile.getAbsolutePath());
            return reportFile;
        }
    }

    public File generateRangeReportExcel(LocalDate start, LocalDate end) throws SQLException, IOException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                SaleDAO saleDAO = new SaleDAO(conn);
                SalePaymentDAO paymentDAO = new SalePaymentDAO(conn);
                InventoryTransactionDAO txDAO = new InventoryTransactionDAO(conn);
                AuditLogDAO auditLogDAO = new AuditLogDAO(conn);
                ExpenseDAO expenseDAO = new ExpenseDAO(conn)) {

            File reportsDir = new File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            String dateStr = start.equals(end) ? start.toString() : start.toString() + "_to_" + end.toString();
            File reportFile = new File(reportsDir, "report-" + dateStr + ".xlsx");

            try (Workbook workbook = new XSSFWorkbook()) {
                // Professional Styles
                CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle summaryStyle = createSummaryStyle(workbook);
                CellStyle currencyStyle = createCurrencyStyle(workbook);
                CellStyle boldStyle = createBoldStyle(workbook);
                java.util.ResourceBundle b = App.getBundle();

                int latestDateSheetIndex = -1;
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    LocalDateTime startOfDay = date.atStartOfDay();
                    LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

                    List<Sale> todaySales = saleDAO.getSalesBetween(startOfDay, endOfDay);
                    if (todaySales.isEmpty() && !start.equals(end))
                        continue;

                    Sheet sheet = workbook.createSheet(date.toString());
                    int rowNum = 0;
                    latestDateSheetIndex = workbook.getNumberOfSheets() - 1;

                    // Title
                    Row titleRow = sheet.createRow(rowNum++);
                    Cell titleCell = titleRow.createCell(0);
                    titleCell.setCellValue(String.format(b.getString("report.excel.dailyTitle"), date));
                    titleCell.setCellStyle(headerStyle);
                    rowNum++;

                    // Summary
                    double totalSales = saleDAO.getTotalSalesBetween(startOfDay, endOfDay);
                    double totalProfit = saleDAO.getTotalProfitBetween(startOfDay, endOfDay);
                    double totalCogs = totalSales - totalProfit;
                    double totalDayExpenses = expenseDAO.getTotalExpensesBetween(startOfDay, endOfDay);
                    double netProfit = totalProfit - totalDayExpenses;

                    Row summaryTitle = sheet.createRow(rowNum++);
                    Cell stCell = summaryTitle.createCell(0);
                    stCell.setCellValue(b.getString("report.excel.summaryTitle"));
                    stCell.setCellStyle(boldStyle);

                    Row revRow = sheet.createRow(rowNum++);
                    revRow.createCell(0).setCellValue(b.getString("report.excel.totalRevenue"));
                    Cell revVal = revRow.createCell(1);
                    revVal.setCellValue(totalSales);
                    revVal.setCellStyle(currencyStyle);

                    Row cogsRow = sheet.createRow(rowNum++);
                    cogsRow.createCell(0).setCellValue(b.getString("report.excel.coGs"));
                    Cell cogsVal = cogsRow.createCell(1);
                    cogsVal.setCellValue(totalCogs);
                    cogsVal.setCellStyle(currencyStyle);

                    Row profRow = sheet.createRow(rowNum++);
                    profRow.createCell(0).setCellValue(b.getString("report.excel.grossProfit"));
                    Cell profVal = profRow.createCell(1);
                    profVal.setCellValue(totalProfit);
                    profVal.setCellStyle(currencyStyle);

                    Row expSumRow = sheet.createRow(rowNum++);
                    expSumRow.createCell(0).setCellValue(b.getString("finance.totalExpenses"));
                    Cell expSumVal = expSumRow.createCell(1);
                    expSumVal.setCellValue(totalDayExpenses);
                    expSumVal.setCellStyle(currencyStyle);

                    Row netRow = sheet.createRow(rowNum++);
                    netRow.createCell(0).setCellValue(b.getString("report.excel.netProfit"));
                    Cell netVal = netRow.createCell(1);
                    netVal.setCellValue(netProfit);
                    netVal.setCellStyle(currencyStyle);

                    Row countRow = sheet.createRow(rowNum++);
                    countRow.createCell(0).setCellValue(b.getString("report.excel.transactionCount"));
                    countRow.createCell(1).setCellValue(todaySales.size());
                    rowNum += 2;

                    // Transactions Table Header
                    Row tableHead = sheet.createRow(rowNum++);
                    String[] headers = {
                            b.getString("report.excel.id"),
                            b.getString("report.excel.time"),
                            b.getString("report.excel.items"),
                            b.getString("report.excel.subtotal"),
                            b.getString("report.excel.discount"),
                            b.getString("report.excel.tax"),
                            b.getString("report.excel.total"),
                            b.getString("report.excel.profit"),
                            b.getString("report.excel.payMethods"),
                            b.getString("report.excel.totalPaid"),
                            b.getString("report.excel.change")
                    };
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = tableHead.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(summaryStyle);
                    }

                    for (Sale sale : todaySales) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(sale.getId());
                        row.createCell(1).setCellValue(sale.getSaleDate() != null
                                ? sale.getSaleDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                : "");

                        List<SaleItem> items = saleDAO.getItemsBySaleId(sale.getId());
                        StringBuilder itemsDesc = new StringBuilder();
                        for (SaleItem item : items) {
                            if (itemsDesc.length() > 0)
                                itemsDesc.append(", ");
                            itemsDesc.append(item.getProductName()).append(" (x").append(item.getQuantity())
                                    .append(")");
                        }
                        row.createCell(2).setCellValue(itemsDesc.toString());

                        Cell c3 = row.createCell(3);
                        c3.setCellValue(sale.getSubtotal());
                        c3.setCellStyle(currencyStyle);
                        Cell c4 = row.createCell(4);
                        c4.setCellValue(sale.getDiscountAmount());
                        c4.setCellStyle(currencyStyle);
                        Cell c5 = row.createCell(5);
                        c5.setCellValue(sale.getTaxAmount());
                        c5.setCellStyle(currencyStyle);
                        Cell c6 = row.createCell(6);
                        c6.setCellValue(sale.getTotalAmount());
                        c6.setCellStyle(currencyStyle);
                        Cell c7 = row.createCell(7);
                        c7.setCellValue(sale.getTotalProfit());
                        c7.setCellStyle(currencyStyle);

                        List<SalePayment> payments = paymentDAO.findBySaleId(sale.getId());
                        StringBuilder paySummary = new StringBuilder();
                        double totalPaid = 0;
                        for (SalePayment p : payments) {
                            if (paySummary.length() > 0)
                                paySummary.append(", ");
                            paySummary.append(p.getPaymentMethod()).append(" (").append(fmt(p.getAmount())).append(")");
                            totalPaid += p.getAmount();
                        }
                        row.createCell(8).setCellValue(paySummary.toString());

                        Cell c9 = row.createCell(9);
                        c9.setCellValue(totalPaid);
                        c9.setCellStyle(currencyStyle);
                        Cell c10 = row.createCell(10);
                        c10.setCellValue(Math.max(0, totalPaid - sale.getTotalAmount()));
                        c10.setCellStyle(currencyStyle);
                    }

                    for (int i = 0; i < headers.length; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    // --- Charts Section ---
                    rowNum += 2;
                    // Visual Analytics moved to bottom of sheet to avoid duplication
                    List<AuditLog> logs = auditLogDAO.getLogsByDateRange(startOfDay, endOfDay);

                    // --- Inventory Activity Section ---
                    rowNum += 2;
                    Row invHeader = sheet.createRow(rowNum++);
                    invHeader.createCell(0).setCellValue(b.getString("report.excel.invActivity"));
                    invHeader.getCell(0).setCellStyle(boldStyle);

                    String[] invHeaders = {
                            b.getString("report.excel.invTime"),
                            b.getString("report.excel.invProduct"),
                            b.getString("report.excel.invType"),
                            b.getString("report.excel.invQtyChange"),
                            b.getString("report.excel.invAction"),
                            b.getString("report.excel.invDetails")
                    };
                    Row invHeadRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < invHeaders.length; i++) {
                        Cell c = invHeadRow.createCell(i);
                        c.setCellValue(invHeaders[i]);
                        c.setCellStyle(summaryStyle);
                    }

                    List<InventoryTransaction> dayTx = txDAO.getTransactionsBetween(startOfDay, endOfDay);
                    boolean hasActivity = false;

                    for (InventoryTransaction tx : dayTx) {
                        hasActivity = true;
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(tx.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));

                        // Show Name (ID)
                        String prodDisplay = (tx.getProductName() != null ? tx.getProductName()
                                : b.getString("report.excel.unknown")) + " ("
                                + tx.getProductId() + ")";
                        row.createCell(1).setCellValue(prodDisplay);

                        row.createCell(2).setCellValue(b.getString("transaction.type." + tx.getTransactionType()));
                        row.createCell(3).setCellValue(tx.getQuantityChange());
                        row.createCell(4).setCellValue(tx.getTransactionType().name());

                        // Calculate amount for display in details
                        String details = tx.getReferenceId();
                        if (tx.getTransactionType().name().equals("SALE")) {
                            try {
                                int saleId = Integer.parseInt(tx.getReferenceId().replace("SALE-", ""));
                                List<SaleItem> sItems = saleDAO.getItemsBySaleId(saleId);
                                for (SaleItem si : sItems) {
                                    if (si.getProductId() == tx.getProductId()) {
                                        double lineTotal = si.getPriceAtSale() * Math.abs(tx.getQuantityChange());
                                        details += " - " + b.getString("report.excel.amtHeader") + ": "
                                                + String.format("%,.2f", lineTotal) + " "
                                                + b.getString("common.mmk");
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore parsing errors
                            }
                        } else if (tx.getTransactionType().name().equals("PURCHASE") && tx.getBatchId() != null) {
                            try (PreparedStatement pstmt = conn
                                    .prepareStatement("SELECT cost_per_unit FROM batches WHERE id = ?")) {
                                pstmt.setInt(1, tx.getBatchId());
                                try (ResultSet rs = pstmt.executeQuery()) {
                                    if (rs.next()) {
                                        double cost = rs.getDouble(1);
                                        details += " - " + b.getString("report.excel.costHeader") + ": "
                                                + String.format("%,.2f", cost * Math.abs(tx.getQuantityChange()))
                                                + " " + b.getString("common.mmk");
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }

                        row.createCell(5).setCellValue(details);
                    }

                    for (AuditLog log : logs) {
                        if (log.getAction().contains("Price") || log.getAction().contains("Cost")) {
                            hasActivity = true;
                            Row row = sheet.createRow(rowNum++);
                            row.createCell(0)
                                    .setCellValue(log.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                            row.createCell(1).setCellValue(log.getEntityId());
                            row.createCell(2).setCellValue(b.getString("transaction.type.VALUE_CHANGE"));
                            row.createCell(3).setCellValue(0);
                            row.createCell(4).setCellValue(log.getAction());
                            row.createCell(5).setCellValue(log.getDetails());
                        }
                    }

                    if (!hasActivity) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(b.getString("report.excel.noInvLogs"));
                    }

                    for (int i = 0; i < invHeaders.length; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    // --- Expense Details ---
                    rowNum += 2;
                    Row expTitleRow = sheet.createRow(rowNum++);
                    Cell expTitleCell = expTitleRow.createCell(0);
                    expTitleCell.setCellValue(b.getString("report.excel.expenseDetails"));
                    expTitleCell.setCellStyle(boldStyle);

                    String[] expHeaders = {
                            b.getString("report.excel.category"),
                            b.getString("report.excel.amount"),
                            b.getString("report.excel.description"),
                            b.getString("report.excel.time")
                    };

                    Row expHeadRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < expHeaders.length; i++) {
                        Cell c = expHeadRow.createCell(i);
                        c.setCellValue(expHeaders[i]);
                        c.setCellStyle(summaryStyle);
                    }

                    // List<Expense> dayExpenses = expenseDAO.getExpensesBetween(startOfDay,
                    // endOfDay);
                    // double totalDayExpenses = 0; // Already calculated in summary
                    List<Expense> dayExpenses = expenseDAO.getExpensesBetween(startOfDay, endOfDay);
                    if (dayExpenses.isEmpty()) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(b.getString("report.excel.noExpenses"));
                    } else {
                        for (Expense exp : dayExpenses) {
                            Row row = sheet.createRow(rowNum++);
                            row.createCell(0).setCellValue(exp.getCategory());
                            Cell amtCell = row.createCell(1);
                            amtCell.setCellValue(exp.getAmount());
                            amtCell.setCellStyle(currencyStyle);
                            row.createCell(2).setCellValue(exp.getDescription());
                            row.createCell(3)
                                    .setCellValue(exp.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
                            totalDayExpenses += exp.getAmount();
                        }
                    }

                    for (int i = 0; i < expHeaders.length; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    // --- Charts (including Waterfall) ---
                    int chartRow = rowNum + 2;
                    if (sheet instanceof XSSFSheet) {
                        XSSFSheet xSheet = (XSSFSheet) sheet;
                        addChartsToSheet(xSheet, todaySales, saleDAO, chartRow);
                        addWaterfallChartToSheet(xSheet, totalSales, totalCogs, totalDayExpenses, chartRow + 17);
                        addInventoryChartsToSheet(xSheet, auditLogDAO.getLogsByDateRange(startOfDay, endOfDay),
                                chartRow + 34);
                    }
                }

                // --- Period Analysis Sheets ---
                LocalDateTime endOfRange = end.atTime(LocalTime.MAX);
                LocalDateTime yearStart = end.withDayOfYear(1).atStartOfDay();

                // Daily Analysis (All days in current year)
                addAnalysisSheet(workbook, b.getString("report.excel.dailyAnalysis"),
                        yearStart, endOfRange, saleDAO, paymentDAO, expenseDAO, auditLogDAO,
                        headerStyle, summaryStyle, currencyStyle, boldStyle, AggregationLevel.DAILY);

                // Weekly Analysis (All weeks in current year)
                addAnalysisSheet(workbook, b.getString("report.excel.weeklyAnalysis"),
                        yearStart, endOfRange, saleDAO, paymentDAO, expenseDAO, auditLogDAO,
                        headerStyle, summaryStyle, currencyStyle, boldStyle, AggregationLevel.WEEKLY);

                // Monthly Analysis (All months in current year)
                addAnalysisSheet(workbook, b.getString("report.excel.monthlyAnalysis"),
                        yearStart, endOfRange, saleDAO, paymentDAO, expenseDAO, auditLogDAO,
                        headerStyle, summaryStyle, currencyStyle, boldStyle, AggregationLevel.MONTHLY);

                // Yearly Analysis (At least 5 years)
                addAnalysisSheet(workbook, b.getString("report.excel.yearlyAnalysis"),
                        end.minusYears(4).withDayOfYear(1).atStartOfDay(), endOfRange, saleDAO, paymentDAO, expenseDAO,
                        auditLogDAO,
                        headerStyle, summaryStyle, currencyStyle, boldStyle, AggregationLevel.YEARLY);

                if (latestDateSheetIndex >= 0) {
                    workbook.setActiveSheet(latestDateSheetIndex);
                    workbook.setSelectedTab(latestDateSheetIndex);
                } else if (workbook.getNumberOfSheets() > 0) {
                    workbook.setActiveSheet(workbook.getNumberOfSheets() - 1);
                    workbook.setSelectedTab(workbook.getNumberOfSheets() - 1);
                }

                try (FileOutputStream fileOut = new FileOutputStream(reportFile)) {
                    workbook.write(fileOut);
                }
            }

            logger.info("Range Report Excel generated: {}", reportFile.getAbsolutePath());
            return reportFile;
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void addChartsToSheet(XSSFSheet sheet, List<Sale> sales, SaleDAO saleDAO, int startRow) {
        if (sales.isEmpty())
            return;
        LocalDateTime start = sales.stream().map(Sale::getSaleDate).min(LocalDateTime::compareTo).get();
        LocalDateTime end = sales.stream().map(Sale::getSaleDate).max(LocalDateTime::compareTo).get();
        addChartsToSheet(sheet, sales, saleDAO, startRow, AggregationLevel.AUTO, start, end);
    }

    private void addChartsToSheet(XSSFSheet sheet, List<Sale> sales, SaleDAO saleDAO, int startRow,
            AggregationLevel aggregation, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (sales.isEmpty())
            return;

        // Sales Timeline (Line Chart)
        java.util.ResourceBundle b = App.getBundle();
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null)
            drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchorLine = drawing.createAnchor(0, 0, 0, 0, 0, startRow, 5, startRow + 15);
        XSSFChart lineChart = drawing.createChart(anchorLine);
        lineChart.setTitleText(b.getString("report.excel.chartSalesTitle"));
        lineChart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = lineChart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle(b.getString("report.excel.chartSalesX"));
        XDDFValueAxis leftAxis = lineChart.createValueAxis(AxisPosition.LEFT);
        String currency = App.getBundle().getString("common.mmk");
        leftAxis.setTitle(String.format(b.getString("report.excel.chartSalesY"), currency));

        XDDFDataSource<String> categoriesDS;
        XDDFNumericalDataSource<Double> valuesDS;

        if (aggregation == AggregationLevel.YEARLY) {
            java.util.Map<Integer, Double> yearlyRev = new java.util.TreeMap<>();
            // Fill at least 5 years
            int startYear = rangeStart.getYear();
            int endYear = rangeEnd.getYear();
            for (int y = startYear; y <= endYear; y++)
                yearlyRev.put(y, 0.0);

            for (Sale s : sales) {
                int y = s.getSaleDate().getYear();
                yearlyRev.put(y, yearlyRev.getOrDefault(y, 0.0) + s.getTotalAmount());
            }
            categoriesDS = XDDFDataSourcesFactory
                    .fromArray(yearlyRev.keySet().stream().map(String::valueOf).toArray(String[]::new));
            valuesDS = XDDFDataSourcesFactory.fromArray(yearlyRev.values().toArray(new Double[0]));
        } else if (aggregation == AggregationLevel.MONTHLY) {
            java.util.Map<String, Double> monthlyRev = new java.util.TreeMap<>();
            // Fill months
            LocalDateTime curr = rangeStart.withDayOfMonth(1);
            while (!curr.isAfter(rangeEnd)) {
                monthlyRev.put(curr.format(DateTimeFormatter.ofPattern("yyyy-MM")), 0.0);
                curr = curr.plusMonths(1);
            }
            for (Sale s : sales) {
                String m = s.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthlyRev.put(m, monthlyRev.getOrDefault(m, 0.0) + s.getTotalAmount());
            }
            categoriesDS = XDDFDataSourcesFactory.fromArray(monthlyRev.keySet().toArray(new String[0]));
            valuesDS = XDDFDataSourcesFactory.fromArray(monthlyRev.values().toArray(new Double[0]));
        } else if (aggregation == AggregationLevel.WEEKLY) {
            java.util.Map<String, Double> weeklyRev = new java.util.TreeMap<>();
            java.time.temporal.TemporalField woy = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                    .weekOfWeekBasedYear();
            // Fill weeks
            LocalDateTime curr = rangeStart;
            while (!curr.isAfter(rangeEnd)) {
                String w = curr.getYear() + "-W" + curr.get(woy);
                weeklyRev.put(w, 0.0);
                curr = curr.plusWeeks(1);
            }
            for (Sale s : sales) {
                String w = s.getSaleDate().getYear() + "-W" + s.getSaleDate().get(woy);
                weeklyRev.put(w, weeklyRev.getOrDefault(w, 0.0) + s.getTotalAmount());
            }
            categoriesDS = XDDFDataSourcesFactory.fromArray(weeklyRev.keySet().toArray(new String[0]));
            valuesDS = XDDFDataSourcesFactory.fromArray(weeklyRev.values().toArray(new Double[0]));
        } else {
            // Grouping sales by date if they span multiple days (AUTO/DAILY)
            long distinctDays = sales.stream().map(s -> s.getSaleDate().toLocalDate()).distinct().count();

            if (distinctDays > 1 || aggregation == AggregationLevel.DAILY) {
                java.util.Map<LocalDate, Double> dailyRevenue = new java.util.TreeMap<>();
                // Fill days
                LocalDate curr = rangeStart.toLocalDate();
                while (!curr.isAfter(rangeEnd.toLocalDate())) {
                    dailyRevenue.put(curr, 0.0);
                    curr = curr.plusDays(1);
                }
                for (Sale s : sales) {
                    LocalDate d = s.getSaleDate().toLocalDate();
                    dailyRevenue.put(d, dailyRevenue.getOrDefault(d, 0.0) + s.getTotalAmount());
                }
                categoriesDS = XDDFDataSourcesFactory.fromArray(dailyRevenue.keySet().stream()
                        .map(LocalDate::toString).toArray(String[]::new));
                valuesDS = XDDFDataSourcesFactory.fromArray(dailyRevenue.values().toArray(new Double[0]));
            } else {
                categoriesDS = XDDFDataSourcesFactory.fromArray(sales.stream()
                        .map(s -> s.getSaleDate().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .toArray(String[]::new));
                valuesDS = XDDFDataSourcesFactory.fromArray(sales.stream()
                        .map(Sale::getTotalAmount).toArray(Double[]::new));
            }
        }

        XDDFLineChartData lineData = (XDDFLineChartData) lineChart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFLineChartData.Series series1 = (XDDFLineChartData.Series) lineData.addSeries(categoriesDS, valuesDS);
        series1.setTitle(b.getString("report.excel.chartSales"), null);
        lineChart.plot(lineData);

        // Bar Chart (Product popularity)
        java.util.Map<String, Integer> productCounts = new java.util.HashMap<>();
        for (Sale s : sales) {
            try {
                List<SaleItem> items = saleDAO.getItemsBySaleId(s.getId());
                for (SaleItem item : items) {
                    productCounts.put(item.getProductName(),
                            productCounts.getOrDefault(item.getProductName(), 0) + item.getQuantity());
                }
            } catch (SQLException ignored) {
            }
        }

        if (!productCounts.isEmpty()) {
            XSSFClientAnchor anchorBar = drawing.createAnchor(0, 0, 0, 0, 6, startRow, 12, startRow + 15);
            XSSFChart barChart = drawing.createChart(anchorBar);
            barChart.setTitleText(b.getString("report.excel.chartProductTitle"));

            XDDFCategoryAxis catAxis = barChart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis valAxis = barChart.createValueAxis(AxisPosition.LEFT);
            valAxis.setTitle(b.getString("report.excel.chartProductY"));

            XDDFDataSource<String> products = XDDFDataSourcesFactory
                    .fromArray(productCounts.keySet().toArray(new String[0]));
            XDDFNumericalDataSource<Integer> counts = XDDFDataSourcesFactory
                    .fromArray(productCounts.values().toArray(new Integer[0]));

            XDDFBarChartData barData = (XDDFBarChartData) barChart.createData(ChartTypes.BAR, catAxis, valAxis);
            barData.setBarDirection(BarDirection.COL);
            XDDFBarChartData.Series barSeries = (XDDFBarChartData.Series) barData.addSeries(products, counts);
            barSeries.setTitle(b.getString("report.excel.chartProductY"), null);
            barChart.plot(barData);
        }

        // Pie Chart (Category Distribution)
        java.util.Map<String, Double> categoryTotals = new java.util.HashMap<>();
        for (Sale s : sales) {
            try {
                List<SaleItem> items = saleDAO.getItemsBySaleId(s.getId());
                for (SaleItem item : items) {
                    String cat = item.getCategoryName() != null ? item.getCategoryName() : "Unknown";
                    categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0)
                            + (item.getPriceAtSale() * item.getQuantity()));
                }
            } catch (SQLException ignored) {
            }
        }

        if (!categoryTotals.isEmpty()) {
            XSSFClientAnchor anchorPie = drawing.createAnchor(0, 0, 0, 0, 13, startRow, 19, startRow + 15);
            XSSFChart pieChart = drawing.createChart(anchorPie);
            pieChart.setTitleText(b.getString("report.excel.chartCategoryTitle"));

            XDDFDataSource<String> categories = XDDFDataSourcesFactory
                    .fromArray(categoryTotals.keySet().toArray(new String[0]));
            XDDFNumericalDataSource<Double> revenue = XDDFDataSourcesFactory
                    .fromArray(categoryTotals.values().toArray(new Double[0]));

            XDDFPieChartData pieData = (XDDFPieChartData) pieChart.createData(ChartTypes.PIE, null, null);
            pieData.addSeries(categories, revenue);
            pieChart.plot(pieData);
        }
    }

    private void addInventoryChartsToSheet(XSSFSheet sheet, List<AuditLog> logs, int startRow) {
        if (logs == null || logs.isEmpty())
            return;

        java.util.Map<String, Integer> adjustmentCounts = new java.util.HashMap<>();
        for (AuditLog log : logs) {
            if ("Product".equalsIgnoreCase(log.getEntityName()) || log.getAction().contains("Inventory")) {
                String key = log.getEntityId() != null ? log.getEntityId() : "Unknown";
                adjustmentCounts.put(key, adjustmentCounts.getOrDefault(key, 0) + 1);
            }
        }

        if (adjustmentCounts.isEmpty())
            return;

        java.util.ResourceBundle b = App.getBundle();
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null)
            drawing = sheet.createDrawingPatriarch();

        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow, 6, startRow + 15);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(b.getString("report.excel.chartInvTitle"));
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle(b.getString("report.excel.chartInvX"));
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(b.getString("report.excel.chartInvY"));

        XDDFDataSource<String> products = XDDFDataSourcesFactory
                .fromArray(adjustmentCounts.keySet().toArray(new String[0]));
        XDDFNumericalDataSource<Integer> counts = XDDFDataSourcesFactory
                .fromArray(adjustmentCounts.values().toArray(new Integer[0]));

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(products, counts);
        series.setTitle(b.getString("report.excel.invAdjustments"), null);
        chart.plot(data);
    }

    private void addWaterfallChartToSheet(XSSFSheet sheet, double revenue, double cogs, double expenses, int startRow) {
        if (revenue == 0 && cogs == 0 && expenses == 0)
            return;

        java.util.ResourceBundle b = App.getBundle();
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null)
            drawing = sheet.createDrawingPatriarch();

        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow, 8, startRow + 15);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(b.getString("report.excel.chartWaterfallTitle"));
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);

        String[] categories = {
                b.getString("report.excel.totalRevenue"),
                b.getString("report.excel.coGs"),
                b.getString("report.excel.grossProfit"),
                b.getString("finance.totalExpenses"),
                b.getString("report.excel.netProfit")
        };

        double grossProfit = revenue - cogs;
        double netProfit = grossProfit - expenses;

        Double[] values = { revenue, -cogs, grossProfit, -expenses, netProfit };

        XDDFDataSource<String> categoryDS = XDDFDataSourcesFactory.fromArray(categories);
        XDDFNumericalDataSource<Double> valuesDS = XDDFDataSourcesFactory.fromArray(values);

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(categoryDS, valuesDS);
        series.setTitle(b.getString("report.excel.chartWaterfallTitle"), null);

        chart.plot(data);
    }

    private void addAnalysisSheet(Workbook workbook, String sheetName, LocalDateTime start, LocalDateTime end,
            SaleDAO saleDAO, SalePaymentDAO paymentDAO, ExpenseDAO expenseDAO, AuditLogDAO auditLogDAO,
            CellStyle headerStyle, CellStyle summaryStyle, CellStyle currencyStyle, CellStyle boldStyle,
            AggregationLevel aggregation) {

        Sheet sheet = workbook.createSheet(sheetName);
        int rowNum = 0;
        java.util.ResourceBundle b = App.getBundle();

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(sheetName);
        titleCell.setCellStyle(headerStyle);
        rowNum++;

        try {
            // Aggregated Summary
            double totalSales = saleDAO.getTotalSalesBetween(start, end);
            double totalProfit = saleDAO.getTotalProfitBetween(start, end);
            double totalCogs = totalSales - totalProfit;
            double totalExpenses = expenseDAO.getTotalExpensesBetween(start, end);
            double netProfit = totalProfit - totalExpenses;
            List<Sale> sales = saleDAO.getSalesBetween(start, end);

            Row sumHead = sheet.createRow(rowNum++);
            Cell shc = sumHead.createCell(0);
            shc.setCellValue(b.getString("report.excel.summaryTitle"));
            shc.setCellStyle(boldStyle);

            String[][] sumData = {
                    { b.getString("report.excel.totalRevenue"), String.valueOf(totalSales) },
                    { b.getString("report.excel.coGs"), String.valueOf(totalCogs) },
                    { b.getString("report.excel.grossProfit"), String.valueOf(totalProfit) },
                    { b.getString("finance.totalExpenses"), String.valueOf(totalExpenses) },
                    { b.getString("report.excel.netProfit"), String.valueOf(netProfit) }
            };

            for (String[] data : sumData) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(data[0]);
                Cell v = r.createCell(1);
                v.setCellValue(Double.parseDouble(data[1]));
                v.setCellStyle(currencyStyle);
            }
            rowNum++;

            // --- ABC Analysis ---
            Row abcHead = sheet.createRow(rowNum++);
            Cell abcHc = abcHead.createCell(0);
            abcHc.setCellValue(b.getString("report.excel.abcTitle"));
            abcHc.setCellStyle(boldStyle);

            String[] abcHeaders = {
                    b.getString("report.excel.abcProduct"),
                    b.getString("report.excel.abcRevenue"),
                    b.getString("report.excel.abcCumulative"),
                    b.getString("report.excel.abcCategory")
            };
            Row abcHRow = sheet.createRow(rowNum++);
            for (int i = 0; i < abcHeaders.length; i++) {
                Cell c = abcHRow.createCell(i);
                c.setCellValue(abcHeaders[i]);
                c.setCellStyle(summaryStyle);
            }

            // Group revenue by product
            java.util.Map<String, Double> productRevenue = new java.util.HashMap<>();
            for (Sale s : sales) {
                List<SaleItem> items = saleDAO.getItemsBySaleId(s.getId());
                for (SaleItem si : items) {
                    productRevenue.put(si.getProductName(),
                            productRevenue.getOrDefault(si.getProductName(), 0.0)
                                    + (si.getPriceAtSale() * si.getQuantity()));
                }
            }

            // Sort by revenue desc
            List<java.util.Map.Entry<String, Double>> sorted = new java.util.ArrayList<>(productRevenue.entrySet());
            sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            double runningTotal = 0;
            int count = 0;
            for (java.util.Map.Entry<String, Double> entry : sorted) {
                if (count++ > 100)
                    break; // Limit to top 100 for high level
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(entry.getKey());
                Cell revCell = r.createCell(1);
                revCell.setCellValue(entry.getValue());
                revCell.setCellStyle(currencyStyle);

                runningTotal += entry.getValue();
                double percent = (totalSales > 0) ? (runningTotal / totalSales) : 0;
                r.createCell(2).setCellValue(String.format("%.2f%%", percent * 100));

                String category = b.getString("report.excel.abc.c");
                if (percent <= 0.7)
                    category = b.getString("report.excel.abc.a");
                else if (percent <= 0.9)
                    category = b.getString("report.excel.abc.b");
                r.createCell(3).setCellValue(category);
            }
            rowNum++;

            // --- Charts ---
            if (sheet instanceof XSSFSheet) {
                XSSFSheet xSheet = (XSSFSheet) sheet;
                int chartRow = rowNum + 1;
                addChartsToSheet(xSheet, sales, saleDAO, chartRow, aggregation, start, end);
                addWaterfallChartToSheet(xSheet, totalSales, totalCogs, totalExpenses, chartRow + 17);

                // --- Expense Distribution (Bar Chart) ---
                List<Expense> allExpenses = expenseDAO.getExpensesBetween(start, end);
                java.util.Map<String, Double> expenseCats = new java.util.TreeMap<>();
                for (Expense e : allExpenses) {
                    expenseCats.put(e.getCategory(), expenseCats.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
                }

                if (!expenseCats.isEmpty()) {
                    XSSFDrawing drawing = xSheet.getDrawingPatriarch();
                    if (drawing == null)
                        drawing = xSheet.createDrawingPatriarch();
                    XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, chartRow + 34, 8, chartRow + 49);
                    XSSFChart chart = drawing.createChart(anchor);
                    chart.setTitleText(b.getString("finance.expensesTitle"));
                    XDDFCategoryAxis bot = chart.createCategoryAxis(AxisPosition.BOTTOM);
                    XDDFValueAxis lft = chart.createValueAxis(AxisPosition.LEFT);
                    XDDFDataSource<String> cD = XDDFDataSourcesFactory
                            .fromArray(expenseCats.keySet().toArray(new String[0]));
                    XDDFNumericalDataSource<Double> vD = XDDFDataSourcesFactory
                            .fromArray(expenseCats.values().toArray(new Double[0]));
                    XDDFBarChartData bd = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bot, lft);
                    bd.setBarDirection(BarDirection.COL);
                    bd.addSeries(cD, vD).setTitle(b.getString("finance.totalExpenses"), null);
                    chart.plot(bd);
                }
            }

            // --- Professional Sheets for Monthly Analysis ---
            if (aggregation == AggregationLevel.MONTHLY) {
                addProfitAndLossSheet(workbook, start, end, saleDAO, expenseDAO, headerStyle, currencyStyle, boldStyle);
                addBalanceSheet(workbook, start, end, saleDAO, expenseDAO, headerStyle, currencyStyle, boldStyle);
                addCashFlowSheet(workbook, start, end, saleDAO, paymentDAO, expenseDAO, headerStyle, currencyStyle,
                        boldStyle);
                addInventoryValueSheet(workbook, headerStyle, summaryStyle, currencyStyle, boldStyle);
            }

            for (int i = 0; i < 5; i++)
                sheet.autoSizeColumn(i);

        } catch (SQLException e) {
            logger.error("Error generating analysis sheet", e);
        }
    }

    private void addProfitAndLossSheet(Workbook workbook, LocalDateTime start, LocalDateTime end,
            SaleDAO saleDAO, ExpenseDAO expenseDAO,
            CellStyle headerStyle, CellStyle currencyStyle, CellStyle boldStyle) throws SQLException {
        java.util.ResourceBundle b = App.getBundle();
        Sheet sheet = workbook.createSheet(b.getString("report.excel.plSheet"));
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(b.getString("report.excel.plSheet"));
        titleCell.setCellStyle(headerStyle);
        rowNum++;

        LocalDateTime currentMonthStart = start.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        while (currentMonthStart.isBefore(end)) {
            LocalDateTime currentMonthEnd = currentMonthStart.plusMonths(1).minusNanos(1);
            if (currentMonthEnd.isAfter(end))
                currentMonthEnd = end;

            // Month Header
            Row monthRow = sheet.createRow(rowNum++);
            Cell monthCell = monthRow.createCell(0);
            monthCell.setCellValue(currentMonthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")));
            monthCell.setCellStyle(boldStyle);

            double revenue = saleDAO.getTotalSalesBetween(currentMonthStart, currentMonthEnd);
            double profit = saleDAO.getTotalProfitBetween(currentMonthStart, currentMonthEnd);
            double cogs = revenue - profit;

            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.pl.revenue"), revenue, currencyStyle, null);
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.pl.cogs"), -cogs, currencyStyle, null);
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.pl.grossProfit"), profit, currencyStyle,
                    boldStyle);
            rowNum++;

            // Expenses
            List<Expense> expenses = expenseDAO.getExpensesBetween(currentMonthStart, currentMonthEnd);
            java.util.Map<String, Double> expByCat = new java.util.TreeMap<>();
            for (Expense e : expenses) {
                expByCat.put(e.getCategory(), expByCat.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
            }

            double totalExpenses = 0;
            for (java.util.Map.Entry<String, Double> entry : expByCat.entrySet()) {
                writeSummaryRow(sheet, rowNum++, entry.getKey(), -entry.getValue(), currencyStyle, null);
                totalExpenses += entry.getValue();
            }

            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.pl.netProfit"), profit - totalExpenses,
                    currencyStyle, boldStyle);
            rowNum++;
            Row line = sheet.createRow(rowNum++);
            for (int i = 0; i < 2; i++)
                line.createCell(i).setCellValue("---");
            rowNum++;

            currentMonthStart = currentMonthStart.plusMonths(1);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void addBalanceSheet(Workbook workbook, LocalDateTime start, LocalDateTime end,
            SaleDAO saleDAO, ExpenseDAO expenseDAO,
            CellStyle headerStyle, CellStyle currencyStyle, CellStyle boldStyle) throws SQLException {
        java.util.ResourceBundle b = App.getBundle();
        Sheet sheet = workbook.createSheet(b.getString("report.excel.bsSheet"));
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(b.getString("report.excel.bsSheet"));
        titleCell.setCellStyle(headerStyle);
        rowNum++;

        LocalDateTime currentMonthStart = start.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        while (currentMonthStart.isBefore(end)) {
            LocalDateTime currentMonthEnd = currentMonthStart.plusMonths(1).minusNanos(1);
            if (currentMonthEnd.isAfter(end))
                currentMonthEnd = end;

            // Month Header
            Row monthRow = sheet.createRow(rowNum++);
            Cell monthCell = monthRow.createCell(0);
            monthCell.setCellValue(currentMonthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")));
            monthCell.setCellStyle(boldStyle);

            // --- ASSETS ---
            Row assetHead = sheet.createRow(rowNum++);
            assetHead.createCell(0).setCellValue(b.getString("report.excel.bs.assets"));
            assetHead.getCell(0).setCellStyle(boldStyle);

            double totalRev = saleDAO.getTotalSalesBetween(currentMonthStart, currentMonthEnd);
            double totalExp = expenseDAO.getTotalExpensesBetween(currentMonthStart, currentMonthEnd);
            double cash = totalRev - totalExp; // Simplification for current period cash flow

            double invValue = 0;
            // For historical balance sheets, we use current inventory value as a proxy if
            // historical depth is missing
            try (com.pos.system.dao.ProductDAO pDAO = new com.pos.system.dao.ProductDAO(
                    DatabaseManager.getInstance().getConnection())) {
                for (com.pos.system.models.Product p : pDAO.getAllProducts()) {
                    invValue += p.getStock() * p.getCostPrice();
                }
            }

            writeSummaryRow(sheet, rowNum++, "  " + b.getString("report.excel.bs.cash"), cash, currencyStyle, null);
            writeSummaryRow(sheet, rowNum++, "  " + b.getString("report.excel.bs.inventoryValue"), invValue,
                    currencyStyle,
                    null);
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.bs.assets"), cash + invValue, currencyStyle,
                    boldStyle);
            rowNum++;

            // --- LIABILITIES & EQUITY ---
            Row leHead = sheet.createRow(rowNum++);
            leHead.createCell(0).setCellValue(b.getString("report.excel.bs.totalLiabilitiesEquity"));
            leHead.getCell(0).setCellStyle(boldStyle);

            double liabilities = 0;
            double netIncome = totalRev - totalExp;
            double equitySource = invValue;

            writeSummaryRow(sheet, rowNum++, "  " + b.getString("report.excel.bs.liabilities"), liabilities,
                    currencyStyle,
                    null);
            writeSummaryRow(sheet, rowNum++, "  " + b.getString("report.excel.bs.retainedEarnings"), netIncome,
                    currencyStyle, null);
            writeSummaryRow(sheet, rowNum++, "  " + b.getString("report.excel.bs.equity"), equitySource,
                    currencyStyle, null);
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.bs.totalLiabilitiesEquity"),
                    liabilities + netIncome + equitySource, currencyStyle, boldStyle);

            rowNum++;
            Row lineRow = sheet.createRow(rowNum++);
            for (int i = 0; i < 2; i++)
                lineRow.createCell(i).setCellValue("---");
            rowNum++;
            currentMonthStart = currentMonthStart.plusMonths(1);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void addCashFlowSheet(Workbook workbook, LocalDateTime start, LocalDateTime end,
            SaleDAO saleDAO, SalePaymentDAO paymentDAO, ExpenseDAO expenseDAO,
            CellStyle headerStyle, CellStyle currencyStyle, CellStyle boldStyle) throws SQLException {
        java.util.ResourceBundle b = App.getBundle();
        Sheet sheet = workbook.createSheet(b.getString("report.excel.cfSheet"));
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(b.getString("report.excel.cfSheet"));
        titleCell.setCellStyle(headerStyle);
        rowNum++;

        LocalDateTime currentMonthStart = start.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        while (currentMonthStart.isBefore(end)) {
            LocalDateTime currentMonthEnd = currentMonthStart.plusMonths(1).minusNanos(1);
            if (currentMonthEnd.isAfter(end))
                currentMonthEnd = end;

            // Month Header
            Row monthRow = sheet.createRow(rowNum++);
            Cell monthCell = monthRow.createCell(0);
            monthCell.setCellValue(currentMonthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")));
            monthCell.setCellStyle(boldStyle);

            // Inflow
            double totalInflow = 0;
            List<Sale> sales = saleDAO.getSalesBetween(currentMonthStart, currentMonthEnd);
            java.util.Map<String, Double> inflowByMethod = new java.util.TreeMap<>();
            for (Sale s : sales) {
                List<SalePayment> payments = paymentDAO.findBySaleId(s.getId());
                for (SalePayment p : payments) {
                    inflowByMethod.put(p.getPaymentMethod(),
                            inflowByMethod.getOrDefault(p.getPaymentMethod(), 0.0) + p.getAmount());
                }
            }
            for (java.util.Map.Entry<String, Double> entry : inflowByMethod.entrySet()) {
                writeSummaryRow(sheet, rowNum++, entry.getKey(), entry.getValue(), currencyStyle, null);
                totalInflow += entry.getValue();
            }
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.cf.inflow"), totalInflow, currencyStyle,
                    boldStyle);
            rowNum++;

            // Outflow
            double totalExpenses = expenseDAO.getTotalExpensesBetween(currentMonthStart, currentMonthEnd);
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.pl.expenses"), -totalExpenses, currencyStyle,
                    null);
            writeSummaryRow(sheet, rowNum++, b.getString("report.excel.cf.outflow"), -totalExpenses, currencyStyle,
                    boldStyle);

            rowNum++;
            Row lineRow = sheet.createRow(rowNum++);
            for (int i = 0; i < 2; i++)
                lineRow.createCell(i).setCellValue("---");
            rowNum++;
            currentMonthStart = currentMonthStart.plusMonths(1);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void addInventoryValueSheet(Workbook workbook, CellStyle headerStyle, CellStyle summaryStyle,
            CellStyle currencyStyle, CellStyle boldStyle) throws SQLException {
        java.util.ResourceBundle b = App.getBundle();
        Sheet sheet = workbook.createSheet(b.getString("report.excel.ivSheet"));
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(b.getString("report.excel.ivSheet"));
        titleCell.setCellStyle(headerStyle);
        rowNum++;

        String[] headers = {
                b.getString("report.excel.iv.product"),
                b.getString("report.excel.iv.stock"),
                b.getString("report.excel.iv.cost"),
                b.getString("report.excel.iv.totalValue")
        };

        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(summaryStyle);
        }

        double grandTotal = 0;
        try (com.pos.system.dao.ProductDAO pDAO = new com.pos.system.dao.ProductDAO(
                DatabaseManager.getInstance().getConnection())) {
            List<com.pos.system.models.Product> products = pDAO.getAllProducts();
            for (com.pos.system.models.Product p : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getName());
                row.createCell(1).setCellValue(p.getStock());
                Cell c2 = row.createCell(2);
                c2.setCellValue(p.getCostPrice());
                c2.setCellStyle(currencyStyle);
                Cell c3 = row.createCell(3);
                double total = p.getStock() * p.getCostPrice();
                c3.setCellValue(total);
                c3.setCellStyle(currencyStyle);
                grandTotal += total;
            }
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue(b.getString("report.excel.ivSheet"));
        totalRow.getCell(0).setCellStyle(boldStyle);
        Cell totalVal = totalRow.createCell(3);
        totalVal.setCellValue(grandTotal);
        totalVal.setCellStyle(currencyStyle);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeSummaryRow(Sheet sheet, int rowNum, String label, double value, CellStyle currencyStyle,
            CellStyle fontStyle) {
        Row row = sheet.createRow(rowNum);
        Cell l = row.createCell(0);
        l.setCellValue(label);
        if (fontStyle != null)
            l.setCellStyle(fontStyle);
        Cell v = row.createCell(1);
        v.setCellValue(value);
        v.setCellStyle(currencyStyle);
    }

    private static String fmt(double value) {
        return String.format("%,.2f", value);
    }
}
