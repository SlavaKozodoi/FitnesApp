package com.example.fitnesapp.ui.achievements;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.models.Achievement;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AchievementsViewModel extends ViewModel {

    private DatabaseReference dbRef;
    private String uid;

    private final MutableLiveData<List<Achievement>> allAchievementsList = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();

    private List<Achievement> catalog = new ArrayList<>();
    private DailyData currentDayData = new DailyData();
    private UserProfile currentProfile = new UserProfile();
    private List<String> collectedIds = new ArrayList<>();

    public AchievementsViewModel() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadCatalog();
            loadUserData();
        }
    }

    public LiveData<List<Achievement>> getAllAchievementsList() { return allAchievementsList; }
    public LiveData<UserProfile> getUserProfile() { return userProfile; }

    private void loadCatalog() {
        dbRef.child("all_achievements").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                catalog.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Achievement a = child.getValue(Achievement.class);
                    if (a != null) {
                        a.id = child.getKey();
                        if (a.requiredIds == null) a.requiredIds = new ArrayList<>();
                        catalog.add(a);
                    }
                }
                recalculate();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserData() {
        dbRef.child("users").child(uid).child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentProfile = snapshot.getValue(UserProfile.class);
                    userProfile.setValue(currentProfile);
                    recalculate();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbRef.child("users").child(uid).child("achievements_collection").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                collectedIds.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    collectedIds.add(child.getKey());
                }
                recalculate();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        dbRef.child("users").child(uid).child("daily_data").child(today).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentDayData = snapshot.getValue(DailyData.class);
                } else {
                    currentDayData = new DailyData();
                }
                recalculate();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void recalculate() {
        if (catalog.isEmpty()) return;

        Map<String, Achievement> processedMap = new HashMap<>();
        List<Achievement> tempList = new ArrayList<>();

        Map<String, String> titlesMap = new HashMap<>();
        for (Achievement a : catalog) {
            titlesMap.put(a.id, a.title);
        }

        for (Achievement template : catalog) {
            Achievement userAch = copyAchievement(template);

            if ("collection".equals(userAch.type)) {
                int foundCount = 0;
                StringBuilder statusBuilder = new StringBuilder();

                if (userAch.requiredIds != null) {
                    for (String reqId : userAch.requiredIds) {
                        String reqName = titlesMap.get(reqId);
                        if (reqName == null) reqName = reqId;

                        if (collectedIds.contains(reqId)) {
                            foundCount++;
                            statusBuilder.append("✅ ").append(reqName).append("\n");
                        } else {
                            statusBuilder.append("❌ ").append(reqName).append("\n");
                        }
                    }
                    userAch.target = userAch.requiredIds.size();
                }
                userAch.currentProgress = foundCount;
                userAch.subItemsStatus = statusBuilder.toString();
                userAch.isLocked = false;

            } else {
                if (currentProfile != null && currentDayData != null) {
                    switch (userAch.type) {
                        case "daily_steps": userAch.currentProgress = currentDayData.steps; break;
                        case "daily_calories": userAch.currentProgress = currentDayData.caloriesBurned; break;
                        case "daily_sleep_min": userAch.currentProgress = currentDayData.sleep != null ? currentDayData.sleep.durationMinutes : 0; break;
                        case "daily_nutrition": userAch.currentProgress = currentDayData.nutrition != null ? currentDayData.nutrition.totalCalories : 0; break;

                        // НОВАЯ ПРОВЕРКА ИДЕАЛЬНОГО ДНЯ (ЗАКРЫТЫ ЛИ 3 КОЛЬЦА?)
                        case "daily_closed_rings":
                            int rings = 0;
                            float cGoal = currentDayData.caloriesGoal > 0 ? currentDayData.caloriesGoal : 2000f;
                            float sGoal = currentDayData.stepsGoal > 0 ? currentDayData.stepsGoal : 10000f;
                            float nGoal = (currentDayData.nutrition != null && currentDayData.nutrition.maxCalories > 0) ? currentDayData.nutrition.maxCalories : 0f;

                            if (currentDayData.steps >= sGoal) rings++;
                            if (currentDayData.caloriesBurned >= cGoal) rings++;
                            if (nGoal > 0 && currentDayData.nutrition != null && currentDayData.nutrition.totalCalories >= nGoal) rings++;
                            else if (nGoal == 0) rings++; // Если цель питания не задана, считаем кольцо закрытым по умолчанию

                            userAch.currentProgress = (rings >= 3) ? 1 : 0;
                            break;

                        case "total_xp": userAch.currentProgress = currentProfile.totalXP; break;
                        case "total_steps": userAch.currentProgress = currentProfile.totalSteps; break;
                        case "total_workouts": userAch.currentProgress = currentProfile.totalWorkouts; break;
                        case "total_calories": userAch.currentProgress = currentProfile.totalCaloriesBurned; break;

                        // НОВЫЕ ТРЕКЕРЫ ЗДОРОВЬЯ
                        case "total_weight_logs": userAch.currentProgress = currentProfile.totalWeightLogs; break;
                        case "total_pulse_logs": userAch.currentProgress = currentProfile.totalPulseLogs; break;
                        case "total_oxygen_logs": userAch.currentProgress = currentProfile.totalOxygenLogs; break;

                        case "streak_steps": userAch.currentProgress = currentProfile.stepStreakDays; break;
                        case "perfect_sleeps": userAch.currentProgress = currentProfile.perfectSleepDays; break;
                        case "closed_rings_streak": userAch.currentProgress = currentProfile.closedRingsStreak; break;

                        default: userAch.currentProgress = 0; break;
                    }
                }
            }

            if (userAch.currentProgress >= userAch.target && userAch.target > 0) {
                userAch.currentProgress = userAch.target;
                userAch.isCompleted = true;
            }

            if (collectedIds.contains(userAch.id)) {
                userAch.isCollected = true;
                userAch.isCompleted = true;
                userAch.isLocked = false;
            }

            processedMap.put(userAch.id, userAch);
            tempList.add(userAch);
        }

        List<Achievement> resultList = new ArrayList<>();

        for (Achievement userAch : tempList) {
            if (!"collection".equals(userAch.type)) {
                if (userAch.previousId != null && !userAch.previousId.isEmpty()) {
                    Achievement prevAch = processedMap.get(userAch.previousId);

                    if (prevAch != null) {
                        boolean isPrevFinished = prevAch.isCompleted || prevAch.isCollected;

                        if (!isPrevFinished) {
                            userAch.isLocked = true;
                            userAch.currentProgress = 0;
                            userAch.requiredTitle = prevAch.title;
                        }
                    }
                }
            }

            if (userAch.isCollected) {
                userAch.isLocked = false;
            }

            resultList.add(userAch);
        }

        allAchievementsList.setValue(resultList);
    }

    private Achievement copyAchievement(Achievement src) {
        Achievement dest = new Achievement();
        dest.id = src.id;
        dest.category = src.category;
        dest.type = src.type;
        dest.title = src.title;
        dest.description = src.description;
        dest.tier = src.tier;
        dest.target = src.target;
        dest.xpReward = src.xpReward;
        dest.icon = src.icon;
        dest.previousId = src.previousId;
        if (src.requiredIds != null) dest.requiredIds = new ArrayList<>(src.requiredIds);
        return dest;
    }

    public void collectAchievement(Achievement achievement) {
        String date = new SimpleDateFormat("dd.MM.yy", Locale.US).format(new Date());

        dbRef.child("users").child(uid).child("achievements_collection")
                .child(achievement.id).child("unlockedDate").setValue(date);

        if (currentProfile != null) {
            dbRef.child("users").child(uid).child("profile")
                    .child("totalXP").setValue(currentProfile.totalXP + achievement.xpReward);
        }
    }
}