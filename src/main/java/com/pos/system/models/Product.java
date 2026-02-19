package com.pos.system.models;

public class Product {
    private int id;
    private String barcode;
    private String name;
    private String category;
    private double costPrice;
    private double sellingPrice;
    private int stock;
    private String imagePath;

    public Product(int id, String barcode, String name, String category, double costPrice, double sellingPrice,
            int stock, String imagePath) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.stock = stock;
        this.imagePath = imagePath;
    }

    // Constructor overload for backward compatibility (optional but helpful)
    public Product(int id, String barcode, String name, String category, double costPrice, double sellingPrice,
            int stock) {
        this(id, barcode, name, category, costPrice, sellingPrice, stock, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
