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
import java.util.List;

public class ActiveTrenViewModel extends ViewModel {

    private DatabaseReference userRef;
    // Главный список для UI
    private final MutableLiveData<List<WorkoutSessionUI>> workoutSessions = new MutableLiveData<>();

    public ActiveTrenViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
    }

    public LiveData<List<WorkoutSessionUI>> getWorkoutSessions() { return workoutSessions; }

    public void loadWorkoutsForDate(String date) {
        if (userRef == null) return;

        // 1. Грузим данные за день (там список тренировок)
        userRef.child("daily_data").child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DailyData dailyData = snapshot.getValue(DailyData.class);
                if (dailyData != null && dailyData.workouts != null && !dailyData.workouts.isEmpty()) {
                    List<WorkoutItem> workouts = new ArrayList<>(dailyData.workouts.values());
                    // Сортируем по времени (сначала утренние)
                    workouts.sort((w1, w2) -> Long.compare(w1.timestamp, w2.timestamp));

                    // 2. Для каждой тренировки нужно подтянуть логи
                    loadLogsAndCombine(workouts);
                } else {
                    workoutSessions.setValue(new ArrayList<>()); // Пусто
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLogsAndCombine(List<WorkoutItem> workouts) {
        // Чтобы не усложнять, загрузим ВСЕ логи, а потом отфильтруем в памяти.
        // Это быстрее, чем делать 30 запросов в базу.
        userRef.child("health_logs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<WorkoutSessionUI> resultList = new ArrayList<>();

                for (WorkoutItem workout : workouts) {
                    long start = workout.timestamp;
                    long end = start + (workout.durationMin * 60000L);

                    List<HealthLogItem> p = filterLogs(snapshot.child("pulse"), start, end);
                    List<HealthLogItem> pc = filterLogs(snapshot.child("pace"), start, end);
                    List<HealthLogItem> ox = filterLogs(snapshot.child("oxygen"), start, end);

                    resultList.add(new WorkoutSessionUI(workout, p, pc, ox));
                }
                workoutSessions.setValue(resultList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Хелпер фильтрации
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
}