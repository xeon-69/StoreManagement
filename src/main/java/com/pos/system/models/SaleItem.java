package com.pos.system.models;

public class SaleItem {
    private int id;
    private int saleId;
    private int productId;
    private String productName;
    private int quantity;
    private double priceAtSale;
    private double costAtSale;
    private double discountAmount;
    private double taxAmount;
    private String categoryName;

    public SaleItem(int id, int saleId, int productId, String productName, int quantity, double priceAtSale,
            double costAtSale) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
        this.costAtSale = costAtSale;
        this.discountAmount = 0.0;
        this.taxAmount = 0.0;
    }

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

    public void setPriceAtSale(double priceAtSale) {
        this.priceAtSale = priceAtSale;
    }

    public double getCostAtSale() {
        return costAtSale;
    }

    public void setCostAtSale(double costAtSale) {
        this.costAtSale = costAtSale;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getTotal() {
        return (quantity * priceAtSale) - discountAmount + taxAmount;
    }
}
