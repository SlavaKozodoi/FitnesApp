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
        // Профиль
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

        // Коллекция
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

        // Данные за сегодня
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

        // Временная карта для быстрого доступа к объектам по ID
        Map<String, Achievement> processedMap = new HashMap<>();
        List<Achievement> tempList = new ArrayList<>();

        // Справочник названий для отображения требований (Req: Title)
        Map<String, String> titlesMap = new HashMap<>();
        for (Achievement a : catalog) {
            titlesMap.put(a.id, a.title);
        }

        // ==========================================
        // ЭТАП 1: Расчет прогресса для ВСЕХ заданий
        // ==========================================
        for (Achievement template : catalog) {
            Achievement userAch = copyAchievement(template);

            // 1. Расчет Коллекций (Meta-Achievements)
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
                // 2. Расчет Обычных заданий (Steps, XP, Cal)
                // Сначала считаем прогресс БЕЗ учета блокировки
                if ("steps".equals(userAch.type)) userAch.currentProgress = currentDayData.steps;
                else if ("calories".equals(userAch.type)) userAch.currentProgress = currentDayData.caloriesBurned;
                else if ("total_xp".equals(userAch.type)) userAch.currentProgress = (currentProfile != null) ? currentProfile.totalXP : 0;
            }

            // Проверка достижения цели
            if (userAch.currentProgress >= userAch.target && userAch.target > 0) {
                userAch.currentProgress = userAch.target;
                userAch.isCompleted = true; // Задание выполнено физически
            }

            // Проверка наличия в коллекции (уже забрали награду)
            if (collectedIds.contains(userAch.id)) {
                userAch.isCollected = true;
                userAch.isCompleted = true;
                userAch.isLocked = false;
            }

            // Сохраняем во временные хранилища
            processedMap.put(userAch.id, userAch);
            tempList.add(userAch);
        }

        // ==========================================
        // ЭТАП 2: Проверка зависимостей (Цепочки)
        // ==========================================
        List<Achievement> resultList = new ArrayList<>();

        for (Achievement userAch : tempList) {
            // Если это не коллекция (коллекции не блокируются)
            if (!"collection".equals(userAch.type)) {

                // Если есть требование предыдущего уровня
                if (userAch.previousId != null && !userAch.previousId.isEmpty()) {

                    // Находим предыдущее задание в уже рассчитанном списке
                    Achievement prevAch = processedMap.get(userAch.previousId);

                    if (prevAch != null) {
                        // ГЛАВНОЕ ИЗМЕНЕНИЕ:
                        // Разблокируем, если предыдущее ВЫПОЛНЕНО (isCompleted) ИЛИ СОБРАНО (isCollected)
                        boolean isPrevFinished = prevAch.isCompleted || prevAch.isCollected;

                        if (!isPrevFinished) {
                            // Блокируем текущее
                            userAch.isLocked = true;
                            userAch.currentProgress = 0; // Скрываем прогресс

                            // Устанавливаем название требования
                            userAch.requiredTitle = prevAch.title;
                        }
                    }
                }
            }

            // Защита: если само задание уже собрано, оно точно не заблокировано
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

        if (src.requiredIds != null) {
            dest.requiredIds = new ArrayList<>(src.requiredIds);
        }
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