package com.pos.system.models;

import java.time.LocalDateTime;

public class Batch {
    private int id;
    private int productId;
    private String batchNumber;
    private LocalDateTime expiryDate;
    private double costPrice;
    private int remainingQuantity;
    private LocalDateTime createdAt;

    public Batch() {
    }

    public Batch(int id, int productId, String batchNumber, LocalDateTime expiryDate, double costPrice,
            int remainingQuantity, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.costPrice = costPrice;
        this.remainingQuantity = remainingQuantity;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
