package com.example.fitnesapp.models.firebase;

public class UserProfile {
    public String firstName;
    public String secondName;
    public String birthDate;
    public String gender;       // "Male" / "Female"
    public double weight;       // Используем double, так как вес дробный (78.4)
    public int height;
    public int totalXP;
    public int maxXp;
    public boolean notificationsEnabled;

    // Пустой конструктор ОБЯЗАТЕЛЕН для Firebase
    public UserProfile() {}
}
