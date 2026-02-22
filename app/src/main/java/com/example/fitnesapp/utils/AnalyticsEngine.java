package com.example.fitnesapp.utils;

import android.content.Context;

import com.example.fitnesapp.models.InsightItem;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserGoals;
import com.example.fitnesapp.models.firebase.UserProfile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AnalyticsEngine {

    private MLPredictor mlPredictor;

    public AnalyticsEngine(Context context) {
        this.mlPredictor = new MLPredictor(context);
    }

    public List<InsightItem> generate(DailyData data, UserProfile profile, UserGoals goals) {
        List<InsightItem> insights = new ArrayList<>();

        if (data == null) {
            insights.add(new InsightItem(InsightItem.Type.TIP, "Нет данных", "Ждем загрузки данных за сегодня..."));
            return insights;
        }

        // Парсим цель из БД
        String goalType = "maintain";
        if (goals != null && goals.mainGoal != null) {
            String rawGoal = goals.mainGoal.toLowerCase();
            if (rawGoal.contains("gain")) {
                goalType = "gain";
            } else if (rawGoal.contains("lose") || rawGoal.contains("loss") || rawGoal.contains("cut")) {
                goalType = "lose";
            }
        }

        generateMlInsights(data, profile, insights);
        generateHabitInsights(data, insights);
        generateMacroInsights(data, goalType, insights);
        generateRuleBasedInsights(data, goalType, insights);

        if (insights.isEmpty()) {
            insights.add(new InsightItem(InsightItem.Type.TIP, "Всё идет по плану", "Продолжайте в том же духе!"));
        }

        return insights;
    }

    private void generateMlInsights(DailyData data, UserProfile profile, List<InsightItem> insights) {
        long sleep = data.sleep != null ? data.sleep.durationMinutes : 0;
        long steps = data.steps;
        float cals = data.caloriesBurned;
        long stepsGoal = data.stepsGoal;
        float calsGoal = data.caloriesGoal;

        int age = (profile != null) ? profile.getAge() : 0;
        float weight = (profile != null) ? profile.weight : 0;
        float height = (profile != null) ? profile.height : 0;

        float batteryScore = mlPredictor.predictRecovery(sleep, steps, cals, stepsGoal, calsGoal, age, weight, height);
        if (batteryScore < 0f) return;

        int percent = Math.round(batteryScore * 100);

        if (batteryScore > 0.7f) {
            insights.add(new InsightItem(InsightItem.Type.PRAISE,
                    "Энергия тела: " + percent + "%",
                    "Вы полны сил! Отличное время для интенсивной тренировки или пробежки. Заряд на максимуме."));
        } else if (batteryScore > 0.3f) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION,
                    "Энергия тела: " + percent + "%",
                    "Заряд постепенно снижается за счет активности. Нормальный уровень энергии для середины дня."));
        } else {
            insights.add(new InsightItem(InsightItem.Type.WARNING,
                    "Энергия на исходе: " + percent + "%",
                    "Ваша внутренняя батарейка садится. Избегайте тяжелых физических нагрузок и готовьтесь к отдыху."));
        }
    }

    private void generateHabitInsights(DailyData data, List<InsightItem> insights) {
        long steps = data.steps;
        float cals = data.caloriesBurned;
        long stepsGoal = data.stepsGoal;
        float calsGoal = data.caloriesGoal;

        Calendar calendar = Calendar.getInstance();
        float currentHourFloat = calendar.get(Calendar.HOUR_OF_DAY) + (calendar.get(Calendar.MINUTE) / 60.0f);

        float successProb = mlPredictor.predictHabitSuccess(currentHourFloat, steps, stepsGoal, cals, calsGoal);
        if (successProb < 0f) return;

        int percent = Math.round(successProb * 100);

        if (successProb >= 0.8f) {
            insights.add(new InsightItem(InsightItem.Type.PRAISE,
                    "Прогноз: Успех (" + percent + "%)",
                    "Вы идете с сильным опережением графика! Цели на сегодня будут легко достигнуты."));
        } else if (successProb >= 0.4f) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION,
                    "Прогноз: В графике (" + percent + "%)",
                    "Шансы закрыть все цели высоки. Держите текущий темп и не забывайте двигаться."));
        } else {
            insights.add(new InsightItem(InsightItem.Type.WARNING,
                    "Прогноз: Отставание (" + percent + "%)",
                    "Вероятность выполнить цели падает. Время суток уходит, вам нужно добавить активности прямо сейчас!"));
        }
    }

    private void generateMacroInsights(DailyData data, String goalType, List<InsightItem> insights) {
        if (data.nutrition == null) return;

        float calsEaten = data.nutrition.totalCalories;
        float calsGoal = data.nutrition.maxCalories;
        float calsBurned = data.caloriesBurned;
        float burnGoal = 500f;

        Calendar calendar = Calendar.getInstance();
        float currentHourFloat = calendar.get(Calendar.HOUR_OF_DAY) + (calendar.get(Calendar.MINUTE) / 60.0f);

        float macroScore = mlPredictor.predictMacros(currentHourFloat, calsEaten, calsGoal, calsBurned, burnGoal);
        if (macroScore < 0f) return;

        if (macroScore >= 0.7f) {
            if (goalType.equals("lose")) {
                insights.add(new InsightItem(InsightItem.Type.TIP, "Питание: Восполнение сил", "Вы потратили много энергии. Чтобы не замедлять метаболизм, сделайте легкий белковый перекус."));
            } else {
                insights.add(new InsightItem(InsightItem.Type.WARNING, "Питание: Восполнение энергии", "Организм истощен активностью. Добавьте сложные углеводы (пасту, крупы) для восстановления."));
            }
        } else if (macroScore >= 0.4f) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION, "Питание: Отличный баланс", "Ваш рацион отлично покрывает энергозатраты на движение. Придерживайтесь плана."));
        } else {
            if (goalType.equals("gain")) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, "Питание: Анаболический фон", "Питание опережает энергозатраты. Отличная база для качественного набора массы!"));
            } else {
                insights.add(new InsightItem(InsightItem.Type.TIP, "Питание: Легкий рацион", "Потребление калорий слегка опережает физическую активность. Сделайте ужин более легким."));
            }
        }
    }

    private void generateRuleBasedInsights(DailyData data, String goalType, List<InsightItem> insights) {
        if (data.nutrition != null && data.nutrition.maxCalories > 0) {
            float leftCals = data.nutrition.maxCalories - data.nutrition.totalCalories;

            if (leftCals < 0) {
                int excessCals = Math.abs((int)leftCals);

                if (goalType.equals("gain")) {
                    insights.add(new InsightItem(InsightItem.Type.PRAISE,
                            "Профицит достигнут",
                            "Отличная работа! Вы в плюсе на " + excessCals + " ккал. То, что нужно для качественного роста мышечной массы."));
                } else {
                    int extraSteps = excessCals * 25;
                    insights.add(new InsightItem(InsightItem.Type.WARNING,
                            "Лимит калорий превышен",
                            "Вы перебрали " + excessCals + " ккал. Постарайтесь сделать на " + extraSteps + " шагов больше обычной цели, чтобы легко сжечь этот излишек."));
                }
            } else if (leftCals > 500) {
                if (goalType.equals("gain")) {
                    insights.add(new InsightItem(InsightItem.Type.WARNING,
                            "Риск потери массы",
                            "Вам не хватает еще " + (int)leftCals + " ккал до дневной нормы. Обязательно добавьте плотный прием пищи!"));
                } else if (goalType.equals("lose")) {
                    insights.add(new InsightItem(InsightItem.Type.PRAISE,
                            "Идеальный дефицит",
                            "У вас в запасе еще " + (int)leftCals + " ккал. Вы идете в отличном темпе для похудения!"));
                }
            }
        }

        if (data.sleep != null && data.sleep.durationMinutes > 0 && data.sleep.durationMinutes < 360) {
            insights.add(new InsightItem(InsightItem.Type.TIP, "Недосып", "Вы спали менее 6 часов. Постарайтесь сегодня лечь пораньше."));
        }
    }

    public void cleanup() {
        if (mlPredictor != null) {
            mlPredictor.close();
        }
    }
}