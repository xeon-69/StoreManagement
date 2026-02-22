package com.pos.system.services;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;

import javax.print.PrintService;
import java.io.IOException;
import java.util.List;

public class PrinterService {

    // Standard 58mm thermal paper ≈ 32 chars, 80mm ≈ 48 chars
    private static final int RECEIPT_LINE_WIDTH = 32;

    private PrintService getFallbackPrinter() {
        return PrinterOutputStream.getDefaultPrintService();
    }

    /**
     * Prints a receipt. Returns true if printing succeeded, false on failure.
     */
    public boolean printReceipt(Sale sale, List<SaleItem> items, double amountTendered, double changeDue) {
        PrintService printService = getFallbackPrinter();
        if (printService == null) {
            System.err.println("No default printer found. Printing to console instead.");
            printToConsole(sale, items, amountTendered, changeDue);
            return false;
        }

        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                EscPos escpos = new EscPos(printerOutputStream)) {

            // Set UTF-8 charset for Unicode support (Chinese/Burmese)
            // ESC t n — Select character code table
            // Many thermal printers support UTF-8 via code page selection
            try {
                // Initialize printer with ESC @ command via raw output stream
                printerOutputStream.write(new byte[] { 0x1B, 0x40 });
                // Set UTF-8 encoding if supported by printer firmware
                escpos.setCharsetName("UTF-8");
            } catch (Exception e) {
                // Some printers don't support UTF-8 — continue with default
                System.err.println("UTF-8 charset not supported by printer, using default.");
            }

            Style title = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Center);

            escpos.writeLF(title, "MY STORE");
            escpos.writeLF("Receipt No: " + sale.getId());
            escpos.writeLF("Date: " + (sale.getSaleDate() != null
                    ? sale.getSaleDate().toString().replace("T", " ")
                    : ""));
            escpos.writeLF(dashLine());

            for (SaleItem item : items) {
                String productName = item.getProductName() != null
                        ? item.getProductName()
                        : "Item " + item.getProductId();
                String qtyPrice = String.format("x%d  %,.2f", item.getQuantity(), item.getTotal());

                // Wrap product name if it's too long
                int nameWidth = RECEIPT_LINE_WIDTH - qtyPrice.length() - 2; // 2 for spacing
                if (nameWidth < 5)
                    nameWidth = 5;

                if (productName.length() > nameWidth) {
                    // Print name on first line, qty+price on second
                    escpos.writeLF(productName);
                    escpos.writeLF(padLeft(qtyPrice, RECEIPT_LINE_WIDTH));
                } else {
                    escpos.writeLF(padRight(productName, nameWidth) + "  " + qtyPrice);
                }

                if (item.getDiscountAmount() > 0) {
                    escpos.writeLF(String.format("  Discount: -%,.2f", item.getDiscountAmount()));
                }
            }

            escpos.writeLF(dashLine());
            escpos.writeLF(formatLine("Subtotal:", String.format("%,.2f", sale.getSubtotal())));
            escpos.writeLF(formatLine("Tax:", String.format("%,.2f", sale.getTaxAmount())));
            if (sale.getDiscountAmount() > 0) {
                escpos.writeLF(formatLine("Discount:", String.format("-%,.2f", sale.getDiscountAmount())));
            }
            escpos.writeLF(new Style().setBold(true),
                    formatLine("TOTAL:", String.format("%,.2f", sale.getTotalAmount())));
            escpos.writeLF(dashLine());
            escpos.writeLF(formatLine("Tendered:", String.format("%,.2f", amountTendered)));
            escpos.writeLF(formatLine("Change:", String.format("%,.2f", changeDue)));

            escpos.writeLF(new Style().setJustification(EscPosConst.Justification.Center), "Thank You!");

            // Feed, cut, and kick cash drawer
            escpos.feed(5);
            escpos.cut(EscPos.CutMode.FULL);
            byte[] drawerCommand = new byte[] { 27, 112, 0, 50, (byte) 250 };
            printerOutputStream.write(drawerCommand);

            return true;

        } catch (IOException e) {
            System.err.println("Failed to print to ESC/POS. Printing to console instead. " + e.getMessage());
            printToConsole(sale, items, amountTendered, changeDue);
            return false;
        }
    }

    public void openCashDrawer() {
        PrintService printService = getFallbackPrinter();
        if (printService != null) {
            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService)) {
                byte[] drawerCommand = new byte[] { 27, 112, 0, 50, (byte) 250 };
                printerOutputStream.write(drawerCommand);
            } catch (IOException e) {
                System.err.println("Failed to kick drawer: " + e.getMessage());
            }
        } else {
            System.out.println("--- VIRTUAL CASH DRAWER KICKED ---");
        }
    }

    // --- Helper methods for receipt formatting ---

    /** Create a dashed separator line */
    private String dashLine() {
        return "-".repeat(RECEIPT_LINE_WIDTH);
    }

    /** Format a label:value line, right-aligning value */
    private String formatLine(String label, String value) {
        int spaces = RECEIPT_LINE_WIDTH - label.length() - value.length();
        if (spaces < 1)
            spaces = 1;
        return label + " ".repeat(spaces) + value;
    }

    /** Pad string to the right with spaces */
    private String padRight(String s, int width) {
        if (s.length() >= width)
            return s;
        return s + " ".repeat(width - s.length());
    }

    /** Pad string to the left with spaces */
    private String padLeft(String s, int width) {
        if (s.length() >= width)
            return s;
        return " ".repeat(width - s.length()) + s;
    }

    private void printToConsole(Sale sale, List<SaleItem> items, double amountTendered, double changeDue) {
        System.out.println("\n========= RECEIPT =========");
        System.out.println("Receipt No: " + sale.getId());
        System.out.println("Date: " + (sale.getSaleDate() != null ? sale.getSaleDate().toString() : ""));
        System.out.println("---------------------------");
        for (SaleItem item : items) {
            System.out.printf("%-15s x%d  $%,.2f\n",
                    item.getProductName() != null ? item.getProductName() : "Item " + item.getProductId(),
                    item.getQuantity(),
                    item.getTotal());
            if (item.getDiscountAmount() > 0) {
                System.out.printf("  Discount: -$%,.2f\n", item.getDiscountAmount());
            }
        }
        System.out.println("---------------------------");
        System.out.printf("Subtotal:  $%,.2f\n", sale.getSubtotal());
        System.out.printf("Tax:       $%,.2f\n", sale.getTaxAmount());
        if (sale.getDiscountAmount() > 0) {
            System.out.printf("Discount: -$%,.2f\n", sale.getDiscountAmount());
        }
        System.out.printf("TOTAL:     $%,.2f\n", sale.getTotalAmount());
        System.out.println("---------------------------");
        System.out.printf("Tendered:  $%,.2f\n", amountTendered);
        System.out.printf("Change:    $%,.2f\n", changeDue);
        System.out.println("===========================\n");
    }
}
