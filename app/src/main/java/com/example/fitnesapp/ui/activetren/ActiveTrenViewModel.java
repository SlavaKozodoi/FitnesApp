package com.example.fitnesapp.ui.activetren;

import android.annotation.SuppressLint;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.WorkoutSessionUI;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.WorkoutItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ActiveTrenViewModel extends AndroidViewModel {

    private DatabaseReference userRef;
    private final MutableLiveData<List<WorkoutSessionUI>> workoutSessions = new MutableLiveData<>();

    public ActiveTrenViewModel(@NonNull Application application) {
        super(application);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
    }

    public LiveData<List<WorkoutSessionUI>> getWorkoutSessions() { return workoutSessions; }

    public void loadWorkoutsForDate(String date) {
        if (userRef == null) return;

        userRef.child("daily_data").child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DailyData dailyData = snapshot.getValue(DailyData.class);
                if (dailyData != null && dailyData.workouts != null && !dailyData.workouts.isEmpty()) {
                    List<WorkoutItem> workouts = new ArrayList<>(dailyData.workouts.values());
                    workouts.sort((w1, w2) -> Long.compare(w1.timestamp, w2.timestamp));

                    loadLogsAndCombine(workouts);
                } else {
                    workoutSessions.setValue(new ArrayList<>());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLogsAndCombine(List<WorkoutItem> workouts) {
        userRef.child("health_logs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<WorkoutSessionUI> resultList = new ArrayList<>();

                for (WorkoutItem workout : workouts) {
                    long start = workout.timestamp;
                    long end = start + (workout.durationSeconds * 1000L);

                    List<HealthLogItem> p = filterLogs(snapshot.child("pulse"), start, end);
                    List<HealthLogItem> pc = filterLogs(snapshot.child("pace"), start, end);
                    List<HealthLogItem> ox = filterLogs(snapshot.child("oxygen"), start, end);

                    // Передаем пульс, кислород и темп для глубокой аналитики
                    List<WorkoutSessionUI.WorkoutAdvice> insights = generateSmartInsights(workout, p, pc, ox);

                    resultList.add(new WorkoutSessionUI(workout, p, pc, ox, insights));
                }
                workoutSessions.setValue(resultList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private List<HealthLogItem> filterLogs(DataSnapshot node, long start, long end) {
        List<HealthLogItem> list = new ArrayList<>();
        for (DataSnapshot child : node.getChildren()) {
            HealthLogItem item = child.getValue(HealthLogItem.class);
            if (item != null && item.time >= start && item.time <= end) {
                list.add(item);
            }
        }
        return list;
    }

    // ==========================================
    // ГЛУБОКАЯ АНАЛИТИКА ТРЕНИРОВКИ (ВИРТУАЛЬНЫЙ ТРЕНЕР)
    // ==========================================
    @SuppressLint("StringFormatInvalid")
    private List<WorkoutSessionUI.WorkoutAdvice> generateSmartInsights(
            WorkoutItem workout,
            List<HealthLogItem> pulseList,
            List<HealthLogItem> speedList, // Изменили название на speedList
            List<HealthLogItem> oxList) {

        List<WorkoutSessionUI.WorkoutAdvice> analytics = new ArrayList<>();

        double durationMinutes = workout.durationSeconds / 60.0;
        double calsPerMinute = durationMinutes > 0 ? workout.calories / durationMinutes : 0;

        // 1. ЭНЕРГЕТИЧЕСКИЙ ПРОФИЛЬ И ТОПЛИВО
        if (durationMinutes > 0) {
            if (calsPerMinute >= 10.0) {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "⚡", getApplication().getString(R.string.advice_history_workout_energy_extreme),
                        String.format(Locale.getDefault(), getApplication().getString(R.string.advice_history_workout_energy_extreme_desc), calsPerMinute), "Middle"));
            } else if (calsPerMinute >= 5.0) {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "🔥", getApplication().getString(R.string.advice_history_workout_energy_avarage),
                        String.format(Locale.getDefault(), getApplication().getString(R.string.advice_history_workout_energy_avarage_desc), calsPerMinute), "Middle"));
            } else {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "🚶", getApplication().getString(R.string.advice_history_workout_energy_low),
                        String.format(Locale.getDefault(),getApplication().getString(R.string.advice_history_workout_energy_low_desc) , calsPerMinute), "Middle"));
            }
        }

        // 2. КАРДИО-АНАЛИТИКА И НАГРУЗКА НА СЕРДЦЕ
        int maxPulse = 0, minPulse = 999, sumPulse = 0, avgPulse = 0;
        if (!pulseList.isEmpty()) {
            for (HealthLogItem p : pulseList) {
                if (p.val > maxPulse) maxPulse = (int) p.val;
                if (p.val < minPulse && p.val > 0) minPulse = (int) p.val;
                sumPulse += p.val;
            }
            avgPulse = sumPulse / pulseList.size();

            if (maxPulse - avgPulse >= 35 && maxPulse > 140) {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "🎢", getApplication().getString(R.string.advice_history_workout_pulse_rate_interval),
                        getApplication().getString(R.string.advice_history_workout_pulse_rate_interval_dec_1)  + minPulse
                                + getApplication().getString(R.string.text_to) + maxPulse + getApplication().getString(R.string.advice_history_workout_pulse_rate_interval_dec_2), "Middle"));
            } else {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "➖", getApplication().getString(R.string.advice_history_workout_pulse_rate_steady),
                        getApplication().getString(R.string.advice_history_workout_pulse_rate_steady_dec_1) + avgPulse + getApplication().getString(R.string.advice_history_workout_pulse_rate_steady_dec_2), "Middle"));
            }

            if (maxPulse > 165) {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "🫀", getApplication().getString(R.string.advice_history_workout_pulse_rate_myocardial),
                        getApplication().getString(R.string.advice_history_workout_pulse_rate_myocardial_dec_1) + maxPulse + getApplication().getString(R.string.advice_history_workout_pulse_rate_myocardial_dec_2), "End"));
            }
        }

        // 3. МЕХАНИЧЕСКАЯ АНАЛИТИКА (СКОРОСТЬ И ВЫНОСЛИВОСТЬ)
        if (speedList != null && speedList.size() > 4) {
            double maxSpeed = 0;
            double sumSpeed = 0;
            for (HealthLogItem s : speedList) {
                if (s.val > maxSpeed) maxSpeed = s.val;
                sumSpeed += s.val;
            }
            double avgSpeed = sumSpeed / speedList.size();

            // Сравнение первой и второй половины (разбиваем массив пополам)
            int half = speedList.size() / 2;
            double sumFirstHalf = 0;
            for (int i = 0; i < half; i++) sumFirstHalf += speedList.get(i).val;
            double avgFirstHalf = sumFirstHalf / half;

            double sumSecondHalf = 0;
            for (int i = half; i < speedList.size(); i++) sumSecondHalf += speedList.get(i).val;
            double avgSecondHalf = sumSecondHalf / (speedList.size() - half);

            // А) Анализ на наличие мощных спринтов
            if (maxSpeed > avgSpeed * 1.6 && maxSpeed > 8.0) {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "🚀", getApplication().getString(R.string.advice_history_workout_explosive_power),
                        String.format(Locale.getDefault(), getApplication().getString(R.string.advice_history_workout_explosive_power_dec), maxSpeed), "Middle"));
            }

            // Б) Анализ пейсинга (сохранение скорости)
            if (avgFirstHalf > 2.0) { // Исключаем стояние на месте
                if (avgSecondHalf < avgFirstHalf * 0.85) {
                    // Скорость упала больше чем на 15%
                    analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                            "📉",
                            getApplication().getString(R.string.advice_history_workout_muscle_fatigue),
                            getApplication().getString(R.string.advice_history_workout_muscle_fatigue_desc),
                            "End"));
                } else if (avgSecondHalf >= avgFirstHalf * 0.95 && avgSecondHalf <= avgFirstHalf * 1.05) {
                    // Скорость осталась практически такой же
                    analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                            "🎯",
                            getApplication().getString(R.string.advice_history_workout_perfect_pacing),
                            getApplication().getString(R.string.advice_history_workout_perfect_pacing_desc),
                            "Middle"));
                }
            }
        }

        // 4. АНАЛИЗ КИСЛОРОДА (VO2 Max / Анаэробный порог)
        if (oxList != null && !oxList.isEmpty()) {
            int minOx = 100;
            for(HealthLogItem ox : oxList) {
                if(ox.val < minOx && ox.val > 0) minOx = (int) ox.val;
            }
            if (minOx < 95 && maxPulse > 150) {
                analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                        "🫁",
                        getApplication().getString(R.string.advice_history_workout_oxygen_debt),
                        getApplication().getString(R.string.advice_history_workout_oxygen_debt_desc, minOx),
                        "End"));
            }
        }

        // 5. ЦНС И ВОССТАНОВЛЕНИЕ
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(workout.timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 20 && (avgPulse > 130 || calsPerMinute > 7.0)) {
            analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                    "🌙",
                    getApplication().getString(R.string.advice_history_workout_cns_stress),
                    getApplication().getString(R.string.advice_history_workout_cns_stress_desc),
                    "End"));
        }

        // Базовая заглушка
        if (analytics.isEmpty()) {
            analytics.add(new WorkoutSessionUI.WorkoutAdvice(
                    "📊",
                    getApplication().getString(R.string.advice_history_workout_activity_summary),
                    getApplication().getString(R.string.advice_history_workout_activity_summary_desc, Math.round(workout.calories)),
                    "End"));
        }

        return analytics;
    }
}