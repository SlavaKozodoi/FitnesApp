package com.example.fitnesapp.models.firebase;

public class WeightHistoryItem {
    public double val;   // Вес (78.4)
    public String date;  // "2026-01-22"
    public long timestamp; // Время в формате Long


    public WeightHistoryItem() {}

    public WeightHistoryItem(double val, String date) {
        this.val = val;
        this.date = date;
    }
}
