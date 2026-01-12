package com.example.fitnesapp.models.firebase;

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

    // === Вложенный класс: Сон ===
    public static class Sleep {
        public int score;               // 68
        public String quality;          // "Very well"
        public int durationMinutes;     // 602
        public String bedTime;          // "23:40"
        public String wakeTime;         // "09:02"
        public int fallingAsleepMin;
        public SleepPhases phases;      // Фазы сна

        public Sleep() {}
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
}
