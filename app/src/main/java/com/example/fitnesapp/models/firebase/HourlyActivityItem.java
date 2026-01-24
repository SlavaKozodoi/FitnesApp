package com.example.fitnesapp.models.firebase;

public class HourlyActivityItem {
    public long time;   // timestamp часа
    public int steps;   // шаги за этот час
    public int calories; // калории за этот час

    public HourlyActivityItem() {}

    public HourlyActivityItem(long time, int steps, int calories) {
        this.time = time;
        this.steps = steps;
        this.calories = calories;
    }
}