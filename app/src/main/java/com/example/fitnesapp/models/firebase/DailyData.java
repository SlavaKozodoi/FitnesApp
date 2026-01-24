package com.example.fitnesapp.models.firebase;

import java.util.Map;

public class DailyData {
    // --- Основная активность ---
    public int steps;
    public int stepsGoal;
    public int caloriesBurned;
    public int caloriesGoal;

    // --- Вложенные объекты ---
    public Nutrition nutrition;
    public Sleep sleep;
    public VitalsSummary vitals_summary;
    public Map<String, HealthLogItem> pulse;
    public Map<String, HealthLogItem> oxygen;

    // А Вес использует ВАШУ модель (double val, String date)
    public Map<String, WeightHistoryItem> weight_history;

    public java.util.Map<String, MealItem> meals;
    public java.util.Map<String, HourlyActivityItem> hourly_activity;
    public Map<String, WorkoutItem> workouts;

    public DailyData() {}

    // === Вложенный класс: Питание ===
    public static class Nutrition {
        public int totalCalories;
        public int maxCalories;
        public int carbs;
        public int protein;
        public int fat;

        public Nutrition() {}
    }

    // === Вложенный класс: Фазы сна ===
    public static class SleepPhases {
        public int deep;
        public int surface;
        public int awake;
        public int rem;

        public SleepPhases() {}
    }

    // === Вложенный класс: Средние показатели ===
    public static class VitalsSummary {
        public int pulse_avg;
        public int spo2_avg;
        public double weight_today;

        public VitalsSummary() {}
    }


    public static class Sleep {
        public int score;
        public String quality;
        public int durationMinutes;
        public String bedTime;
        public String wakeTime;
        public int fallingAsleepMin;
        public SleepPhases phases;

        // НОВОЕ ПОЛЕ: Список точек для графика
        public java.util.Map<String, SleepStageItem> hypnogram;

        public Sleep() {}
    }
}
