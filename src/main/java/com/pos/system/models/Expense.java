package com.pos.system.models;

import java.time.LocalDateTime;

public class Expense {
    private int id;
    private String category;
    private double amount;
    private String description;
    private LocalDateTime date;

    public Expense(int id, String category, double amount, String description, LocalDateTime date) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
