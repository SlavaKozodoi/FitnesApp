package com.example.fitnesapp.models.firebase;

public class UserGoals {
    public String mainGoal;       // "Lose Weight"
    public double targetWeight;   // 75.0
    public String activityLevel;  // "Sedentary"
    public int dailyCalories;     // Рассчитанное значение
    public int dailySteps;        // Рассчитанное значение

    public UserGoals() {}

    public UserGoals(String mainGoal, double targetWeight, String activityLevel, int dailyCalories, int dailySteps) {
        this.mainGoal = mainGoal;
        this.targetWeight = targetWeight;
        this.activityLevel = activityLevel;
        this.dailyCalories = dailyCalories;
        this.dailySteps = dailySteps;
    }
}