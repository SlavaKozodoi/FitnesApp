package com.example.fitnesapp.ui.pulse;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PulseViewModel extends ViewModel {

    // LiveData
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData.VitalsSummary> dailyVitals = new MutableLiveData<>();
    private final MutableLiveData<List<HealthLogItem>> pulseHistory = new MutableLiveData<>();
    private final MutableLiveData<PulseAnalysis> analysisData = new MutableLiveData<>();

    private DatabaseReference userRef;
    private String selectedDateKey; // Хранит выбранную дату "yyyy-MM-dd"

    // Слушатели (чтобы удалять их при смене даты)
    private ValueEventListener vitalsListener;
    private ValueEventListener historyListener;

    // Вспомогательный класс для хранения расчетов
    public static class PulseAnalysis {
        public int minPulse = 0;
        public int maxPulse = 0;
        public String activePeriod = "--:--"; // Время высокой активности
        public String restPeriod = "--:--";   // Время отдыха
    }

    public PulseViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            // По умолчанию грузим сегодня
            loadDataForDate(new Date());
        }
    }

    // --- Getters ---
    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData.VitalsSummary> getDailyVitals() { return dailyVitals; }
    public LiveData<List<HealthLogItem>> getPulseHistory() { return pulseHistory; }
    public LiveData<PulseAnalysis> getAnalysisData() { return analysisData; }


    // --- Логика переключения Даты ---
    public void loadDataForDate(Date date) {
        // 1. Форматируем дату в строку ключа Firebase
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String newDateKey = sdf.format(date);

        // Если дата не изменилась, не перезагружаем
        if (newDateKey.equals(selectedDateKey)) return;

        selectedDateKey = newDateKey;

        // 2. Удаляем старые слушатели (чтобы не было утечек и накладок)
        removeListeners();

        // 3. Загружаем новые данные
        loadDailyStats(selectedDateKey);
        loadPulseHistory(selectedDateKey);
    }

    private void removeListeners() {
        if (userRef == null) return;
        if (vitalsListener != null && selectedDateKey != null) {
            userRef.child("daily_data").child(selectedDateKey).child("vitals_summary").removeEventListener(vitalsListener);
        }
        if (historyListener != null) {
            userRef.child("health_logs").child("pulse").removeEventListener(historyListener);
        }
    }


    // --- Загрузка Профиля (один раз) ---
    private void loadProfile() {
        if (userRef == null) return;
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

    // --- Загрузка Vitals (Сводка) ---
    private void loadDailyStats(String dateKey) {
        if (userRef == null) return;

        vitalsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    dailyVitals.setValue(snapshot.getValue(DailyData.VitalsSummary.class));
                } else {
                    dailyVitals.setValue(new DailyData.VitalsSummary());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        userRef.child("daily_data").child(dateKey).child("vitals_summary").addValueEventListener(vitalsListener);
    }

    // --- Загрузка Истории + Вычисления ---
    private void loadPulseHistory(String dateKey) {
        if (userRef == null) return;

        // Важно: health_logs обычно хранятся просто списком, либо по датам.
        // Если у вас структура health_logs -> pulse -> [список всех времен],
        // нам нужно фильтровать их. Но для простоты предположим, что мы берем последние 100
        // и фильтруем по дате, ЛИБО у вас структура health_logs -> date -> pulse.
        // *В этом решении я предполагаю, что health_logs общий, и мы фильтруем в коде.*

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<HealthLogItem> list = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                for (DataSnapshot child : snapshot.getChildren()) {
                    HealthLogItem item = child.getValue(HealthLogItem.class);
                    if (item != null) {
                        // Фильтрация: берем только те, что совпадают с selectedDateKey
                        // item.timestamp должен быть long (ms)
                        String itemDate = sdf.format(new Date(item.time));
                        if (itemDate.equals(selectedDateKey)) {
                            list.add(item);
                        }
                    }
                }

                // Сортируем по времени
                Collections.sort(list, (o1, o2) -> Long.compare(o1.time, o2.time));

                pulseHistory.setValue(list);

                // ЗАПУСКАЕМ ВЫЧИСЛЕНИЯ
                calculatePulseAnalysis(list);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Берем последние записи (например, 200), чтобы найти нужный день
        userRef.child("health_logs").child("pulse").limitToLast(200).addValueEventListener(historyListener);
    }


    // === ГЛАВНАЯ ЛОГИКА ВЫЧИСЛЕНИЙ ===
    private void calculatePulseAnalysis(List<HealthLogItem> logs) {
        PulseAnalysis analysis = new PulseAnalysis();

        if (logs == null || logs.isEmpty()) {
            analysisData.setValue(analysis); // Пустые данные
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        // Для периодов
        long activeStartTime = 0;
        long activeEndTime = 0;
        long restStartTime = 0;
        long restEndTime = 0;

        // Пороги (примерные)
        int ACTIVE_THRESHOLD = 100; // Пульс выше 100 считается активностью
        int REST_THRESHOLD = 65;    // Пульс ниже 65 считается покоем

        for (HealthLogItem item : logs) {
            int val = (int) item.val;

            // Мин / Макс
            if (val < min) min = val;
            if (val > max) max = val;

            // Активность (ищем самое раннее и самое позднее время активности)
            if (val > ACTIVE_THRESHOLD) {
                if (activeStartTime == 0) activeStartTime = item.time;
                activeEndTime = item.time;
            }

            // Покой
            if (val < REST_THRESHOLD) {
                if (restStartTime == 0) restStartTime = item.time;
                restEndTime = item.time;
            }
        }

        analysis.minPulse = (min == Integer.MAX_VALUE) ? 0 : min;
        analysis.maxPulse = (max == Integer.MIN_VALUE) ? 0 : max;

        // Форматирование времени
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (activeStartTime > 0 && activeEndTime > 0) {
            analysis.activePeriod = timeFormat.format(new Date(activeStartTime)) + " - " + timeFormat.format(new Date(activeEndTime));
        } else {
            analysis.activePeriod = "None";
        }

        if (restStartTime > 0 && restEndTime > 0) {
            analysis.restPeriod = timeFormat.format(new Date(restStartTime)) + " - " + timeFormat.format(new Date(restEndTime));
        } else {
            analysis.restPeriod = "None";
        }

        analysisData.setValue(analysis);
    }
}