package com.pos.system.models;

import java.time.LocalDateTime;

public class SalePayment {
    private int id;
    private int saleId;
    private String paymentMethod;
    private double amount;
    private LocalDateTime paymentDate;

    public SalePayment() {
    }

    public SalePayment(int id, int saleId, String paymentMethod, double amount, LocalDateTime paymentDate) {
        this.id = id;
        this.saleId = saleId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
}
