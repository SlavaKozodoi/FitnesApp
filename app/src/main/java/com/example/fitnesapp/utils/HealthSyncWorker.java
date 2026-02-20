package com.example.fitnesapp.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HealthSyncWorker extends Worker {

    public HealthSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.failure(); // Если юзер не авторизован, отменяем
        }

        HealthConnectManager healthManager = new HealthConnectManager(getApplicationContext());
        if (!healthManager.isAvailable()) {
            return Result.failure();
        }

        String uid = user.getUid();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
        DatabaseReference dailyRef = userRef.child("daily_data").child(todayDate);

        try {
            // === 1. СИНХРОНИЗИРУЕМ ШАГИ ===
            Long steps = healthManager.readStepsForToday().get();
            Tasks.await(dailyRef.child("steps").setValue(steps));

            // === 2. СИНХРОНИЗИРУЕМ КАЛОРИИ ===
            Double cals = healthManager.readCaloriesForToday().get();
            Tasks.await(dailyRef.child("caloriesBurned").setValue(cals));

            // === 3. СИНХРОНИЗИРУЕМ ПУЛЬС ===
            List<HeartRateData> pulseList = healthManager.readHeartRateHistory().get();
            if (!pulseList.isEmpty()) {
                Map<String, Object> pulseUpdates = new HashMap<>();
                for (HeartRateData item : pulseList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("time", item.getTime());
                    map.put("val", item.getBpm());
                    pulseUpdates.put(String.valueOf(item.getTime()), map);
                }
                Tasks.await(userRef.child("health_logs").child("pulse").updateChildren(pulseUpdates));
            }

            // === 4. СИНХРОНИЗИРУЕМ ТРЕНИРОВКИ ===
            List<WorkoutSessionData> workouts = healthManager.readWorkoutSessions().get();
            if (!workouts.isEmpty()) {
                Map<String, Object> workoutUpdates = new HashMap<>();
                for (WorkoutSessionData item : workouts) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", item.getType());
                    map.put("calories", item.getCalories());
                    map.put("durationMin", item.getDurationMinutes());
                    map.put("timestamp", item.getStartTime());
                    workoutUpdates.put(String.valueOf(item.getStartTime()), map);
                }
                Tasks.await(dailyRef.child("workouts").updateChildren(workoutUpdates));
            }

            // === 5. СИНХРОНИЗИРУЕМ СОН (Графики + Сводка) ===
            List<SleepSessionData> sleepSessions = healthManager.readSleepSessions().get();
            if (!sleepSessions.isEmpty()) {
                Map<String, Object> logsUpdates = new HashMap<>();
                long minStartTime = Long.MAX_VALUE;
                long maxEndTime = Long.MIN_VALUE;
                long totalDuration = 0, totalDeep = 0, totalLight = 0, totalRem = 0, totalAwake = 0;

                for (SleepSessionData item : sleepSessions) {
                    String key = String.valueOf(item.getStartTime());
                    Map<String, Object> map = new HashMap<>();
                    map.put("startTime", item.getStartTime());
                    map.put("endTime", item.getEndTime());
                    map.put("duration", item.getDurationMinutes());
                    map.put("stage", 2);
                    logsUpdates.put(key, map);

                    if (item.getStartTime() < minStartTime) minStartTime = item.getStartTime();
                    if (item.getEndTime() > maxEndTime) maxEndTime = item.getEndTime();
                    totalDeep += item.getDeepSleepMin();
                    totalLight += item.getLightSleepMin();
                    totalRem += item.getRemSleepMin();
                    totalAwake += item.getAwakeMin();
                }

                if (minStartTime != Long.MAX_VALUE && maxEndTime != Long.MIN_VALUE) {
                    totalDuration = (maxEndTime - minStartTime) / (1000 * 60);
                }

                // Сохраняем логи для графика
                Tasks.await(userRef.child("health_logs").child("sleep").updateChildren(logsUpdates));

                // Формируем сводку для экрана сна
                Map<String, Object> dailySleepMap = new HashMap<>();
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

                dailySleepMap.put("durationMinutes", totalDuration);
                dailySleepMap.put("bedTime", (minStartTime != Long.MAX_VALUE) ? timeFmt.format(new Date(minStartTime)) : "--:--");
                dailySleepMap.put("wakeTime", (maxEndTime != Long.MIN_VALUE) ? timeFmt.format(new Date(maxEndTime)) : "--:--");
                dailySleepMap.put("fallingAsleepMin", 0);

                // Оценка сна
                int score = 60;
                if (totalDuration >= 420 && totalDuration <= 540) score = 100;
                else if (totalDuration > 540) score = 90;
                else if (totalDuration >= 360) score = 80;
                dailySleepMap.put("score", score);

                String quality = "Fair";
                if (totalDuration >= 420) quality = "Excellent";
                else if (totalDuration >= 360) quality = "Good";
                dailySleepMap.put("quality", quality);

                // Круговые диаграммы фаз
                long totalPhases = totalDeep + totalLight + totalRem + totalAwake;
                if (totalPhases == 0) totalPhases = 1; // Защита от деления на 0
                Map<String, Object> phasesMap = new HashMap<>();
                phasesMap.put("deep", (int) ((totalDeep * 100) / totalPhases));
                phasesMap.put("surface", (int) ((totalLight * 100) / totalPhases));
                phasesMap.put("rem", (int) ((totalRem * 100) / totalPhases));
                phasesMap.put("awake", (int) ((totalAwake * 100) / totalPhases));
                dailySleepMap.put("phases", phasesMap);

                Tasks.await(dailyRef.child("sleep").updateChildren(dailySleepMap));
            }

            // === 6. СИНХРОНИЗИРУЕМ КИСЛОРОД (SpO2) ===
            List<OxygenData> oxygenList = healthManager.readOxygenHistory().get();
            if (!oxygenList.isEmpty()) {
                Map<String, Object> oxyUpdates = new HashMap<>();
                for (OxygenData item : oxygenList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("time", item.getTime());
                    map.put("val", item.getPercentage());
                    oxyUpdates.put(String.valueOf(item.getTime()), map);
                }
                Tasks.await(userRef.child("health_logs").child("oxygen").updateChildren(oxyUpdates));
            }

            // === 7. СИНХРОНИЗИРУЕМ ПИТАНИЕ (Nutrition) ===
            List<MealData> meals = healthManager.readMealsForToday().get();
            if (!meals.isEmpty()) {
                Map<String, Object> mealUpdates = new HashMap<>();
                double totalMealCals = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0;
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

                for (MealData item : meals) {
                    String key = String.valueOf(item.getTime());
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", item.getName());
                    map.put("calories", item.getCalories());
                    map.put("protein", item.getProtein());
                    map.put("carbs", item.getCarbs());
                    map.put("fat", item.getFat());
                    map.put("time", timeFmt.format(new Date(item.getTime())));
                    mealUpdates.put(key, map);

                    totalMealCals += item.getCalories();
                    totalProtein += item.getProtein();
                    totalCarbs += item.getCarbs();
                    totalFat += item.getFat();
                }

                // Сохраняем список съеденного
                Tasks.await(dailyRef.child("meals").setValue(mealUpdates));

                // Обновляем общую сводку БЖУ за день
                Map<String, Object> summaryMap = new HashMap<>();
                summaryMap.put("totalCalories", (int) totalMealCals);
                summaryMap.put("carbs", (int) totalCarbs);
                summaryMap.put("protein", (int) totalProtein);
                summaryMap.put("fat", (int) totalFat);
                // Заметьте: мы НЕ обновляем maxCalories (цель), чтобы не затереть ее
                Tasks.await(dailyRef.child("nutrition").updateChildren(summaryMap));
            }

            // === 8. СИНХРОНИЗИРУЕМ ИСТОРИЮ (Графики 30-min шагов и калорий) ===
            List<ActivityBucket> buckets = healthManager.readHistoryForToday().get();
            if (!buckets.isEmpty()) {
                Map<String, Object> hourlyUpdates = new HashMap<>();
                for (ActivityBucket item : buckets) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("time", item.getStartTime());
                    map.put("steps", item.getSteps());
                    map.put("calories", item.getCalories());
                    hourlyUpdates.put(String.valueOf(item.getStartTime()), map);
                }
                Tasks.await(dailyRef.child("hourly_activity").updateChildren(hourlyUpdates));
            }

            Log.d("HealthSyncWorker", "Background sync COMPLETED successfully for ALL metrics!");
            return Result.success();

        } catch (Exception e) {
            Log.e("HealthSyncWorker", "Background sync failed", e);
            // Если пропал интернет во время выгрузки, Worker попробует запустить задачу позже
            return Result.retry();
        }
    }
}