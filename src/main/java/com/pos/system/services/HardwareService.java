package com.pos.system.services;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardwareService {
    private static final Logger logger = LoggerFactory.getLogger(HardwareService.class);

    public static class HardwareStatus {
        public final boolean scannerConnected;
        public final boolean printerConnected;
        public final boolean drawerLinked;

        public HardwareStatus(boolean scannerConnected, boolean printerConnected, boolean drawerLinked) {
            this.scannerConnected = scannerConnected;
            this.printerConnected = printerConnected;
            this.drawerLinked = drawerLinked;
        }
    }

    /**
     * Checks all hardware status.
     * Note: Cash drawer is assumed linked if printer is found (common RJ11 setup).
     */
    public CompletableFuture<HardwareStatus> checkStatus() {
        return CompletableFuture.supplyAsync(() -> {
            boolean printer = checkPrinter();
            boolean scanner = checkScanner();
            return new HardwareStatus(scanner, printer, printer);
        });
    }

    private boolean checkPrinter() {
        try {
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printer != null) {
                String name = printer.getName().toLowerCase();
                // Common thermal printer identifiers
                return name.contains("pos") || name.contains("thermal") ||
                        name.contains("58mm") || name.contains("80mm") ||
                        name.contains("xprinter") || name.contains("epson");
            }

            // If default isn't found, look for any POS printer
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) {
                String name = service.getName().toLowerCase();
                if (name.contains("pos") || name.contains("thermal")) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error checking printer status", e);
        }
        return false;
    }

    private boolean checkScanner() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return checkScannerWindows();
        }
        // Fallback or other OS (simplified)
        return true;
    }

    private boolean checkScannerWindows() {
        try {
            // Check for HID devices that are likely scanners
            // Most scanners identify as 'Keyboard' or 'HID-compliant device'
            ProcessBuilder pb = new ProcessBuilder("wmic", "path", "Win32_PnPEntity", "get", "Name, Status");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase();
                    // Scanner usually has "Scanner" in name or is an HID device specifically for
                    // input
                    if (lower.contains("barcode") || lower.contains("scanner") ||
                            lower.contains("symbol") || lower.contains("zebra") ||
                            lower.contains("honeywell") || lower.contains("datalogic")) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error checking scanner status via Windows shell", e);
        }
        return false;
    }
}
