package com.example.fitnesapp.utils.analytics;

import android.content.Context;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.InsightItem;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.SleepStageItem;
import com.example.fitnesapp.models.firebase.UserGoals;
import com.example.fitnesapp.models.firebase.UserProfile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AnalyticsEngine {

    // ИЗМЕНЕНИЕ: Сохраняем контекст для доступа к ресурсам
    private final Context context;

    public AnalyticsEngine(Context context) {
        this.context = context;
    }

    public List<InsightItem> generate(DailyData data, UserProfile profile, UserGoals goals, List<DailyData> history) {
        List<InsightItem> insights = new ArrayList<>();

        if (data == null) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.GENERAL,
                    context.getString(R.string.analytics_no_data_title),
                    context.getString(R.string.analytics_no_data_desc)));
            return insights;
        }

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        float currentHourFloat = hour + (calendar.get(Calendar.MINUTE) / 60.0f);

        String goalType = "maintain";
        if (goals != null && goals.mainGoal != null) {
            String rawGoal = goals.mainGoal.toLowerCase();
            if (rawGoal.contains("gain")) {
                goalType = "gain";
            } else if (rawGoal.contains("lose") || rawGoal.contains("loss") || rawGoal.contains("cut")) {
                goalType = "lose";
            }
        }

        generateEnergyInsights(data, hour, insights);
        generateHabitInsights(data, hour, insights);
        generateMacroInsights(data, goalType, currentHourFloat, insights);
        generateRuleBasedInsights(data, hour, goalType, profile, insights);
        generateSleepInsights(data, insights);

        if (insights.isEmpty()) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.GENERAL,
                    context.getString(R.string.analytics_all_good_title),
                    context.getString(R.string.analytics_all_good_desc)));
        }

        /// ==========================================
        // ГЛУБОКАЯ АНАЛИТИКА ЗА НЕДЕЛЮ (Сон, Шаги, Питание)
        // ==========================================
        if (history != null && !history.isEmpty()) {
            List<Integer> sleepHistoryMinutes = new ArrayList<>();
            List<Integer> stepsHistory = new ArrayList<>();
            List<Integer> nutritionHistory = new ArrayList<>(); // <-- Создали список для еды

            for (DailyData pastDay : history) {
                // Сон
                if (pastDay.sleep != null && pastDay.sleep.durationMinutes > 0) {
                    sleepHistoryMinutes.add(pastDay.sleep.durationMinutes);
                }
                // Шаги
                if (pastDay.steps > 0) {
                    stepsHistory.add((int) pastDay.steps);
                }
                // Питание
                if (pastDay.nutrition != null) {
                    nutritionHistory.add((int) pastDay.nutrition.totalCalories);
                } else {
                    nutritionHistory.add(0);
                }
            }

            // Генерируем подсказку по питанию (передаем историю и ЦЕЛИ пользователя)
            InsightItem weeklyNutrInsight = NutritionAnalyticsEngine.generateWeeklyNutritionInsight(nutritionHistory, goals, context);
            if (weeklyNutrInsight != null) {
                insights.add(weeklyNutrInsight);
            }

            // Генерируем подсказку по сну
            InsightItem weeklySleepInsight = SleepAnalyticsEngine.generateWeeklySleepInsight(sleepHistoryMinutes, context);
            if (weeklySleepInsight != null) {
                insights.add(weeklySleepInsight);
            }

            // Генерируем подсказку по шагам
            InsightItem weeklyStepsInsight = ActivityAnalyticsEngine.generateWeeklyStepsInsight(stepsHistory, context);
            if (weeklyStepsInsight != null) {
                insights.add(weeklyStepsInsight);
            }
        }

        return insights;
    }



    // ==========================================
    // ГЛОБАЛЬНЫЙ КАЛЬКУЛЯТОР СНА
    // ==========================================
    // ИЗМЕНЕНИЕ: Убрали static, чтобы использовать context
    public void evaluateSleep(DailyData.Sleep sleep) {
        if (sleep == null || sleep.durationMinutes <= 0) return;

        long totalSleep = sleep.durationMinutes;
        long deepSleep = sleep.phases != null ? sleep.phases.deep : 0;
        long remSleep = sleep.phases != null ? sleep.phases.rem : 0;
        int fallingAsleep = sleep.fallingAsleepMin;

        int awakenings = 0;
        if (sleep.hypnogram != null && !sleep.hypnogram.isEmpty()) {
            int previousStage = -1;
            for (SleepStageItem item : sleep.hypnogram.values()) {
                if (item.stage == 4 && previousStage != 4 && previousStage != -1) {
                    awakenings++;
                }
                previousStage = item.stage;
            }
            if (awakenings > 0) awakenings--;
        }

        float durationScore = 0f;
        if (totalSleep >= 420 && totalSleep <= 540) {
            durationScore = 1.0f;
        } else if (totalSleep < 420) {
            durationScore = Math.max(0f, (totalSleep - 180f) / 240f);
        } else {
            float extraMinutes = totalSleep - 540f;
            durationScore = 1.0f - (extraMinutes / 600f);
            durationScore = Math.max(0.4f, durationScore);
        }

        float wakePenalty = Math.min((float) awakenings * 0.05f, 0.4f);
        float fallingAsleepPenalty = 0f;
        if (fallingAsleep > 30) {
            fallingAsleepPenalty = Math.min((fallingAsleep - 30) * 0.01f, 0.2f);
        }

        float sleepQuality = 0f;
        boolean hasPhases = (deepSleep > 0 || remSleep > 0);

        if (hasPhases) {
            float deepPercent = (float) deepSleep / totalSleep;
            float remPercent = (float) remSleep / totalSleep;
            float deepScore = Math.min(deepPercent / 0.20f, 1.0f);
            float remScore = Math.min(remPercent / 0.25f, 1.0f);
            sleepQuality = (durationScore * 0.4f) + (deepScore * 0.3f) + (remScore * 0.3f) - wakePenalty - fallingAsleepPenalty;
        } else {
            sleepQuality = durationScore - wakePenalty - fallingAsleepPenalty;
        }

        sleepQuality = Math.max(0.1f, Math.min(sleepQuality, 1.0f));

        sleep.score = Math.round(sleepQuality * 100);

        if (sleepQuality >= 0.8f) {
            sleep.quality = context.getString(R.string.sleep_quality_excellent);
        } else if (sleepQuality >= 0.5f) {
            sleep.quality = context.getString(R.string.sleep_quality_normal);
        } else {
            sleep.quality = context.getString(R.string.sleep_quality_bad);
        }
    }

    // ==========================================
    // 1. АНАЛИЗ ЭНЕРГИИ
    // ==========================================
    private void generateEnergyInsights(DailyData data, int hour, List<InsightItem> insights) {
        long sleep = data.sleep != null ? data.sleep.durationMinutes : 0;
        long steps = data.steps;
        float cals = data.caloriesBurned;

        float sleepFactor = Math.min((float) sleep / 480f, 1.0f);
        float stepFactor = data.stepsGoal > 0 ? Math.min((float) steps / data.stepsGoal, 1.0f) : 0f;
        float calFactor = data.caloriesGoal > 0 ? Math.min(cals / data.caloriesGoal, 1.0f) : 0f;
        float activityFatigue = (stepFactor + calFactor) / 2.0f;

        float batteryScore = sleepFactor - (activityFatigue * 0.5f);
        batteryScore = Math.max(0.1f, Math.min(batteryScore, 1.0f));

        int percent = Math.round(batteryScore * 100);

        if (batteryScore > 0.7f) {
            insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.RECOVERY,
                    context.getString(R.string.analytics_energy_title, percent),
                    context.getString(R.string.analytics_energy_high_desc)));
        } else if (batteryScore > 0.3f) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.RECOVERY,
                    context.getString(R.string.analytics_energy_title, percent),
                    context.getString(R.string.analytics_energy_med_desc)));
        } else {
            if (hour >= 20) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.RECOVERY,
                        context.getString(R.string.analytics_energy_done_title, percent),
                        context.getString(R.string.analytics_energy_done_desc)));
            } else {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.RECOVERY,
                        context.getString(R.string.analytics_energy_low_title, percent),
                        context.getString(R.string.analytics_energy_low_desc)));
            }
        }
    }

    // ==========================================
    // 2. АНАЛИЗ ПРИВЫЧЕК
    // ==========================================
    private void generateHabitInsights(DailyData data, int hour, List<InsightItem> insights) {
        long steps = data.steps;
        long stepsGoal = data.stepsGoal > 0 ? data.stepsGoal : 10000;
        float cals = data.caloriesBurned;
        float calsGoal = data.caloriesGoal > 0 ? data.caloriesGoal : 500f;

        float stepProgress = Math.min((float) steps / stepsGoal, 1.0f);
        float calProgress = Math.min(cals / calsGoal, 1.0f);
        float totalProgress = (stepProgress + calProgress) / 2.0f;

        float expectedProgress = Math.max(0f, Math.min((float) (hour - 8) / 14f, 1.0f));

        if (totalProgress >= expectedProgress + 0.2f) {
            insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.ACTIVITY,
                    context.getString(R.string.analytics_habit_ahead_title),
                    context.getString(R.string.analytics_habit_ahead_desc)));
        } else if (totalProgress >= expectedProgress - 0.1f) {
            if (hour >= 12 && totalProgress < 1.0f) {
                insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.ACTIVITY,
                        context.getString(R.string.analytics_habit_on_track_title),
                        context.getString(R.string.analytics_habit_on_track_desc)));
            }
        } else {
            if (hour >= 14 && totalProgress < 1.0f) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.ACTIVITY,
                        context.getString(R.string.analytics_habit_behind_title),
                        context.getString(R.string.analytics_habit_behind_desc)));
            }
        }
    }

    // ==========================================
    // 3. АНАЛИЗ МАКРОНУТРИЕНТОВ
    // ==========================================
    private void generateMacroInsights(DailyData data, String goalType, float currentHourFloat, List<InsightItem> insights) {
        if (data.nutrition == null || data.nutrition.totalCalories <= 0) return;

        float calsEaten = data.nutrition.totalCalories;
        float calsGoal = data.nutrition.maxCalories > 0 ? data.nutrition.maxCalories : 2000f;
        float expectedFoodPercent = Math.max(0.1f, Math.min((currentHourFloat - 8f) / 14f, 1.0f));
        float expectedCalories = calsGoal * expectedFoodPercent;

        if (calsEaten < expectedCalories * 0.7f) {
            if (goalType.equals("lose")) {
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_macro_refill_title),
                        context.getString(R.string.analytics_macro_refill_desc)));
            } else {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_macro_energy_title),
                        context.getString(R.string.analytics_macro_energy_desc)));
            }
        } else if (calsEaten > expectedCalories * 1.3f) {
            if (goalType.equals("gain")) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_macro_anabolic_title),
                        context.getString(R.string.analytics_macro_anabolic_desc)));
            } else {
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_macro_overeat_title),
                        context.getString(R.string.analytics_macro_overeat_desc)));
            }
        } else {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.NUTRITION,
                    context.getString(R.string.analytics_macro_perfect_title),
                    context.getString(R.string.analytics_macro_perfect_desc)));
        }
    }

    // ==========================================
    // 4. ЭКСПЕРТНАЯ СИСТЕМА
    // ==========================================
    private void generateRuleBasedInsights(DailyData data, int hour, String goalType, UserProfile profile, List<InsightItem> insights) {
        long steps = data.steps;
        long stepsGoal = data.stepsGoal > 0 ? data.stepsGoal : 10000;

        if (data.nutrition != null && data.nutrition.totalCalories > 0) {
            float totalCals = data.nutrition.totalCalories;
            float calsGoal = data.nutrition.maxCalories > 0 ? data.nutrition.maxCalories : 2000f;

            float proteinPct = 0.30f; float fatPct = 0.30f; float carbsPct = 0.40f;

            if (goalType.equals("gain")) {
                proteinPct = 0.25f;
                fatPct = 0.25f;
                carbsPct = 0.50f;
            } else if (goalType.equals("lose")) {
                proteinPct = 0.40f;
                fatPct = 0.25f;
                carbsPct = 0.35f;
            }

            int targetProtein = Math.round((calsGoal * proteinPct) / 4f);
            int targetFat = Math.round((calsGoal * fatPct) / 9f);
            int targetCarbs = Math.round((calsGoal * carbsPct) / 4f);

            float protein = data.nutrition.protein;
            float carbs = data.nutrition.carbs;
            float fat = data.nutrition.fat;

            int proteinLeft = targetProtein - Math.round(protein);
            if (hour >= 15 && proteinLeft > 20) {
                int chickenAmount = Math.round((proteinLeft / 23f) * 100f);
                String whyText = goalType.equals("lose") ?
                        context.getString(R.string.analytics_why_lose) :
                        context.getString(R.string.analytics_why_gain);

                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_protein_norm_title, Math.round(protein), targetProtein),
                        context.getString(R.string.analytics_protein_left_desc, proteinLeft, whyText, chickenAmount)));
            } else if (hour >= 18 && proteinLeft <= 10 && proteinLeft >= -30) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_protein_perfect_title),
                        context.getString(R.string.analytics_protein_perfect_desc, Math.round(protein))));
            }

            if (hour >= 18 && fat < targetFat) {
                int fatLeft = targetFat - Math.round(fat);
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_fat_deficit_title, Math.round(fat), targetFat),
                        context.getString(R.string.analytics_fat_deficit_desc, fatLeft)));
            } else if (fat > targetFat + 20) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_fat_excess_title),
                        context.getString(R.string.analytics_fat_excess_desc)));
            }

            int carbsLeft = targetCarbs - Math.round(carbs);
            if (goalType.equals("gain") && hour >= 16 && carbsLeft > 50) {
                int buckwheatAmount = Math.round((carbsLeft / 65f) * 100f);
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_carb_load_title),
                        context.getString(R.string.analytics_carb_load_desc, carbsLeft, buckwheatAmount)));
            } else if (goalType.equals("lose") && carbs > targetCarbs + 30) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        context.getString(R.string.analytics_carb_limit_title),
                        context.getString(R.string.analytics_carb_limit_desc, Math.round(carbs), targetCarbs)));
            }

            if (hour < 15 && totalCals > (calsGoal * 0.65f)) {
                int calsLeft = Math.round(calsGoal - totalCals);
                if (calsLeft > 0) {
                    insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                            context.getString(R.string.analytics_fast_start_title),
                            context.getString(R.string.analytics_fast_start_desc, Math.round(totalCals), calsLeft)));
                }
            }
        }

        if (hour >= 15 && hour <= 18 && steps < (stepsGoal * 0.3f)) {
            insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.ACTIVITY,
                    context.getString(R.string.analytics_sat_too_long_title),
                    context.getString(R.string.analytics_sat_too_long_desc, hour, steps)));
        }

        if (hour >= 19 && hour <= 22) {
            long stepsLeft = stepsGoal - steps;
            if (stepsLeft > 0 && stepsLeft <= 3000) {
                insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.ACTIVITY,
                        context.getString(R.string.analytics_goal_close_title),
                        context.getString(R.string.analytics_goal_close_desc, stepsLeft)));
            }
        }

        if (data.sleep != null && data.sleep.durationMinutes > 0) {
            long sleepMin = data.sleep.durationMinutes;
            if (sleepMin < 360 && steps > (stepsGoal * 0.8f)) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.RECOVERY,
                        context.getString(R.string.analytics_overtrain_risk_title),
                        context.getString(R.string.analytics_overtrain_risk_desc, steps, (sleepMin / 60))));
            }
        }
    }

    // ==========================================
    // 5. ОЦЕНКА СНА ДЛЯ ПОДСКАЗОК
    // ==========================================
    private void generateSleepInsights(DailyData todayData, List<InsightItem> insights) {
        if (todayData.sleep == null || todayData.sleep.durationMinutes == 0) return;

        evaluateSleep(todayData.sleep);

        int percent = todayData.sleep.score;
        String quality = todayData.sleep.quality;

        String qualityExcellent = context.getString(R.string.sleep_quality_excellent);
        String qualityNormal = context.getString(R.string.sleep_quality_normal);

        if (qualityExcellent.equals(quality)) {
            insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_index_title, percent, quality),
                    context.getString(R.string.analytics_sleep_exc_desc)));
        } else if (qualityNormal.equals(quality)) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_index_title, percent, quality),
                    context.getString(R.string.analytics_sleep_norm_desc)));
        } else {
            insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_index_title, percent, quality),
                    context.getString(R.string.analytics_sleep_bad_desc)));
        }

        long totalSleep = todayData.sleep.durationMinutes;
        long deepSleep = todayData.sleep.phases != null ? todayData.sleep.phases.deep : 0;
        long remSleep = todayData.sleep.phases != null ? todayData.sleep.phases.rem : 0;

        float deepPercent = totalSleep > 0 ? (float) deepSleep / totalSleep : 0f;
        float remPercent = totalSleep > 0 ? (float) remSleep / totalSleep : 0f;

        if (deepPercent > 0 && deepPercent < 0.15f) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_deep_deficit_title),
                    context.getString(R.string.analytics_sleep_deep_deficit_desc, Math.round(deepPercent * 100))));
        }

        if (remPercent > 0 && remPercent < 0.20f) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_rem_deficit_title),
                    context.getString(R.string.analytics_sleep_rem_deficit_desc, Math.round(remPercent * 100))));
        }

        int awakenings = 0;
        if (todayData.sleep.hypnogram != null && !todayData.sleep.hypnogram.isEmpty()) {
            int previousStage = -1;
            for (SleepStageItem item : todayData.sleep.hypnogram.values()) {
                if (item.stage == 4 && previousStage != 4 && previousStage != -1) awakenings++;
                previousStage = item.stage;
            }
            if (awakenings > 0) awakenings--;
        }

        if (awakenings > 3) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.SLEEP,
                    context.getString(R.string.analytics_sleep_frag_title),
                    context.getString(R.string.analytics_sleep_frag_desc, awakenings)));
        }
    }

    public void cleanup() {
    }
}