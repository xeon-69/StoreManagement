package com.pos.system.models;

public class SaleItem {
    private int id;
    private int saleId;
    private int productId;
    private String productName; // Helper for display
    private int quantity;
    private double priceAtSale;
    private double costAtSale;

    public SaleItem(int id, int saleId, int productId, String productName, int quantity, double priceAtSale,
            double costAtSale) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
        this.costAtSale = costAtSale;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public int getSaleId() {
        return saleId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceAtSale() {
        return priceAtSale;
    }

    public double getCostAtSale() {
        return costAtSale;
    }

    public double getTotal() {
        return quantity * priceAtSale;
    }
}
