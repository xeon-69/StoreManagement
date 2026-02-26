package com.pos.system.utils;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Product;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFReportGenerator {

    public static void generateInventoryReport(String dest) {
        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Inventory Report"));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));

            float[] pointColumnWidths = { 100F, 200F, 100F, 100F };
            Table table = new Table(pointColumnWidths);
            table.addCell("Barcode");
            table.addCell("Name");
            table.addCell("Price");
            table.addCell("Stock");

            try (ProductDAO productDAO = new ProductDAO()) {
                List<Product> products = productDAO.getAllProductsSummary();

                for (Product p : products) {
                    table.addCell(p.getBarcode());
                    table.addCell(p.getName());
                    table.addCell(String.valueOf(p.getSellingPrice()));
                    table.addCell(String.valueOf(p.getStock()));
                }
            }

            document.add(table);
            document.close();

        } catch (FileNotFoundException | SQLException e) {
            // Ignored, handled by caller
        }
    }
}
