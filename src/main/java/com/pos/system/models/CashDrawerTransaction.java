package com.pos.system.models;

import java.time.LocalDateTime;

public class CashDrawerTransaction {
    private int id;
    private int shiftId;
    private int userId;
    private double amount;
    private String transactionType;
    private String description;
    private LocalDateTime transactionDate;

    public CashDrawerTransaction() {
    }

    public CashDrawerTransaction(int id, int shiftId, int userId, double amount, String transactionType,
            String description, LocalDateTime transactionDate) {
        this.id = id;
        this.shiftId = shiftId;
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
        this.transactionDate = transactionDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getShiftId() {
        return shiftId;
    }

    public void setShiftId(int shiftId) {
        this.shiftId = shiftId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
