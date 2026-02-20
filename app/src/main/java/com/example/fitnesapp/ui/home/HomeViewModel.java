package com.example.fitnesapp.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends ViewModel {

    // Данные профиля и за сегодня
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData> dailyData = new MutableLiveData<>();

    // Списки для графиков
    private final MutableLiveData<List<HealthLogItem>> pulseHistory = new MutableLiveData<>();
    private final MutableLiveData<List<WeightHistoryItem>> weightHistory = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeDays = new MutableLiveData<>();

    // НОВОЕ: Отдельная LiveData для ТЕКУЩЕГО пульса (последнего за СЕГОДНЯ)
    private final MutableLiveData<Integer> todayPulseValue = new MutableLiveData<>();

    private final MutableLiveData<Boolean> requireLogin = new MutableLiveData<>();
    public LiveData<Boolean> getRequireLogin() { return requireLogin; }

    private DatabaseReference userRef;

    // НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ АНИМАЦИЙ
    private boolean isDashboardAnimated = false;
    private boolean isPulseChartAnimated = false;
    private boolean isWeightChartAnimated = false;

    // Геттеры и сеттеры
    public boolean isDashboardAnimated() { return isDashboardAnimated; }
    public void setDashboardAnimated(boolean animated) { isDashboardAnimated = animated; }

    public boolean isPulseChartAnimated() { return isPulseChartAnimated; }
    public void setPulseChartAnimated(boolean animated) { isPulseChartAnimated = animated; }

    public boolean isWeightChartAnimated() { return isWeightChartAnimated; }
    public void setWeightChartAnimated(boolean animated) { isWeightChartAnimated = animated; }

    public HomeViewModel() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = null;

        if (user != null) {
            uid = user.getUid();
        } else {
            requireLogin.setValue(true);
        }

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            loadProfile();
            loadTodayStats();
            loadChartsHistory();
            loadActiveDays();
        }
    }

    // --- Геттеры для Fragment ---
    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData> getDailyData() { return dailyData; }
    public LiveData<List<HealthLogItem>> getPulseHistory() { return pulseHistory; }
    public LiveData<List<WeightHistoryItem>> getWeightHistory() { return weightHistory; }
    public LiveData<List<String>> getActiveDays() { return activeDays; }

    // Геттер для нового значения пульса
    public LiveData<Integer> getTodayPulseValue() { return todayPulseValue; }


    // --- Загрузка Профиля ---
    private void loadProfile() {
        userRef.child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userProfile.setValue(snapshot.getValue(UserProfile.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Загрузка статистики за СЕГОДНЯ ---
    private void loadTodayStats() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        userRef.child("daily_data").child(today).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    dailyData.setValue(snapshot.getValue(DailyData.class));
                } else {
                    dailyData.setValue(new DailyData());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Загрузка Истории для Графиков ---
    private void loadChartsHistory() {
        // Пульс (последние 50 записей)
        userRef.child("health_logs").child("pulse").limitToLast(50)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<HealthLogItem> list = new ArrayList<>();

                        // 1. Вычисляем начало сегодняшнего дня (00:00:00)
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

                                // 2. Проверяем: если запись сделана сегодня, запоминаем её
                                // Так как цикл идет по порядку, lastTodayItem в конце будет содержать самую свежую запись
                                if (item.time >= startOfDay) {
                                    lastTodayItem = item;
                                }
                            }
                        }

                        // Обновляем список для графика
                        pulseHistory.setValue(list);

                        // 3. Обновляем значение "Пульс сейчас"
                        if (lastTodayItem != null) {
                            todayPulseValue.setValue((int) lastTodayItem.val);
                        } else {
                            // Если сегодня записей нет, отправляем null (или 0)
                            todayPulseValue.setValue(null);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Вес (оставляем без изменений, так как вес не обязательно обновляется каждый день)
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
                List<String> validDays = new ArrayList<>();
                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    DailyData dayData = daySnap.getValue(DailyData.class);
                    if (dayData != null) {
                        boolean hasWorkouts = (dayData.workouts != null && !dayData.workouts.isEmpty());
                        if (hasWorkouts) {
                            validDays.add(daySnap.getKey());
                        }
                    }
                }
                activeDays.setValue(validDays);
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
}