package com.pos.system.models;

import java.time.LocalDateTime;

public class InventoryTransaction {
    private int id;
    private int productId;
    private Integer batchId; // Can be null for raw adjustments
    private int quantityChange;
    private TransactionType transactionType;
    private String referenceId;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private String productName;
    private double amount;

    public InventoryTransaction() {
    }

    public InventoryTransaction(int id, int productId, Integer batchId, int quantityChange,
            TransactionType transactionType, String referenceId, LocalDateTime createdAt, Integer createdBy) {
        this.id = id;
        this.productId = productId;
        this.batchId = batchId;
        this.quantityChange = quantityChange;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

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

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }
}
