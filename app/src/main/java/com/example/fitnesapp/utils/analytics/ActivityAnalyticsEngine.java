package com.example.fitnesapp.utils.analytics;

import android.content.Context;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.InsightItem;

import java.util.List;

public class ActivityAnalyticsEngine {

    public static InsightItem generateWeeklyStepsInsight(List<Integer> weeklySteps, Context context) {
        // Если данных меньше чем за 3 дня, не анализируем
        if (weeklySteps == null || weeklySteps.size() < 3) {
            return null;
        }

        int totalSteps = 0;
        int maxSteps = Integer.MIN_VALUE;
        int minSteps = Integer.MAX_VALUE;

        for (int steps : weeklySteps) {
            totalSteps += steps;
            if (steps > maxSteps) maxSteps = steps;
            if (steps < minSteps) minSteps = steps;
        }

        int averageSteps = totalSteps / weeklySteps.size();

        // 1. НЕСТАБИЛЬНОСТЬ (Разрыв больше 8 000 шагов между минимумом и максимумом)
        if ((maxSteps - minSteps) > 8000 && minSteps > 0) {
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.ACTIVITY,
                    context.getString(R.string.analytics_steps_unstable_title),
                    context.getString(R.string.analytics_steps_unstable_desc, String.valueOf(minSteps), String.valueOf(maxSteps))
            );
        }

        // 2. ОТЛИЧНАЯ АКТИВНОСТЬ (В среднем больше 10 000)
        if (averageSteps >= 10000) {
            return new InsightItem(
                    InsightItem.Type.PRAISE,
                    InsightItem.Category.ACTIVITY,
                    context.getString(R.string.analytics_steps_high_title),
                    context.getString(R.string.analytics_steps_high_desc, String.valueOf(averageSteps))
            );
        }

        // 3. НИЗКАЯ АКТИВНОСТЬ (В среднем меньше 5 000)
        if (averageSteps < 5000 && averageSteps > 0) {
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.ACTIVITY,
                    context.getString(R.string.analytics_steps_low_title),
                    context.getString(R.string.analytics_steps_low_desc, String.valueOf(averageSteps))
            );
        }

        return null; // Если человек ходит стабильно 6k-9k шагов (это норма, ничего не выводим)
    }
}