package com.example.fitnesapp.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.SleepStageItem;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.models.firebase.WeightHistoryItem;
import com.example.fitnesapp.models.firebase.WorkoutItem;
import com.example.fitnesapp.utils.MLPredictor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// ВАЖНО: Класс теперь наследует AndroidViewModel для работы ML
public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData> dailyData = new MutableLiveData<>();
    private final MutableLiveData<List<HealthLogItem>> pulseHistory = new MutableLiveData<>();
    private final MutableLiveData<List<WeightHistoryItem>> weightHistory = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeDays = new MutableLiveData<>();
    private final MutableLiveData<Integer> todayPulseValue = new MutableLiveData<>();
    private final MutableLiveData<Boolean> requireLogin = new MutableLiveData<>();

    public LiveData<Boolean> getRequireLogin() { return requireLogin; }

    private DatabaseReference userRef;

    private boolean isDashboardAnimated = false;
    private boolean isPulseChartAnimated = false;
    private boolean isWeightChartAnimated = false;

    // --- НАША НЕЙРОСЕТЬ ---
    private MLPredictor mlPredictor;
    private Query historyQuery;
    private List<DailyData> currentWeekHistory = new ArrayList<>();
    private String todayDate;

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

        // Инициализируем предсказатель
        mlPredictor = new MLPredictor(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = null;

        if (user != null) {
            uid = user.getUid();
        } else {
            requireLogin.setValue(true);
        }

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

            // Запрашиваем историю за неделю
            historyQuery = userRef.child("daily_data").orderByKey().limitToLast(8);

            loadProfile();
            loadHistoryForML();
            loadTodayStats();
            loadChartsHistory();
            loadActiveDays();
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData> getDailyData() { return dailyData; }
    public LiveData<List<HealthLogItem>> getPulseHistory() { return pulseHistory; }
    public LiveData<List<WeightHistoryItem>> getWeightHistory() { return weightHistory; }
    public LiveData<List<String>> getActiveDays() { return activeDays; }
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

    // Скачиваем историю сна для работы ИИ
    private void loadHistoryForML() {
        historyQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DailyData> history = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key != null && !key.equals(todayDate)) {
                        DailyData d = child.getValue(DailyData.class);
                        if (d != null) history.add(d);
                    }
                }
                currentWeekHistory = history;

                // Пересчитываем текущие данные, если они уже загрузились
                DailyData current = dailyData.getValue();
                if (current != null && current.sleep != null && current.sleep.durationMinutes > 0) {
                    enrichSleepWithML(current.sleep);
                    dailyData.setValue(current);
                }
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

                // Пропускаем сырые данные через ИИ
                if (data != null && data.sleep != null && data.sleep.durationMinutes > 0) {
                    enrichSleepWithML(data.sleep);
                }

                dailyData.setValue(data);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // МЕТОД: Пересчет сна нейросетью
    private void enrichSleepWithML(DailyData.Sleep sleep) {
        if (sleep == null) return;
        long totalSleep = sleep.durationMinutes;
        long deepSleep = sleep.phases != null ? sleep.phases.deep : 0;
        long remSleep = sleep.phases != null ? sleep.phases.rem : 0;

        int awakenings = 0;
        if (sleep.hypnogram != null && !sleep.hypnogram.isEmpty()) {
            int previousStage = -1;
            for (SleepStageItem item : sleep.hypnogram.values()) {
                int currentStage = item.stage;
                if (currentStage == 4 && previousStage != 4 && previousStage != -1) {
                    awakenings++;
                }
                previousStage = currentStage;
            }
            if (awakenings > 0) awakenings--;
        }

        long totalHistorySleep = 0;
        int validDays = 0;
        for (DailyData day : currentWeekHistory) {
            if (day.sleep != null && day.sleep.durationMinutes > 0) {
                totalHistorySleep += day.sleep.durationMinutes;
                validDays++;
            }
        }
        long avgHistory = validDays > 0 ? totalHistorySleep / validDays : 420;

        float mlScore = mlPredictor.predictSleepQuality(totalSleep, deepSleep, remSleep, awakenings, avgHistory);

        if (mlScore >= 0f) {
            sleep.score = Math.round(mlScore * 100);
            if (mlScore >= 0.8f) sleep.quality = "Отлично";
            else if (mlScore >= 0.5f) sleep.quality = "Норма";
            else sleep.quality = "Плохо";
        } else {
            // Если ИИ почему-то не отработал, переводим английское слово
            if ("Excellent".equalsIgnoreCase(sleep.quality)) sleep.quality = "Отлично";
            else if ("Good".equalsIgnoreCase(sleep.quality)) sleep.quality = "Норма";
            else if ("Poor".equalsIgnoreCase(sleep.quality)) sleep.quality = "Плохо";
        }
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
                List<String> validDays = new ArrayList<>();
                for (DataSnapshot daySnap : snapshot.getChildren()) {
                    DailyData dayData = daySnap.getValue(DailyData.class);
                    if (dayData != null && dayData.workouts != null && !dayData.workouts.isEmpty()) {
                        validDays.add(daySnap.getKey());
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mlPredictor != null) {
            mlPredictor.close();
        }
    }
}