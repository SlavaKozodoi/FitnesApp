package com.example.fitnesapp.models.firebase;

public class MealItem {
    public String title;     // "Breakfast"
    public String time;      // "08:30"
    public int calories;
    public int protein;
    public int carbs;
    public int fat;

    public MealItem() {}

    public MealItem(String title, String time, int calories, int protein, int carbs, int fat) {
        this.title = title;
        this.time = time;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }
}