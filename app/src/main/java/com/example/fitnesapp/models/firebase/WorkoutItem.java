package com.example.fitnesapp.models.firebase;

public class WorkoutItem {
    public String id;
    public String type;       // "Running", "Gym", "Yoga"
    public int durationMin;   // 45
    public int calories;      // 350
    public long timestamp;    // Время начала

    public WorkoutItem() {}

    public WorkoutItem(String type, int durationMin, int calories, long timestamp) {
        this.type = type;
        this.durationMin = durationMin;
        this.calories = calories;
        this.timestamp = timestamp;
    }
}