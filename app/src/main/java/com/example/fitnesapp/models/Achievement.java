package com.example.fitnesapp.models;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class Achievement {
    // Поля из Firebase
    public String id;
    public String category;
    public String type; // steps, calories, total_xp, collection
    public String title;
    public String description;
    public String tier;
    public int target;
    public int xpReward;
    public String icon;
    public String previousId; // Для цепочек (Tier system)

    // НОВОЕ ПОЛЕ: Список ID для коллекций
    public List<String> requiredIds = new ArrayList<>();

    // Динамические состояния
    public int currentProgress = 0;
    public boolean isCompleted = false;
    public boolean isCollected = false;
    public boolean isLocked = false;

    public String requiredTitle = ""; // Название требуемого задания (для Lock)
    public String subItemsStatus = ""; // Текст списка подзадач (для диалога)

    public String unlockedDate = "";

    public Achievement() {}

    // Хелпер для иконки
    public int getIconResId(Context context) {
        if (icon == null || icon.isEmpty()) return 0;
        return context.getResources().getIdentifier(icon, "drawable", context.getPackageName());
    }
}