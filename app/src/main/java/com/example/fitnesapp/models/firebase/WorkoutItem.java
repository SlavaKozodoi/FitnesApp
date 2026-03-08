package com.example.fitnesapp.models.firebase;

public class WorkoutItem {
    public String id;
    public String type;       // "Running", "Gym", "Yoga"
    public long durationSeconds;   // 45
    public int calories;      // 350
    public long timestamp;
    public double distance;// Время начала
    public java.util.Map<String, Double> speed_data;

    public WorkoutItem() {}

    public WorkoutItem(String type, long durationSec, int calories, long timestamp) {
        this.type = type;
        this.durationSeconds = durationSec;
        this.calories = calories;
        this.timestamp = timestamp;
    }
}