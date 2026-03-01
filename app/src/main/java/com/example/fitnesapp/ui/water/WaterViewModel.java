package com.example.fitnesapp.ui.water;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WaterViewModel extends ViewModel {

    private DatabaseReference userRef;
    private DatabaseReference waterRef;
    private String todayDate;

    private final MutableLiveData<Integer> consumedWater = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> waterGoal = new MutableLiveData<>(2500);
    private final MutableLiveData<String> adviceText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isFeedbackGiven = new MutableLiveData<>(false);

    private int weatherBonusMl = 0; // Добавочная вода из-за жары

    public WaterViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
            waterRef = userRef.child("daily_data").child(todayDate).child("water");
            loadWaterData();
            // Обратите внимание: расчет цели теперь вызывается из Фрагмента после получения геолокации
        }
    }

    public LiveData<Integer> getConsumedWater() { return consumedWater; }
    public LiveData<Integer> getWaterGoal() { return waterGoal; }
    public LiveData<String> getAdviceText() { return adviceText; }
    public LiveData<Boolean> getIsFeedbackGiven() { return isFeedbackGiven; }

    // ==========================================
    // ЧТЕНИЕ ДАННЫХ
    // ==========================================
    private void loadWaterData() {
        if (waterRef == null) return;
        waterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer consumed = snapshot.child("consumedMl").getValue(Integer.class);
                    Integer goal = snapshot.child("goalMl").getValue(Integer.class);
                    Boolean voted = snapshot.child("hasVotedToday").getValue(Boolean.class);

                    if (consumed != null) consumedWater.setValue(consumed);
                    if (goal != null) waterGoal.setValue(goal);
                    if (voted != null) isFeedbackGiven.setValue(voted);
                } else {
                    // Инициализация нового дня
                    waterRef.child("consumedMl").setValue(0);
                    waterRef.child("goalMl").setValue(2500);
                    waterRef.child("hasVotedToday").setValue(false);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================================
    // ДОБАВЛЕНИЕ И ОТМЕНА ВОДЫ
    // ==========================================
    public void addWater(int amountMl) {
        if (waterRef != null) {
            waterRef.child("consumedMl").setValue(ServerValue.increment(amountMl));
            // Лог с секундами для точной сортировки
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            waterRef.child("logs").child(currentTime).setValue(amountMl);
        }
    }

    public void returnWater() {
        if (waterRef == null) return;

        // Ищем самую последнюю добавленную порцию воды в базе
        waterRef.child("logs").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String lastTimeKey = child.getKey();
                                Integer lastAmount = child.getValue(Integer.class);

                                if (lastTimeKey != null && lastAmount != null) {
                                    // Вычитаем найденный объем
                                    waterRef.child("consumedMl").setValue(ServerValue.increment(-lastAmount));
                                    // Удаляем эту запись, чтобы следующий раз удалилась предыдущая
                                    waterRef.child("logs").child(lastTimeKey).removeValue();
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ==========================================
    // ПОГОДА И СМАРТ-РАСЧЕТ
    // ==========================================
    public void calculateWithoutWeather() {
        weatherBonusMl = 0;
        calculateSmartWaterGoal();
    }

    public void fetchWeatherAndCalculate(double lat, double lon) {
        new Thread(() -> {
            try {
                // ВАЖНО: Вставьте сюда ваш настоящий API-ключ OpenWeatherMap
                String apiKey = "8aeb28f3a18de490c4765b80c3480a29";
                String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&units=metric&appid=" + apiKey;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                    StringBuilder response = new StringBuilder();
                    int data = reader.read();
                    while (data != -1) {
                        response.append((char) data);
                        data = reader.read();
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONObject main = jsonObject.getJSONObject("main");
                    double temp = main.getDouble("temp");

                    // Бонус за жару
                    if (temp >= 30) weatherBonusMl = 800;
                    else if (temp >= 25) weatherBonusMl = 500;
                    else if (temp >= 20) weatherBonusMl = 300;
                    else weatherBonusMl = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                weatherBonusMl = 0;
            }

            // Запускаем расчет в главном потоке
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::calculateSmartWaterGoal);

        }).start();
    }

    private void calculateSmartWaterGoal() {
        if (userRef == null) return;

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile profile = snapshot.child("profile").getValue(UserProfile.class);
                DailyData daily = snapshot.child("daily_data").child(todayDate).getValue(DailyData.class);

                if (profile == null) return;

                // База
                float weight = profile.weight > 0 ? profile.weight : 70f;
                int baseGoal = (int) (weight * ("Female".equalsIgnoreCase(profile.gender) ? 30 : 35));

                // Активность
                int stepBonus = 0;
                int workoutBonus = 0;

                if (daily != null) {
                    if (daily.steps > 0) stepBonus = (daily.steps / 5000) * 250;
                    if (daily.workouts != null) {
                        int totalWorkoutMin = 0;
                        for (com.example.fitnesapp.models.firebase.WorkoutItem w : daily.workouts.values()) {
                            totalWorkoutMin += w.durationMin;
                        }
                        workoutBonus = (totalWorkoutMin / 30) * 300;
                    }
                }
                int extraActivityWater = stepBonus + workoutBonus;

                // Самообучаемый коэффициент (ML)
                float multiplier = profile.waterMultiplier > 0 ? profile.waterMultiplier : 1.0f;
                int rawTotal = baseGoal + extraActivityWater + weatherBonusMl;
                int finalGoal = Math.round(rawTotal * multiplier);

                // Ограничители
                if (finalGoal < 1500) finalGoal = 1500;
                if (finalGoal > 5000) finalGoal = 5000;

                // Формируем детальное объяснение
                StringBuilder explanation = new StringBuilder();
                explanation.append("🔹 База (от веса): ").append(baseGoal).append(" мл\n");
                if (stepBonus > 0) explanation.append("🏃 Активность (шаги): +").append(stepBonus).append(" мл\n");
                if (workoutBonus > 0) explanation.append("🏋️ Тренировка: +").append(workoutBonus).append(" мл\n");
                if (weatherBonusMl > 0) explanation.append("☀️ Жаркая погода: +").append(weatherBonusMl).append(" мл\n");

                if (multiplier != 1.0f) {
                    int mlDifference = finalGoal - rawTotal;
                    if (mlDifference != 0) {
                        String sign = mlDifference > 0 ? "+" : "";
                        explanation.append("🧠 Умная подстройка: ").append(sign).append(mlDifference).append(" мл\n");
                    }
                }

                adviceText.setValue(explanation.toString().trim());
                waterGoal.setValue(finalGoal);
                waterRef.child("goalMl").setValue(finalGoal);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================================
    // ОБРАТНАЯ СВЯЗЬ (ОБУЧЕНИЕ МОДЕЛИ)
    // ==========================================
    public void submitFeedback(int rating) {
        if (userRef == null) return;

        waterRef.child("hasVotedToday").setValue(true);
        isFeedbackGiven.setValue(true);

        userRef.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Float currentMultiplier = snapshot.child("waterMultiplier").getValue(Float.class);
                if (currentMultiplier == null || currentMultiplier <= 0) currentMultiplier = 1.0f;

                if (rating == 1) currentMultiplier -= 0.05f; // Слишком много
                else if (rating == 3) currentMultiplier += 0.05f; // Хотелось пить еще

                if (currentMultiplier < 0.7f) currentMultiplier = 0.7f;
                if (currentMultiplier > 1.5f) currentMultiplier = 1.5f;

                userRef.child("profile").child("waterMultiplier").setValue(currentMultiplier);
                calculateSmartWaterGoal();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}