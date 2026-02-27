package com.example.fitnesapp.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
            return Result.failure();
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
            // === 1. ШАГИ ===
            Long steps = healthManager.readStepsForToday().get();
            Tasks.await(dailyRef.child("steps").setValue(steps));

            // === 2. КАЛОРИИ ===
            Double cals = healthManager.readCaloriesForToday().get();
            Tasks.await(dailyRef.child("caloriesBurned").setValue(cals));

            // === 3. ПУЛЬС ===
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

            // === 4. ТРЕНИРОВКИ ===
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

            // === 5. СОН ===
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

                Tasks.await(userRef.child("health_logs").child("sleep").updateChildren(logsUpdates));

                Map<String, Object> dailySleepMap = new HashMap<>();
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
                dailySleepMap.put("durationMinutes", totalDuration);
                dailySleepMap.put("bedTime", (minStartTime != Long.MAX_VALUE) ? timeFmt.format(new Date(minStartTime)) : "--:--");
                dailySleepMap.put("wakeTime", (maxEndTime != Long.MIN_VALUE) ? timeFmt.format(new Date(maxEndTime)) : "--:--");
                dailySleepMap.put("fallingAsleepMin", 0);

                int score = 60;
                if (totalDuration >= 420 && totalDuration <= 540) score = 100;
                else if (totalDuration > 540) score = 90;
                else if (totalDuration >= 360) score = 80;
                dailySleepMap.put("score", score);

                String quality = "Fair";
                if (totalDuration >= 420) quality = "Excellent";
                else if (totalDuration >= 360) quality = "Good";
                dailySleepMap.put("quality", quality);

                long totalPhases = totalDeep + totalLight + totalRem + totalAwake;
                if (totalPhases == 0) totalPhases = 1;
                Map<String, Object> phasesMap = new HashMap<>();
                phasesMap.put("deep", (int) ((totalDeep * 100) / totalPhases));
                phasesMap.put("surface", (int) ((totalLight * 100) / totalPhases));
                phasesMap.put("rem", (int) ((totalRem * 100) / totalPhases));
                phasesMap.put("awake", (int) ((totalAwake * 100) / totalPhases));
                dailySleepMap.put("phases", phasesMap);

                Tasks.await(dailyRef.child("sleep").updateChildren(dailySleepMap));
            }

            // === 6. КИСЛОРОД (SpO2) ===
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

            // === 7. ПИТАНИЕ ===
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

                Tasks.await(dailyRef.child("meals").setValue(mealUpdates));

                Map<String, Object> summaryMap = new HashMap<>();
                summaryMap.put("totalCalories", (int) totalMealCals);
                summaryMap.put("carbs", (int) totalCarbs);
                summaryMap.put("protein", (int) totalProtein);
                summaryMap.put("fat", (int) totalFat);
                Tasks.await(dailyRef.child("nutrition").updateChildren(summaryMap));
            }

            // === 8. ИСТОРИЯ АКТИВНОСТИ ===
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

            // ==========================================================
            // === 9. УМНЫЙ ПОДСЧЕТ СТРИКОВ (ДНЕЙ ПОДРЯД) ДЛЯ АЧИВОК ===
            // ==========================================================
            DataSnapshot dailySnap = Tasks.await(dailyRef.get());
            DataSnapshot profileSnap = Tasks.await(userRef.child("profile").get());

            if (dailySnap.exists() && profileSnap.exists()) {
                int currentStepStreak = profileSnap.child("stepStreakDays").exists() ? profileSnap.child("stepStreakDays").getValue(Integer.class) : 0;
                int currentSleepStreak = profileSnap.child("perfectSleepDays").exists() ? profileSnap.child("perfectSleepDays").getValue(Integer.class) : 0;
                int currentRingsStreak = profileSnap.child("closedRingsStreak").exists() ? profileSnap.child("closedRingsStreak").getValue(Integer.class) : 0;

                // Флаги, чтобы не начислить стрик дважды за один день
                boolean stepAwarded = dailySnap.child("streaksAwarded").child("steps").exists() && Boolean.TRUE.equals(dailySnap.child("streaksAwarded").child("steps").getValue(Boolean.class));
                boolean sleepAwarded = dailySnap.child("streaksAwarded").child("sleep").exists() && Boolean.TRUE.equals(dailySnap.child("streaksAwarded").child("sleep").getValue(Boolean.class));
                boolean ringsAwarded = dailySnap.child("streaksAwarded").child("rings").exists() && Boolean.TRUE.equals(dailySnap.child("streaksAwarded").child("rings").getValue(Boolean.class));

                Map<String, Object> profileUpdates = new HashMap<>();
                Map<String, Object> dailyStreakUpdates = new HashMap<>();

                // Проверяем, конец ли это дня (после 23:00)
                Calendar cal = Calendar.getInstance();
                boolean isLateNight = cal.get(Calendar.HOUR_OF_DAY) >= 23;

                // --- ЛОГИКА ШАГОВ ---
                long actualSteps = dailySnap.child("steps").exists() ? dailySnap.child("steps").getValue(Long.class) : 0;
                long stepGoal = dailySnap.child("stepsGoal").exists() ? dailySnap.child("stepsGoal").getValue(Long.class) : 10000;

                if (actualSteps >= stepGoal && stepGoal > 0) {
                    if (!stepAwarded) {
                        profileUpdates.put("stepStreakDays", currentStepStreak + 1);
                        dailyStreakUpdates.put("steps", true);
                    }
                } else if (isLateNight && !stepAwarded) {
                    profileUpdates.put("stepStreakDays", 0); // Цель не выполнена, день закончен = обнуляем стрик
                }

                // --- ЛОГИКА СНА ---
                long sleepDur = dailySnap.child("sleep").child("durationMinutes").exists() ? dailySnap.child("sleep").child("durationMinutes").getValue(Long.class) : 0;
                if (sleepDur >= 420) { // Идеальный сон: 7+ часов (420 минут)
                    if (!sleepAwarded) {
                        profileUpdates.put("perfectSleepDays", currentSleepStreak + 1);
                        dailyStreakUpdates.put("sleep", true);
                    }
                } else if (isLateNight && !sleepAwarded) {
                    profileUpdates.put("perfectSleepDays", 0);
                }

                // --- ЛОГИКА 3 КОЛЕЦ ---
                long actualCals = dailySnap.child("caloriesBurned").exists() ? dailySnap.child("caloriesBurned").getValue(Long.class) : 0;
                long calGoal = dailySnap.child("caloriesGoal").exists() ? dailySnap.child("caloriesGoal").getValue(Long.class) : 2000;
                long actualNutr = dailySnap.child("nutrition").child("totalCalories").exists() ? dailySnap.child("nutrition").child("totalCalories").getValue(Long.class) : 0;
                long nutrGoal = dailySnap.child("nutrition").child("maxCalories").exists() ? dailySnap.child("nutrition").child("maxCalories").getValue(Long.class) : 0;

                int rings = 0;
                if (actualSteps >= stepGoal && stepGoal > 0) rings++;
                if (actualCals >= calGoal && calGoal > 0) rings++;
                if (nutrGoal > 0 && actualNutr >= nutrGoal) rings++;
                else if (nutrGoal <= 0) rings++; // Если цель питания не задана, кольцо дается по умолчанию

                if (rings >= 3) {
                    if (!ringsAwarded) {
                        profileUpdates.put("closedRingsStreak", currentRingsStreak + 1);
                        dailyStreakUpdates.put("rings", true);
                    }
                } else if (isLateNight && !ringsAwarded) {
                    profileUpdates.put("closedRingsStreak", 0);
                }

                // Сохраняем обновленные стрики
                if (!profileUpdates.isEmpty()) {
                    Tasks.await(userRef.child("profile").updateChildren(profileUpdates));
                }
                if (!dailyStreakUpdates.isEmpty()) {
                    Tasks.await(dailyRef.child("streaksAwarded").updateChildren(dailyStreakUpdates));
                }
            }

            Log.d("HealthSyncWorker", "Background sync & Streaks COMPLETED successfully!");
            return Result.success();

        } catch (Exception e) {
            Log.e("HealthSyncWorker", "Background sync failed", e);
            return Result.retry();
        }
    }
}