package com.example.fitnesapp.ui.activetren;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

public class ActiveTrenViewModel extends ViewModel {

    private DatabaseReference userRef;
    private final MutableLiveData<List<WorkoutSessionUI>> workoutSessions = new MutableLiveData<>();

    public ActiveTrenViewModel() {
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

                    // === ГЕНЕРИРУЕМ УМНЫЕ СОВЕТЫ ===
                    List<WorkoutSessionUI.WorkoutAdvice> insights = generateSmartInsights(workout, p);

                    // Добавляем советы в финальный объект (обновите конструктор WorkoutSessionUI)
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
    // ЛОГИКА ГЕНЕРАЦИИ СОВЕТОВ (ВИРТУАЛЬНЫЙ ТРЕНЕР)
    // ==========================================
    private List<WorkoutSessionUI.WorkoutAdvice> generateSmartInsights(WorkoutItem workout, List<HealthLogItem> pulseList) {
        List<WorkoutSessionUI.WorkoutAdvice> advices = new ArrayList<>();

        // 1. Анализ пульса
        if (!pulseList.isEmpty()) {
            int maxPulse = 0;
            int sumPulse = 0;
            for (HealthLogItem p : pulseList) {
                if (p.val > maxPulse) maxPulse = (int) p.val;
                sumPulse += p.val;
            }
            int avgPulse = sumPulse / pulseList.size();

            if (maxPulse > 165) {
                advices.add(new WorkoutSessionUI.WorkoutAdvice("⚡", "Пиковая нагрузка!", "Твой пульс достигал " + maxPulse + " уд/мин. Отличная работа на взрывную силу, но удели время плавной заминке.", "Middle"));
            } else if (avgPulse >= 110 && avgPulse <= 140) {
                advices.add(new WorkoutSessionUI.WorkoutAdvice("🔥", "Зона жиросжигания", "Идеальный средний пульс (" + avgPulse + " уд/мин) для сжигания липидов и тренировки сердца.", "End"));
            } else if (avgPulse < 100) {
                advices.add(new WorkoutSessionUI.WorkoutAdvice("🚶", "Легкая активность", "Хороший темп для активного восстановления мышц и разгона крови.", "End"));
            }
        }

        // 2. Анализ калорий
        if (workout.calories > 400) {
            advices.add(new WorkoutSessionUI.WorkoutAdvice("💧", "Водный баланс", "Ты сжег много энергии (" + workout.calories + " ккал). Твоя норма воды увеличена, обязательно попей!", "End"));
        }

        // 3. Анализ времени суток (через Timestamp)
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(workout.timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 20) {
            advices.add(new WorkoutSessionUI.WorkoutAdvice("🌙", "Поздняя сессия", "После вечерней тренировки прими теплый душ, чтобы снизить кортизол перед сном.", "End"));
        } else if (hour <= 9) {
            advices.add(new WorkoutSessionUI.WorkoutAdvice("☀️", "Ранняя пташка", "Утренняя тренировка отлично разгоняет метаболизм на весь оставшийся день!", "Start"));
        }

        // 4. Анализ длительности
        if (workout.durationSeconds > 3600) {
            advices.add(new WorkoutSessionUI.WorkoutAdvice("🥩", "Долгая тренировка", "Мышцы потратили много гликогена. Легкий углеводно-белковый перекус сейчас не повредит.", "End"));
        }

        // Базовый совет, если ничего не подошло
        if (advices.isEmpty()) {
            advices.add(new WorkoutSessionUI.WorkoutAdvice("🏆", "Workout Finished!", "Отличная работа! Продолжай в том же духе.", "End"));
        }

        return advices;
    }
}