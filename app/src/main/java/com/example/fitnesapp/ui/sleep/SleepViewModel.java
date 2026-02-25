package com.example.fitnesapp.ui.sleep;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.SleepStageItem;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.utils.MLPredictor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// ВАЖНО: Изменили на AndroidViewModel, чтобы работал MLPredictor
public class SleepViewModel extends AndroidViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData.Sleep> sleepData = new MutableLiveData<>();

    private DatabaseReference userRef;

    private DatabaseReference currentSleepRef;
    private ValueEventListener sleepListener;
    private String selectedDateKey;

    // Наша нейросеть
    private MLPredictor mlPredictor;

    public SleepViewModel(@NonNull Application application) {
        super(application);

        // Инициализируем предсказатель
        mlPredictor = new MLPredictor(application);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            loadSleepData(new Date());
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData.Sleep> getSleepData() { return sleepData; }

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

    public void loadSleepData(Date date) {
        if (userRef == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String newDateKey = sdf.format(date);

        if (newDateKey.equals(selectedDateKey)) return;
        selectedDateKey = newDateKey;

        if (currentSleepRef != null && sleepListener != null) {
            currentSleepRef.removeEventListener(sleepListener);
        }

        currentSleepRef = userRef.child("daily_data").child(selectedDateKey).child("sleep");

        sleepListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DailyData.Sleep currentSleep = snapshot.getValue(DailyData.Sleep.class);
                    if (currentSleep != null) {
                        // Данные за текущий день получены! Теперь запрашиваем историю за неделю для ML
                        fetchHistoryAndCalculateML(currentSleep);
                    }
                } else {
                    sleepData.setValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        currentSleepRef.addValueEventListener(sleepListener);
    }

    // Метод, который скачивает историю и пропускает данные через ИИ
    private void fetchHistoryAndCalculateML(DailyData.Sleep currentSleep) {
        // Запрашиваем 8 последних дней до выбранной даты
        userRef.child("daily_data").orderByKey().endAt(selectedDateKey).limitToLast(8)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot historySnap) {
                        long totalHistorySleep = 0;
                        int validDays = 0;

                        // Считаем средний сон за прошлые дни
                        for (DataSnapshot daySnap : historySnap.getChildren()) {
                            String key = daySnap.getKey();
                            if (key != null && !key.equals(selectedDateKey)) {
                                DailyData data = daySnap.getValue(DailyData.class);
                                if (data != null && data.sleep != null && data.sleep.durationMinutes > 0) {
                                    totalHistorySleep += data.sleep.durationMinutes;
                                    validDays++;
                                }
                            }
                        }

                        long avgHistory = validDays > 0 ? totalHistorySleep / validDays : 420;

                        // Обогащаем объект сна данными из нейросети
                        enrichSleepWithML(currentSleep, avgHistory);

                        // Отправляем прокачанный объект во Фрагмент!
                        sleepData.setValue(currentSleep);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Если история не скачалась, отправляем как есть
                        sleepData.setValue(currentSleep);
                    }
                });
    }

    private void enrichSleepWithML(DailyData.Sleep sleep, long avgHistoryMinutes) {
        long totalSleep = sleep.durationMinutes;
        long deepSleep = sleep.phases != null ? sleep.phases.deep : 0;
        long remSleep = sleep.phases != null ? sleep.phases.rem : 0;

        // Считаем пробуждения прямо из графика гипнограммы
        // Считаем пробуждения прямо из графика гипнограммы
        int awakenings = 0;
        if (sleep.hypnogram != null && !sleep.hypnogram.isEmpty()) {
            int previousStage = -1; // -1 значит "еще нет данных"

            for (SleepStageItem item : sleep.hypnogram.values()) {
                int currentStage = item.stage; // Берем вашу цифру (1, 2, 3 или 4)

                // Если текущая фаза 4 (Awake), а предыдущая была не Awake (и не самое начало)
                if (currentStage == 4 && previousStage != 4 && previousStage != -1) {
                    awakenings++;
                }

                previousStage = currentStage;
            }

            // Вычитаем 1, так как последнее "пробуждение" в графике — это окончательный подъем утром
            if (awakenings > 0) awakenings--;
        }

        // Прогоняем через нейросеть
        float mlScore = mlPredictor.predictSleepQuality(totalSleep, deepSleep, remSleep, awakenings, avgHistoryMinutes);

        if (mlScore >= 0f) {
            // Перезаписываем поля объекта!
            sleep.score = Math.round(mlScore * 100);

            if (mlScore >= 0.8f) {
                sleep.quality = "Отлично";
            } else if (mlScore >= 0.5f) {
                sleep.quality = "Норма";
            } else {
                sleep.quality = "Плохо";
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentSleepRef != null && sleepListener != null) {
            currentSleepRef.removeEventListener(sleepListener);
        }
        if (mlPredictor != null) {
            mlPredictor.close();
        }
    }
}