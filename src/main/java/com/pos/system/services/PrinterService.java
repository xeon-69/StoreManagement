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

    private PrintService getFallbackPrinter() {
        return PrinterOutputStream.getDefaultPrintService();
    }

    public void printReceipt(Sale sale, List<SaleItem> items, double amountTendered, double changeDue) {
        PrintService printService = getFallbackPrinter();
        if (printService == null) {
            System.err.println("No default printer found. Printing to console instead.");
            printToConsole(sale, items, amountTendered, changeDue);
            return;
        }

        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                EscPos escpos = new EscPos(printerOutputStream)) {

            Style title = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Center);

            escpos.writeLF(title, "MY STORE");
            escpos.writeLF("Receipt No: " + sale.getId());
            escpos.writeLF("Date: " + (sale.getSaleDate() != null ? sale.getSaleDate().toString() : ""));
            escpos.writeLF("--------------------------------");

            for (SaleItem item : items) {
                String line = String.format("%-15s x%d  $%.2f",
                        item.getProductName() != null ? item.getProductName() : "Item " + item.getProductId(),
                        item.getQuantity(),
                        item.getTotal());
                escpos.writeLF(line);
                if (item.getDiscountAmount() > 0) {
                    escpos.writeLF(String.format("  Discount: -$%.2f", item.getDiscountAmount()));
                }
            }

            escpos.writeLF("--------------------------------");
            escpos.writeLF(String.format("Subtotal:  $%.2f", sale.getSubtotal()));
            escpos.writeLF(String.format("Tax:       $%.2f", sale.getTaxAmount()));
            if (sale.getDiscountAmount() > 0) {
                escpos.writeLF(String.format("Discount: -$%.2f", sale.getDiscountAmount()));
            }
            escpos.writeLF(new Style().setBold(true), String.format("TOTAL:     $%.2f", sale.getTotalAmount()));
            escpos.writeLF("--------------------------------");
            escpos.writeLF(String.format("Tendered:  $%.2f", amountTendered));
            escpos.writeLF(String.format("Change:    $%.2f", changeDue));

            escpos.writeLF(new Style().setJustification(EscPosConst.Justification.Center), "Thank You!");

            // Kick the cash drawer
            escpos.feed(5);
            escpos.cut(EscPos.CutMode.FULL);
            // Cash drawer pulse
            byte[] drawerCommand = new byte[] { 27, 112, 0, 50, (byte) 250 };
            printerOutputStream.write(drawerCommand);

        } catch (IOException e) {
            System.err.println("Failed to print to ESC/POS. Printing to console instead. " + e.getMessage());
            printToConsole(sale, items, amountTendered, changeDue);
        }
    }

    public void openCashDrawer() {
        PrintService printService = getFallbackPrinter();
        if (printService != null) {
            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService)) {
                // ESC p m t1 t2 (Pulse command)
                byte[] drawerCommand = new byte[] { 27, 112, 0, 50, (byte) 250 };
                printerOutputStream.write(drawerCommand);
            } catch (IOException e) {
                System.err.println("Failed to kick drawer: " + e.getMessage());
            }
        } else {
            System.out.println("--- VIRTUAL CASH DRAWER KICKED ---");
        }
    }

    private void printToConsole(Sale sale, List<SaleItem> items, double amountTendered, double changeDue) {
        System.out.println("\n========= RECEIPT =========");
        System.out.println("Receipt No: " + sale.getId());
        System.out.println("Date: " + (sale.getSaleDate() != null ? sale.getSaleDate().toString() : ""));
        System.out.println("---------------------------");
        for (SaleItem item : items) {
            System.out.printf("%-15s x%d  $%.2f\n",
                    item.getProductName() != null ? item.getProductName() : "Item " + item.getProductId(),
                    item.getQuantity(),
                    item.getTotal());
            if (item.getDiscountAmount() > 0) {
                System.out.printf("  Discount: -$%.2f\n", item.getDiscountAmount());
            }
        }
        System.out.println("---------------------------");
        System.out.printf("Subtotal:  $%.2f\n", sale.getSubtotal());
        System.out.printf("Tax:       $%.2f\n", sale.getTaxAmount());
        if (sale.getDiscountAmount() > 0) {
            System.out.printf("Discount: -$%.2f\n", sale.getDiscountAmount());
        }
        System.out.printf("TOTAL:     $%.2f\n", sale.getTotalAmount());
        System.out.println("---------------------------");
        System.out.printf("Tendered:  $%.2f\n", amountTendered);
        System.out.printf("Change:    $%.2f\n", changeDue);
        System.out.println("===========================\n");
    }
}
