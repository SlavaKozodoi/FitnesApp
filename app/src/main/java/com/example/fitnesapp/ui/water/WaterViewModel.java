package com.example.fitnesapp.ui.water;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// ИЗМЕНЕНИЕ: Наследуемся от AndroidViewModel для доступа к строкам
public class WaterViewModel extends AndroidViewModel {

    private DatabaseReference userRef;
    private DatabaseReference waterRef;
    private String todayDate;

    private final MutableLiveData<Integer> consumedWater = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> waterGoal = new MutableLiveData<>(2500);
    private final MutableLiveData<String> adviceText = new MutableLiveData<>("");

    // ИЗМЕНЕНИЕ: Добавлен Application в конструктор
    public WaterViewModel(@NonNull Application application) {
        super(application);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
            waterRef = userRef.child("daily_data").child(todayDate).child("water");
            loadWaterData();
        }
    }

    public LiveData<Integer> getConsumedWater() { return consumedWater; }
    public LiveData<Integer> getWaterGoal() { return waterGoal; }
    public LiveData<String> getAdviceText() { return adviceText; }

    // ==========================================
    // ЧТЕНИЕ ДАННЫХ И ОБНОВЛЕНИЕ UI
    // ==========================================
    private void loadWaterData() {
        if (userRef == null || waterRef == null) return;

        // 1. Загружаем норму воды пользователя (глобальную)
        userRef.child("goals").child("dailyWaterMls").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int goal = 2500; // По умолчанию
                if (snapshot.exists() && snapshot.getValue(Integer.class) != null) {
                    goal = snapshot.getValue(Integer.class);
                }
                waterGoal.setValue(goal);
                updateAdvice(); // Обновляем совет при изменении нормы
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Загружаем выпитую воду за сегодня
        waterRef.child("consumedMl").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int consumed = 0;
                if (snapshot.exists() && snapshot.getValue(Integer.class) != null) {
                    consumed = snapshot.getValue(Integer.class);
                } else {
                    waterRef.child("consumedMl").setValue(0);
                }
                consumedWater.setValue(consumed);
                updateAdvice(); // Обновляем совет при добавлении воды
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================================
    // ЛОГИКА КОРОТКИХ СОВЕТОВ ПО ВОДЕ
    // ==========================================
    private void updateAdvice() {
        Integer consumed = consumedWater.getValue();
        Integer goal = waterGoal.getValue();

        if (consumed == null) consumed = 0;
        if (goal == null || goal == 0) goal = 2500;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        float progress = (float) consumed / goal;
        String tip = "";

        // ИЗМЕНЕНИЕ: Используем getString для локализации советов
        if (consumed >= goal) {
            tip = getApplication().getString(R.string.water_advice_goal_reached);
        } else if (hour < 12) {
            if (consumed == 0) tip = getApplication().getString(R.string.water_advice_morning_empty);
            else if (progress < 0.2f) tip = getApplication().getString(R.string.water_advice_morning_coffee);
            else tip = getApplication().getString(R.string.water_advice_morning_good);
        } else if (hour >= 12 && hour < 18) {
            if (progress < 0.4f) tip = getApplication().getString(R.string.water_advice_day_dehydrated);
            else tip = getApplication().getString(R.string.water_advice_day_good);
        } else {
            if (progress < 0.6f) tip = getApplication().getString(R.string.water_advice_evening_behind);
            else tip = getApplication().getString(R.string.water_advice_evening_almost);
        }

        adviceText.setValue(tip);
    }

    // ==========================================
    // УСТАНОВКА ПОЛЬЗОВАТЕЛЬСКОЙ НОРМЫ
    // ==========================================
    public void setCustomWaterGoal(int customGoal) {
        if (userRef != null) {
            // Сохраняем новую цель в профиле
            userRef.child("goals").child("dailyWaterMls").setValue(customGoal);
        }
    }

    // ==========================================
    // ДОБАВЛЕНИЕ И ОТМЕНА ВОДЫ
    // ==========================================
    public void addWater(int amountMl) {
        if (waterRef != null) {
            waterRef.child("consumedMl").setValue(ServerValue.increment(amountMl));
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            waterRef.child("logs").child(currentTime).setValue(amountMl);
        }
    }

    public void returnWater() {
        if (waterRef == null) return;
        waterRef.child("logs").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String lastTimeKey = child.getKey();
                                Integer lastAmount = child.getValue(Integer.class);

                                if (lastTimeKey != null && lastAmount != null) {
                                    waterRef.child("consumedMl").setValue(ServerValue.increment(-lastAmount));
                                    waterRef.child("logs").child(lastTimeKey).removeValue();
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}