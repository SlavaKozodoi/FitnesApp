package com.example.fitnesapp.models;

public class Nutrition {
    private String time;
    private String carbs;
    private String proteins;
    private String fats;
    private String calories;

    public Nutrition(String time, String carbs, String proteins, String fats, String calories) {
        this.time = time;
        this.carbs = carbs;
        this.proteins = proteins;
        this.fats = fats;
        this.calories = calories;
    }

    public String getTime() {
        return time;
    }

    public String getCarbs() {
        return carbs;
    }

    public String getProteins() {
        return proteins;
    }

    public String getFats() {
        return fats;
    }

    public String getCalories() {
        return calories;
    }
}
