package com.pos.system.models;

public class Product {
    private int id;
    private String barcode;
    private String name;
    private int categoryId;
    private String category;
    private double costPrice;
    private double sellingPrice;
    private int stock;
    private byte[] imageData;

    public Product(int id, String barcode, String name, int categoryId, String category, double costPrice,
            double sellingPrice,
            int stock, byte[] imageData) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.categoryId = categoryId;
        this.category = category;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.stock = stock;
        this.imageData = imageData;
    }

    public Product(int id, String barcode, String name, String category, double costPrice, double sellingPrice,
            int stock, byte[] imageData) {
        this(id, barcode, name, 0, category, costPrice, sellingPrice, stock, imageData);
    }

    // Constructor overload for backward compatibility (optional but helpful)
    public Product(int id, String barcode, String name, String category, double costPrice, double sellingPrice,
            int stock) {
        this(id, barcode, name, 0, category, costPrice, sellingPrice, stock, null);
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
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

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
}
