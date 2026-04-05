package com.example.fitnesapp.ui.oxygenlevel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.R;
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

// ИЗМЕНЕНИЕ: Наследуемся от AndroidViewModel
public class OxygenViewModel extends AndroidViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<List<HealthLogItem>> oxygenHistory = new MutableLiveData<>();
    private final MutableLiveData<OxygenAnalysis> analysisData = new MutableLiveData<>();

    private DatabaseReference userRef;
    private String selectedDateKey;
    private ValueEventListener historyListener;

    // Класс для хранения рассчитанной статистики
    public static class OxygenAnalysis {
        public double min = 0;
        public double max = 0;
        public double avg = 0;
        public String status = "--";
    }

    // ИЗМЕНЕНИЕ: Конструктор теперь принимает Application
    public OxygenViewModel(@NonNull Application application) {
        super(application);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            loadDataForDate(new Date()); // Грузим сегодня по умолчанию
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<List<HealthLogItem>> getOxygenHistory() { return oxygenHistory; }
    public LiveData<OxygenAnalysis> getAnalysisData() { return analysisData; }

    // --- Загрузка Профиля ---
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

    // --- Смена даты и загрузка данных ---
    public void loadDataForDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String newDateKey = sdf.format(date);

        if (newDateKey.equals(selectedDateKey)) return;
        selectedDateKey = newDateKey;

        if (historyListener != null && userRef != null) {
            userRef.child("health_logs").child("oxygen").removeEventListener(historyListener);
        }

        loadOxygenHistory(date);
    }

    private void loadOxygenHistory(Date date) {
        if (userRef == null) return;

        // Вычисляем начало и конец дня
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        long startTimestamp = calendar.getTimeInMillis();

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        long endTimestamp = calendar.getTimeInMillis();

        // Запрос к Firebase (фильтрация по времени)
        com.google.firebase.database.Query query = userRef.child("health_logs").child("oxygen")
                .orderByChild("time")
                .startAt(startTimestamp)
                .endAt(endTimestamp);

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<HealthLogItem> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    HealthLogItem item = child.getValue(HealthLogItem.class);
                    if (item != null) {
                        list.add(item);
                    }
                }
                oxygenHistory.setValue(list);
                calculateAnalysis(list);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        query.addValueEventListener(historyListener);
    }

    private void calculateAnalysis(List<HealthLogItem> logs) {
        OxygenAnalysis analysis = new OxygenAnalysis();

        analysis.status = "--";

        if (logs == null || logs.isEmpty()) {
            analysisData.setValue(analysis);
            return;
        }

        double min = 100;
        double max = 0;
        double sum = 0;

        for (HealthLogItem item : logs) {
            double val = item.val;
            if (val < min) min = val;
            if (val > max) max = val;
            sum += val;
        }

        analysis.min = min;
        analysis.max = max;
        analysis.avg = sum / logs.size();

        // ИЗМЕНЕНИЕ: Определение статуса через локализованные строки
        if (analysis.avg >= 95) {
            analysis.status = getApplication().getString(R.string.oxygen_status_very_well);
        } else if (analysis.avg >= 93) {
            analysis.status = getApplication().getString(R.string.oxygen_status_normal);
        } else if (analysis.avg >= 90) {
            analysis.status = getApplication().getString(R.string.oxygen_status_low);
        } else {
            analysis.status = getApplication().getString(R.string.oxygen_status_critical);
        }

        analysisData.setValue(analysis);
    }
}