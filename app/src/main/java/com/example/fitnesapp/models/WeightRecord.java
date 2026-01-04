package com.example.fitnesapp.models;

// Простая модель для хранения одной записи веса
public class WeightRecord {
    public long timestamp; // Время в миллисекундах
    public float weight;   // Вес

    public WeightRecord(long timestamp, float weight) {
        this.timestamp = timestamp;
        this.weight = weight;
    }
}
