package com.example.fitnesapp.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.models.firebase.WeightHistoryItem;
import com.example.fitnesapp.models.firebase.WorkoutItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData> dailyData = new MutableLiveData<>();
    private final MutableLiveData<List<HealthLogItem>> pulseHistory = new MutableLiveData<>();
    private final MutableLiveData<List<WeightHistoryItem>> weightHistory = new MutableLiveData<>();

    // ДВА СПИСКА ДЛЯ КАЛЕНДАРЯ:
    private final MutableLiveData<List<String>> activeDays = new MutableLiveData<>(); // Дни с тренировками
    private final MutableLiveData<Map<String, Integer>> dailyProgressMap = new MutableLiveData<>(); // Кольца прогресса

    private final MutableLiveData<Integer> todayPulseValue = new MutableLiveData<>();
    private final MutableLiveData<Boolean> requireLogin = new MutableLiveData<>();

    public LiveData<Boolean> getRequireLogin() { return requireLogin; }

    private DatabaseReference userRef;

    private boolean isDashboardAnimated = false;
    private boolean isPulseChartAnimated = false;
    private boolean isWeightChartAnimated = false;

    private String todayDate;
    private String uid;

    public boolean isDashboardAnimated() { return isDashboardAnimated; }
    public void setDashboardAnimated(boolean animated) { isDashboardAnimated = animated; }
    public boolean isPulseChartAnimated() { return isPulseChartAnimated; }
    public void setPulseChartAnimated(boolean animated) { isPulseChartAnimated = animated; }
    public boolean isWeightChartAnimated() { return isWeightChartAnimated; }
    public void setWeightChartAnimated(boolean animated) { isWeightChartAnimated = animated; }

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public HomeViewModel(@NonNull Application application) {
        super(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : null;

        if (uid == null) {
            requireLogin.setValue(true);
        } else {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

            loadProfile();
            loadTodayStats();
            loadChartsHistory();
            loadActiveDays();
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData> getDailyData() { return dailyData; }
    public LiveData<List<HealthLogItem>> getPulseHistory() { return pulseHistory; }
    public LiveData<List<WeightHistoryItem>> getWeightHistory() { return weightHistory; }

    // Геттеры для календаря
    public LiveData<List<String>> getActiveDays() { return activeDays; }
    public LiveData<Map<String, Integer>> getDailyProgressMap() { return dailyProgressMap; }

    public LiveData<Integer> getTodayPulseValue() { return todayPulseValue; }

    private void loadProfile() {
        userRef.child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) userProfile.setValue(snapshot.getValue(UserProfile.class));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTodayStats() {
        userRef.child("daily_data").child(todayDate).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DailyData data = snapshot.exists() ? snapshot.getValue(DailyData.class) : new DailyData();

                if (data != null) {
                    // ПРОВЕРКА НА НОВЫЙ ДЕНЬ: Если цели нулевые, копируем из вчерашнего дня
                    if (data.stepsGoal == 0 || data.caloriesGoal == 0) {
                        fetchYesterdayGoalsAndSaveToToday();
                        return;
                    }

                    dailyData.setValue(data);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchYesterdayGoalsAndSaveToToday() {
        if (uid == null || todayDate == null) return;

        DatabaseReference dailyDataRef = userRef.child("daily_data");

        // 1. Вычисляем вчерашнюю дату
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());

        // 2. Идем во вчерашний день
        dailyDataRef.child(yesterdayDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                if (snapshot.exists()) {
                    DailyData yesterdayData = snapshot.getValue(DailyData.class);
                    if (yesterdayData != null) {
                        // Берем вчерашние цели или дефолтные, если вчера тоже было пусто
                        updates.put("stepsGoal", yesterdayData.stepsGoal > 0 ? yesterdayData.stepsGoal : 10000f);
                        updates.put("caloriesGoal", yesterdayData.caloriesGoal > 0 ? yesterdayData.caloriesGoal : 500f);

                        float oldNutritionGoal = (yesterdayData.nutrition != null && yesterdayData.nutrition.maxCalories > 0)
                                ? yesterdayData.nutrition.maxCalories : 2000f;
                        updates.put("nutrition/maxCalories", oldNutritionGoal);
                    }
                } else {
                    // Если вчерашнего дня нет в базе вообще (новый пользователь)
                    updates.put("stepsGoal", 10000f);
                    updates.put("caloriesGoal", 500f);
                    updates.put("nutrition/maxCalories", 2000f);
                }

                // 3. Сохраняем цели в СЕГОДНЯШНИЙ день
                dailyDataRef.child(todayDate).updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeViewModel", "Failed to fetch yesterday's goals", error.toException());
            }
        });
    }

    private void loadChartsHistory() {
        userRef.child("health_logs").child("pulse").limitToLast(50)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<HealthLogItem> list = new ArrayList<>();
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        long startOfDay = cal.getTimeInMillis();
                        HealthLogItem lastTodayItem = null;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            HealthLogItem item = child.getValue(HealthLogItem.class);
                            if (item != null) {
                                list.add(item);
                                if (item.time >= startOfDay) lastTodayItem = item;
                            }
                        }
                        pulseHistory.setValue(list);
                        todayPulseValue.setValue(lastTodayItem != null ? (int) lastTodayItem.val : null);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        userRef.child("health_logs").child("weight_history").limitToLast(30)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<WeightHistoryItem> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            WeightHistoryItem item = child.getValue(WeightHistoryItem.class);
                            if (item != null) list.add(item);
                        }
                        weightHistory.setValue(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadActiveDays() {
        if (userRef == null) return;
        userRef.child("daily_data").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> progressMap = new HashMap<>();
                List<String> workoutDays = new ArrayList<>();

                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    DailyData data = daySnap.getValue(DailyData.class);
                    if (data != null) {

                        // 1. Проверяем тренировки (добавляем в список для фиолетового фона)
                        if (data.workouts != null && !data.workouts.isEmpty()) {
                            workoutDays.add(daySnap.getKey());
                        }

                        // 2. Считаем прогресс (для кольца)
                        float calG = data.caloriesGoal;
                        float stepG = data.stepsGoal;
                        float nutG = (data.nutrition != null) ? data.nutrition.maxCalories : 0f;

                        float totalPercent = 0f;
                        int activeGoalsCount = 0;

                        // Считаем калории только если цель больше нуля
                        if (calG > 0) {
                            totalPercent += Math.min(data.caloriesBurned / calG, 1f);
                            activeGoalsCount++;
                        }

                        // Считаем шаги только если цель больше нуля
                        if (stepG > 0) {
                            totalPercent += Math.min(data.steps / stepG, 1f);
                            activeGoalsCount++;
                        }

                        // Считаем питание только если цель больше нуля
                        if (nutG > 0) {
                            totalPercent += Math.min(data.nutrition.totalCalories / nutG, 1f);
                            activeGoalsCount++;
                        }

                        // Вычисляем финальный процент без риска деления на ноль
                        int finalPercent = 0;
                        if (activeGoalsCount > 0) {
                            finalPercent = Math.round((totalPercent / activeGoalsCount) * 100f);
                        }

                        // Если процент 0, но активность всё же была, показываем хотя бы 5% для мотивации
                        if (finalPercent == 0 && (data.steps > 0 || data.caloriesBurned > 0 || data.workouts != null)) {
                            finalPercent = 5;
                        }

                        // Добавляем день в календарь, если есть хоть какой-то прогресс или тренировка
                        if (finalPercent > 0 || data.workouts != null) {
                            progressMap.put(daySnap.getKey(), finalPercent);
                        }
                    }
                }

                // Отправляем ОБА списка
                dailyProgressMap.setValue(progressMap);
                activeDays.setValue(workoutDays);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void getWorkoutForDate(String dateKey, OnWorkoutCheckListener listener) {
        if (userRef == null) return;
        userRef.child("daily_data").child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DailyData data = snapshot.getValue(DailyData.class);
                    if (data != null && data.workouts != null && !data.workouts.isEmpty()) {
                        WorkoutItem workout = data.workouts.values().iterator().next();
                        listener.onWorkoutFound(workout);
                    } else {
                        listener.onNoWorkout();
                    }
                } else {
                    listener.onNoWorkout();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onNoWorkout();
            }
        });
    }

    public interface OnWorkoutCheckListener {
        void onWorkoutFound(WorkoutItem workout);
        void onNoWorkout();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}