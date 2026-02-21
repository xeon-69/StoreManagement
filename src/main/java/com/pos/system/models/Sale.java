package com.pos.system.models;

import java.time.LocalDateTime;

public class Sale {
    private int id;
    private int userId;
    private double totalAmount;
    private double totalProfit;
    private LocalDateTime saleDate;
    private String transactionDetails;

    public Sale(int id, int userId, double totalAmount, double totalProfit, LocalDateTime saleDate) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.totalProfit = totalProfit;
        this.saleDate = saleDate;
    }

    public Sale(int id, int userId, double totalAmount, double totalProfit, LocalDateTime saleDate,
            String transactionDetails) {
        this(id, userId, totalAmount, totalProfit, saleDate);
        this.transactionDetails = transactionDetails;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(double totalProfit) {
        this.totalProfit = totalProfit;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public String getTransactionDetails() {
        return transactionDetails;
    }

    public void setTransactionDetails(String transactionDetails) {
        this.transactionDetails = transactionDetails;
    }
}
