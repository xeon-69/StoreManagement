package com.pos.system.models;

import java.time.LocalDateTime;

public class Sale {
    private int id;
    private int userId;
    private Integer shiftId;
    private double subtotal;
    private double taxAmount;
    private double discountAmount;
    private double totalAmount;
    private double totalProfit;
    private LocalDateTime saleDate;
    private String transactionDetails;
    private String paymentMethods; // Transient, for table display

    // Legacy constructor compatibility
    public Sale(int id, int userId, double totalAmount, double totalProfit, LocalDateTime saleDate) {
        this(id, userId, null, totalAmount, 0.0, 0.0, totalAmount, totalProfit, saleDate);
    }

    // Legacy with transaction details
    public Sale(int id, int userId, double totalAmount, double totalProfit, LocalDateTime saleDate,
            String transactionDetails) {
        this(id, userId, null, totalAmount, 0.0, 0.0, totalAmount, totalProfit, saleDate);
        this.transactionDetails = transactionDetails;
    }

    // Full constructor
    public Sale(int id, int userId, Integer shiftId, double subtotal, double taxAmount, double discountAmount,
            double totalAmount, double totalProfit, LocalDateTime saleDate) {
        this.id = id;
        this.userId = userId;
        this.shiftId = shiftId;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.totalProfit = totalProfit;
        this.saleDate = saleDate;
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

    public Integer getShiftId() {
        return shiftId;
    }

    public void setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
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

    public String getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(String paymentMethods) {
        this.paymentMethods = paymentMethods;
    }
}
