package com.example.fitnesapp.ui.settings;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsViewModel extends ViewModel {

    private DatabaseReference userRef;

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<UserGoals> userGoals = new MutableLiveData<>();

    // LiveData для отображения текущего веса в поле ввода
    private final MutableLiveData<Double> currentWeight = new MutableLiveData<>();

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
    public LiveData<Double> getCurrentWeight() { return currentWeight; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }

    // --- ЗАГРУЗКА ДАННЫХ ---
    private void loadData() {
        if (userRef == null) return;

        // 1. Грузим профиль (Имя, рост, дата рождения...)
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

        // 2. Грузим цели
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

        // 3. Грузим ПОСЛЕДНИЙ ВЕС из истории (чтобы показать в поле ввода)
        // limitToLast(1) возьмет самую свежую запись
        userRef.child("health_logs").child("weight_history").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                // Ищем поле "val" (значение веса)
                                Double w = child.child("val").getValue(Double.class);
                                if (w != null) {
                                    currentWeight.setValue(w);
                                }
                            }
                        } else {
                            currentWeight.setValue(0.0); // Веса еще нет
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

        // 1. Обновляем профиль (БЕЗ ВЕСА)
        // Используем Map, чтобы обновить только нужные поля и не затереть ничего лишнего
        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("firstName", name);
        profileUpdates.put("secondName", surname);
        profileUpdates.put("birthDate", birthDate);
        profileUpdates.put("height", height);
        profileUpdates.put("gender", gender);
        profileUpdates.put("notificationsEnabled", notif);
        // weight отсюда убрали!

        userRef.child("profile").updateChildren(profileUpdates);

        // 2. Сохраняем вес в ИСТОРИЮ (Новая логика)
        // Проверяем, изменился ли вес или это первая запись, чтобы не спамить в историю
        Double oldWeight = currentWeight.getValue();
        if (oldWeight == null || Math.abs(oldWeight - weight) > 0.1) {
            saveWeightToHistory(weight);
        }

        // 3. Рассчитываем новые калории и шаги (используем новый вес)
        int calculatedCalories = calculateCalories(weight, height, gender, activityLevel, mainGoal);
        int calculatedSteps = calculateSteps(activityLevel);

        // 4. Сохраняем цели
        UserGoals goals = new UserGoals(mainGoal, targetWeight, activityLevel, calculatedCalories, calculatedSteps);
        userRef.child("goals").setValue(goals)
                .addOnSuccessListener(aVoid -> saveSuccess.setValue(true))
                .addOnFailureListener(e -> saveSuccess.setValue(false));
    }

    // === НОВЫЙ МЕТОД: Запись веса в историю ===
    private void saveWeightToHistory(double weight) {
        // Создаем уникальный ключ (push ID), который сортируется по времени автоматически
        DatabaseReference newWeightRef = userRef.child("health_logs").child("weight_history").push();

        long timestamp = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Map<String, Object> weightMap = new HashMap<>();
        weightMap.put("val", weight);          // Значение (double)
        weightMap.put("timestamp", timestamp); // Время (long) для сортировки
        weightMap.put("date", dateStr);        // Дата (String) для группировки

        newWeightRef.setValue(weightMap);

        // Сразу обновляем локальное значение, чтобы UI отреагировал
        currentWeight.setValue(weight);
    }

    // --- ЛОГИКА РАСЧЕТА КАЛОРИЙ ---
    private int calculateCalories(double weight, double height, String gender, String activity, String goal) {
        // Формула Миффлина-Сан Жеора
        double bmr;
        int age = 25; // Заглушка, если нет возраста. В идеале считать из birthDate

        if ("Male".equals(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
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