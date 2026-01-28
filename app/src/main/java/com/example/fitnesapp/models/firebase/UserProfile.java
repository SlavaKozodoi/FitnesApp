package com.example.fitnesapp.models.firebase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
    public int getAge() {
        if (birthDate == null || birthDate.isEmpty()) {
            return 25; // Возраст по умолчанию, если дата не указана
        }

        // ВАЖНО: Убедитесь, что формат здесь совпадает с тем, как вы сохраняете дату
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);

        try {
            Date date = sdf.parse(birthDate);
            if (date == null) return 25;

            Calendar dob = Calendar.getInstance();
            dob.setTime(date);

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            // Если день рождения в этом году еще не наступил — вычитаем 1 год
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age;

        } catch (Exception e) {
            e.printStackTrace();
            return 25; // Если формат даты неверный, возвращаем дефолт
        }
    }
}