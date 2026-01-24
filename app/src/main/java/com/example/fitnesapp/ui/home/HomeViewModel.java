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
import com.google.firebase.database.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends ViewModel {

    // Данные профиля и за сегодня
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData> dailyData = new MutableLiveData<>();

    // Списки для графиков и календаря
    private final MutableLiveData<List<HealthLogItem>> pulseHistory = new MutableLiveData<>();
    private final MutableLiveData<List<WeightHistoryItem>> weightHistory = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeDays = new MutableLiveData<>();

    private final MutableLiveData<Boolean> requireLogin = new MutableLiveData<>();
    public LiveData<Boolean> getRequireLogin() { return requireLogin; }

    private DatabaseReference userRef;

    public HomeViewModel() {
        // 1. Попытка получить текущего юзера
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = null;

        if (user != null) {
            uid = user.getUid();
        } else {
            requireLogin.setValue(true);


        }

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            // Запускаем загрузку всех данных
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
                    dailyData.setValue(new DailyData()); // Пустые данные для нового дня
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
                        for (DataSnapshot child : snapshot.getChildren()) {
                            HealthLogItem item = child.getValue(HealthLogItem.class);
                            if (item != null) list.add(item);
                        }
                        pulseHistory.setValue(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Вес (последние 30 записей)
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
                        // === СТРОГИЙ ФИЛЬТР ===
                        // День считается активным ТОЛЬКО если есть список тренировок
                        // и он не пустой.

                        boolean hasWorkouts = (dayData.workouts != null && !dayData.workouts.isEmpty());

                        if (hasWorkouts) {
                            validDays.add(daySnap.getKey()); // Добавляем дату "2026-01-15"
                        }
                    }
                }
                activeDays.setValue(validDays);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    // Метод для получения данных конкретного дня по клику
    public void getWorkoutForDate(String dateKey, OnWorkoutCheckListener listener) {
        if (userRef == null) return;

        userRef.child("daily_data").child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DailyData data = snapshot.getValue(DailyData.class);
                    // Проверяем, есть ли тренировки
                    if (data != null && data.workouts != null && !data.workouts.isEmpty()) {
                        // Берем первую попавшуюся тренировку (или можно сделать список выбора)
                        // values().iterator().next() берет первый элемент из Map
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

    // Интерфейс для обратного вызова (Callback)
    public interface OnWorkoutCheckListener {
        void onWorkoutFound(WorkoutItem workout);
        void onNoWorkout();
    }
}