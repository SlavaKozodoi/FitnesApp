package com.example.fitnesapp.models.firebase;

import com.google.firebase.database.Exclude; // ОБЯЗАТЕЛЬНО добавить этот импорт!

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
    public float weight;       // В комментарии было double, но тип float. Оставил float, это нормально.
    public int totalXP;
    public int maxXp;
    public boolean notificationsEnabled;

    public int totalSteps = 0;          // Шагов за всё время
    public int totalWorkouts = 0;       // Тренировок за всё время
    public int totalCaloriesBurned = 0; // Сожжено калорий за всё время
    public int stepStreakDays = 0;      // Дней подряд выполнена цель по шагам
    public int perfectSleepDays = 0;

    public int totalWeightLogs = 0;   // Сколько раз взвешивался
    public int totalPulseLogs = 0;    // Сколько раз мерил пульс
    public int totalOxygenLogs = 0;   // Сколько раз мерил кислород
    public int closedRingsStreak = 0; // Дней подряд закрыто все 3 кольца

    // 1. Пустой конструктор (ОБЯЗАТЕЛЕН для Firebase)
    public UserProfile() {}

    // 2. Полный конструктор
    public UserProfile(String firstName, String secondName, String gender, String birthDate, int height, float weight, int totalXP, int maxXp, boolean notificationsEnabled) {
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

    @Exclude
    public int getAge() {
        if (birthDate == null || birthDate.isEmpty()) {
            return 25;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);

        try {
            Date date = sdf.parse(birthDate);
            if (date == null) return 25;

            Calendar dob = Calendar.getInstance();
            dob.setTime(date);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            int currentMonth = today.get(Calendar.MONTH);
            int birthMonth = dob.get(Calendar.MONTH);

            if (currentMonth < birthMonth || (currentMonth == birthMonth && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            return age;

        } catch (Exception e) {
            e.printStackTrace();
            return 25;
        }
    }
}