package com.example.fitnesapp.models.firebase;

public class UserProfile {
    public String firstName;
    public String secondName;
    public String gender;       // "Male" / "Female"
    public String birthDate;
    public int height;
    public double weight;       // double для веса (например, 78.5)
    public int totalXP;
    public int maxXp;
    public boolean notificationsEnabled;

    // 1. Пустой конструктор (ОБЯЗАТЕЛЕН для Firebase)
    public UserProfile() {}

    // 2. Полный конструктор (Нужен для создания объекта в коде)
    // Обратите внимание на порядок полей, он должен совпадать с тем, как вы их передаете
    public UserProfile(String firstName, String secondName, String gender, String birthDate, int height, double weight, int totalXP, int maxXp, boolean notificationsEnabled) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.height = height;
        this.weight = weight;
        this.totalXP = totalXP;
        this.maxXp = maxXp;
        this.notificationsEnabled = notificationsEnabled;
    }
}