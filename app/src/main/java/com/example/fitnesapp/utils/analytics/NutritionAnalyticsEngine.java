package com.example.fitnesapp.utils.analytics;

import android.content.Context;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.InsightItem;
import com.example.fitnesapp.models.firebase.UserGoals;

import java.util.List;

public class NutritionAnalyticsEngine {

    public static InsightItem generateWeeklyNutritionInsight(List<Integer> weeklyCalories, UserGoals goals, Context context) {
        // Если у пользователя не настроены цели, мы не можем дать правильный совет
        if (goals == null || goals.dailyCalories <= 0 || weeklyCalories == null) {
            return null;
        }

        int totalCalories = 0;
        int validDaysCount = 0;

        // ФИЛЬТРАЦИЯ ЛЕНИВЫХ ДНЕЙ
        for (int cals : weeklyCalories) {
            // Если записано меньше 300 ккал, скорее всего человек просто забыл внести данные. Игнорируем этот день.
            if (cals > 300) {
                totalCalories += cals;
                validDaysCount++;
            }
        }

        // Если человек вел дневник питания меньше 3 дней за неделю, данных для анализа недостаточно
        if (validDaysCount < 3) {
            return null;
        }

        int averageCalories = totalCalories / validDaysCount;
        int target = goals.dailyCalories;
        String goalType = goals.mainGoal != null ? goals.mainGoal.toLowerCase() : "";
        String avgFormatted = String.valueOf(averageCalories);

        // 1. ОПАСНЫЙ ДЕФИЦИТ (Если человек ест меньше 60% от своей нормы)
        // Это актуально для любой цели, так как это ведет к срывам и потере мышц
        if (averageCalories < (target * 0.6)) {
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.NUTRITION,
                    context.getString(R.string.analytics_nutr_starvation_title),
                    context.getString(R.string.analytics_nutr_starvation_desc, avgFormatted)
            );
        }

        // 2. ЦЕЛЬ: ПОХУДЕНИЕ (Перебор с калориями)
        if (goalType.contains("lose") && averageCalories > (target + 250)) {
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.NUTRITION,
                    context.getString(R.string.analytics_nutr_over_cut_title),
                    context.getString(R.string.analytics_nutr_over_cut_desc, avgFormatted)
            );
        }

        // 3. ЦЕЛЬ: НАБОР МАССЫ (Недобор калорий)
        if (goalType.contains("gain") && averageCalories < (target - 250)) {
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.NUTRITION,
                    context.getString(R.string.analytics_nutr_under_bulk_title),
                    context.getString(R.string.analytics_nutr_under_bulk_desc, avgFormatted)
            );
        }

        // 4. ИДЕАЛЬНОЕ ПОПАДАНИЕ (В пределах +/- 200 ккал от цели)
        if (Math.abs(averageCalories - target) <= 200) {
            return new InsightItem(
                    InsightItem.Type.PRAISE,
                    InsightItem.Category.NUTRITION,
                    context.getString(R.string.analytics_nutr_perfect_title),
                    context.getString(R.string.analytics_nutr_perfect_desc, avgFormatted)
            );
        }

        return null;
    }
}