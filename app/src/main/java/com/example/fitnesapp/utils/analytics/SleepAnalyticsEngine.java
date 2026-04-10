package com.example.fitnesapp.utils.analytics;

import android.content.Context;
import com.example.fitnesapp.R;
import com.example.fitnesapp.models.InsightItem;
import java.util.List;

public class SleepAnalyticsEngine {

    public static InsightItem generateWeeklySleepInsight(List<Integer> weeklySleepMinutes, Context context) {
        // Якщо даних менше ніж за 4 дні, аналіз буде неточним
        if (weeklySleepMinutes == null || weeklySleepMinutes.size() < 4) {
            return null;
        }

        int totalMinutes = 0;
        int maxSleep = Integer.MIN_VALUE;
        int minSleep = Integer.MAX_VALUE;

        for (int mins : weeklySleepMinutes) {
            if (mins <= 0) continue; // Ігноруємо порожні дні
            totalMinutes += mins;
            if (mins > maxSleep) maxSleep = mins;
            if (mins < minSleep) minSleep = mins;
        }

        int averageMinutes = totalMinutes / weeklySleepMinutes.size();
        String avgTimeFormatted = formatTime(averageMinutes,context);

        // 1. ПЕРЕВІРКА НА НЕСТАБІЛЬНІСТЬ (ДЖЕТЛАГ)
        // Якщо різниця між найдовшим і найкоротшим сном більше 3 годин (180 хв)
        if ((maxSleep - minSleep) > 180 && averageMinutes > 0) {
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_unstable_title),
                    context.getString(R.string.analytics_sleep_unstable_desc)
            );
        }

        // 2. ПЕРЕВІРКА НА НЕДОСИП (Менше 6.5 годин в середньому)
        if (averageMinutes > 0 && averageMinutes < 390) { // 390 хв = 6.5 годин
            return new InsightItem(
                    InsightItem.Type.WARNING,
                    InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_debt_title),
                    String.format(context.getString(R.string.analytics_sleep_debt_desc), avgTimeFormatted)
            );
        }

        // 3. ПЕРЕВІРКА НА ПЕРЕСИПАННЯ (Більше 9 годин в середньому)
        if (averageMinutes > 540) { // 540 хв = 9 годин
            return new InsightItem(
                    InsightItem.Type.TIP,
                    InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_oversleep_title),
                    String.format(context.getString(R.string.analytics_sleep_oversleep_desc), avgTimeFormatted)
            );
        }

        // 4. ІДЕАЛЬНИЙ СОН (7 - 8.5 годин в середньому)
        if (averageMinutes >= 420 && averageMinutes <= 510) {
            return new InsightItem(
                    InsightItem.Type.PRAISE,
                    InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_perfect_week_title),
                    String.format(context.getString(R.string.analytics_sleep_perfect_week_desc), avgTimeFormatted)
            );
        }

        return null; // Якщо нічого особливого не знайдено
    }

    // Допоміжний метод для форматування (наприклад, 450 хв -> "7г 30хв")
    private static String formatTime(int totalMinutes,Context context) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        return context.getString(R.string.format_sleep_time, hours, mins);

    }
}
