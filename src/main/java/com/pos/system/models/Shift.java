package com.pos.system.models;

import java.time.LocalDateTime;

public class Shift {
    private int id;
    private int userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double openingCash;
    private Double expectedClosingCash;
    private Double actualClosingCash;
    private String status;

    public Shift() {
    }

    public Shift(int id, int userId, LocalDateTime startTime, LocalDateTime endTime, double openingCash,
            Double expectedClosingCash, Double actualClosingCash, String status) {
        this.id = id;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.openingCash = openingCash;
        this.expectedClosingCash = expectedClosingCash;
        this.actualClosingCash = actualClosingCash;
        this.status = status;
    }

    // Getters and Setters
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public double getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(double openingCash) {
        this.openingCash = openingCash;
    }

    public Double getExpectedClosingCash() {
        return expectedClosingCash;
    }

    public void setExpectedClosingCash(Double expectedClosingCash) {
        this.expectedClosingCash = expectedClosingCash;
    }

    public Double getActualClosingCash() {
        return actualClosingCash;
    }

    public void setActualClosingCash(Double actualClosingCash) {
        this.actualClosingCash = actualClosingCash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
