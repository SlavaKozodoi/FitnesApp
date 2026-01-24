package com.example.fitnesapp.ui.settings;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserGoals;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsViewModel extends ViewModel {

    private DatabaseReference userRef;

    // LiveData для отображения данных в UI
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<UserGoals> userGoals = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();

    public SettingsViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadData();
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<UserGoals> getUserGoals() { return userGoals; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }

    // --- ЗАГРУЗКА ДАННЫХ ---
    private void loadData() {
        if (userRef == null) return;

        // Грузим профиль
        userRef.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userProfile.setValue(snapshot.getValue(UserProfile.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Грузим цели
        userRef.child("goals").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userGoals.setValue(snapshot.getValue(UserGoals.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- СОХРАНЕНИЕ ДАННЫХ ---
    public void saveSettings(String name, String surname, String birthDate,
                             int height, double weight, String gender, boolean notif,
                             String mainGoal, double targetWeight, String activityLevel) {

        if (userRef == null) return;

        // 1. Обновляем профиль
        UserProfile profile = new UserProfile(name, surname, gender, birthDate, height, weight, 0, 0, notif);
        // Сохраняем поля профиля (лучше обновлять конкретные поля, чтобы не затереть XP, но для простоты перезапишем объект, если XP хранятся там же)
        // ВАЖНО: Если XP хранятся в profile, их нужно сохранить.
        // В идеале лучше делать userRef.child("profile").updateChildren(map);
        // Но сейчас просто обновим основные поля:
        userRef.child("profile").child("firstName").setValue(name);
        userRef.child("profile").child("secondName").setValue(surname);
        userRef.child("profile").child("birthDate").setValue(birthDate);
        userRef.child("profile").child("height").setValue(height);
        userRef.child("profile").child("weight").setValue(weight);
        userRef.child("profile").child("gender").setValue(gender);
        userRef.child("profile").child("notificationsEnabled").setValue(notif);

        // 2. Рассчитываем новые калории и шаги (Умная аналитика!)
        int calculatedCalories = calculateCalories(weight, height, gender, activityLevel, mainGoal);
        int calculatedSteps = calculateSteps(activityLevel);

        // 3. Сохраняем цели
        UserGoals goals = new UserGoals(mainGoal, targetWeight, activityLevel, calculatedCalories, calculatedSteps);
        userRef.child("goals").setValue(goals)
                .addOnSuccessListener(aVoid -> {
                    // Также обновляем daily_data на сегодня, чтобы пользователь сразу увидел новые цели
                    updateTodayGoals(calculatedCalories, calculatedSteps);
                    saveSuccess.setValue(true);
                })
                .addOnFailureListener(e -> saveSuccess.setValue(false));
    }

    private void updateTodayGoals(int cals, int steps) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        userRef.child("daily_data").child(today).child("caloriesGoal").setValue(cals);
        userRef.child("daily_data").child(today).child("stepsGoal").setValue(steps);
    }

    // --- ЛОГИКА РАСЧЕТА ---
    private int calculateCalories(double weight, double height, String gender, String activity, String goal) {
        // Формула Миффлина-Сан Жеора
        double bmr;
        if ("Male".equals(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * 25 + 5; // возраст заглушка 25
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * 25 - 161;
        }

        double multiplier = 1.2; // Sedentary
        if (activity.contains("Light")) multiplier = 1.375;
        if (activity.contains("Moderate")) multiplier = 1.55;
        if (activity.contains("Active")) multiplier = 1.725;

        double tdee = bmr * multiplier;

        if (goal.contains("Lose")) return (int) (tdee - 400);
        if (goal.contains("Gain")) return (int) (tdee + 300);
        return (int) tdee;
    }

    private int calculateSteps(String activity) {
        if (activity.contains("Sedentary")) return 6000;
        if (activity.contains("Light")) return 8000;
        if (activity.contains("Moderate")) return 10000;
        return 12000;
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
    }
}