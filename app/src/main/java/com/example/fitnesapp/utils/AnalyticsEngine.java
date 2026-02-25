package com.example.fitnesapp.utils;

import android.content.Context;

import com.example.fitnesapp.models.InsightItem;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.SleepStageItem; // Раскомментировал импорт
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

    public List<InsightItem> generate(DailyData data, UserProfile profile, UserGoals goals, List<DailyData> weekHistory) {
        List<InsightItem> insights = new ArrayList<>();

        if (data == null) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.GENERAL, "Нет данных", "Ждем загрузки данных за сегодня..."));
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

        generateMlInsights(data, profile, hour, insights);
        generateHabitInsights(data, currentHourFloat, hour, insights);
        generateMacroInsights(data, goalType, currentHourFloat, insights);
        generateRuleBasedInsights(data, hour, goalType, profile, insights);
        generateSleepInsights(data, weekHistory, insights);

        if (insights.isEmpty()) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.GENERAL, "Всё идет по плану", "День только начался. Продолжайте в том же духе!"));
        }

        return insights;
    }

    private void generateMlInsights(DailyData data, UserProfile profile, int hour, List<InsightItem> insights) {
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
            insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.RECOVERY,
                    "Энергия тела: " + percent + "%",
                    "Вы полны сил! Отличное время для интенсивной тренировки или пробежки. Заряд на максимуме."));
        } else if (batteryScore > 0.3f) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.RECOVERY,
                    "Энергия тела: " + percent + "%",
                    "Заряд постепенно снижается за счет активности. Нормальный уровень энергии для этого времени суток."));
        } else {
            if (hour >= 20) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.RECOVERY,
                        "День прожит не зря (" + percent + "%)",
                        "Отличный активный день! Ваша батарейка разряжена, самое время восстанавливать силы во сне."));
            } else {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.RECOVERY,
                        "Энергия на исходе: " + percent + "%",
                        "Ваша внутренняя батарейка садится. Избегайте тяжелых физических нагрузок и дайте себе перерыв."));
            }
        }
    }

    private void generateHabitInsights(DailyData data, float currentHourFloat, int hour, List<InsightItem> insights) {
        long steps = data.steps;
        long stepsGoal = data.stepsGoal;
        float cals = data.caloriesBurned;
        float calsGoal = data.caloriesGoal;

        float successProb = mlPredictor.predictHabitSuccess(currentHourFloat, steps, stepsGoal, cals, calsGoal);
        if (successProb < 0f) return;

        int percent = Math.round(successProb * 100);

        if (successProb >= 0.8f) {
            insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.ACTIVITY,
                    "Прогноз: Успех (" + percent + "%)",
                    "Вы идете с сильным опережением графика! Цели на сегодня будут легко достигнуты."));
        } else if (successProb >= 0.4f) {
            if (hour >= 12) {
                insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.ACTIVITY,
                        "Прогноз: В графике (" + percent + "%)",
                        "Шансы закрыть все цели высоки. Держите текущий темп и не забывайте двигаться."));
            }
        } else {
            if (hour >= 14) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.ACTIVITY,
                        "Прогноз: Отставание (" + percent + "%)",
                        "Вероятность выполнить цели падает. Время уходит, вам нужно добавить активности прямо сейчас!"));
            }
        }
    }

    private void generateMacroInsights(DailyData data, String goalType, float currentHourFloat, List<InsightItem> insights) {
        if (data.nutrition == null || data.nutrition.totalCalories <= 0) return;

        float calsEaten = data.nutrition.totalCalories;
        float calsGoal = data.nutrition.maxCalories > 0 ? data.nutrition.maxCalories : 2000f;
        float calsBurned = data.caloriesBurned;
        float burnGoal = 500f;

        float macroScore = mlPredictor.predictMacros(currentHourFloat, calsEaten, calsGoal, calsBurned, burnGoal);
        if (macroScore < 0f) return;

        if (macroScore >= 0.7f) {
            if (goalType.equals("lose")) {
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION, "Питание: Восполнение сил", "Вы потратили много энергии. Чтобы не замедлять метаболизм, сделайте легкий белковый перекус."));
            } else {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION, "Питание: Восполнение энергии", "Организм истощен активностью. Добавьте сложные углеводы (пасту, крупы) для восстановления."));
            }
        } else if (macroScore >= 0.4f) {
            insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.NUTRITION, "Питание: Отличный баланс", "Ваш рацион идеально совпадает с графиком дня и энергозатратами. Так держать!"));
        } else {
            if (goalType.equals("gain")) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.NUTRITION, "Питание: Анаболический фон", "Отличная загрузка калориями! Питание опережает траты — идеальная база для набора массы."));
            } else {
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION, "Питание: Запас энергии", "Вы очень плотно поели для этого времени суток. Постарайтесь больше двигаться сегодня, а следующий прием пищи сделать легким."));
            }
        }
    }

    private void generateRuleBasedInsights(DailyData data, int hour, String goalType, UserProfile profile, List<InsightItem> insights) {

        long steps = data.steps;
        long stepsGoal = data.stepsGoal > 0 ? data.stepsGoal : 10000;

        float weight = (profile != null && profile.weight > 0) ? profile.weight : 75f;

        float proteinMult = 1.8f;
        float fatMult = 1.0f;
        float carbsMult = 3.0f;

        if (goalType.equals("gain")) {
            proteinMult = 2.0f;
            fatMult = 1.1f;
            carbsMult = 4.5f;
        } else if (goalType.equals("lose")) {
            proteinMult = 2.2f;
            fatMult = 0.8f;
            carbsMult = 2.0f;
        }

        int targetProtein = Math.round(weight * proteinMult);
        int targetFat = Math.round(weight * fatMult);
        int targetCarbs = Math.round(weight * carbsMult);

        if (data.nutrition != null && data.nutrition.totalCalories > 0) {
            float totalCals = data.nutrition.totalCalories;
            float calsGoal = data.nutrition.maxCalories > 0 ? data.nutrition.maxCalories : 2000f;
            float protein = data.nutrition.protein;
            float carbs = data.nutrition.carbs;
            float fat = data.nutrition.fat;

            int proteinLeft = targetProtein - Math.round(protein);
            if (hour >= 15 && proteinLeft > 20) {
                int chickenAmount = Math.round((proteinLeft / 23f) * 100f);
                String whyText = goalType.equals("lose") ? "на сушке (чтобы не потерять мышцы)" : "для роста мышц";
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION,
                        "Белковая норма: " + Math.round(protein) + " / " + targetProtein + "г",
                        "Осталось добрать " + proteinLeft + "г белка " + whyText + ". Это примерно " + chickenAmount + "г куриного филе (или рыбы/творога)."));
            } else if (hour >= 18 && proteinLeft <= 10 && proteinLeft >= -30) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.NUTRITION,
                        "Идеальный белок",
                        "Вы набрали " + Math.round(protein) + "г белка! Мышцы получили 100% строительного материала."));
            }

            if (hour >= 18 && fat < targetFat) {
                int fatLeft = targetFat - Math.round(fat);
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        "Дефицит жиров: " + Math.round(fat) + " / " + targetFat + "г",
                        "Вам не хватает " + fatLeft + "г жиров. Добавьте орехи, сыр или ложку оливкового масла. Это критически важно для гормонального фона."));
            } else if (fat > targetFat + 20) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        "Перебор по жирам",
                        "Вы превысили вашу норму жиров на сегодня. Старайтесь выбирать более постные продукты на ужин."));
            }

            int carbsLeft = targetCarbs - Math.round(carbs);
            if (goalType.equals("gain") && hour >= 16 && carbsLeft > 50) {
                int buckwheatAmount = Math.round((carbsLeft / 65f) * 100f);
                insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.NUTRITION,
                        "Углеводная загрузка",
                        "Для набора массы не хватает " + carbsLeft + "г углеводов. Съешьте еще ~" + buckwheatAmount + "г крупы или макарон, иначе вес не будет расти."));
            } else if (goalType.equals("lose") && carbs > targetCarbs + 30) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                        "Лимит углеводов",
                        "Вы значительно превысили норму углеводов для похудения (" + Math.round(carbs) + " / " + targetCarbs + "г). Это заблокирует жиросжигание на сегодня."));
            }

            if (hour < 15 && totalCals > (calsGoal * 0.65f)) {
                int calsLeft = Math.round(calsGoal - totalCals);
                if (calsLeft > 0) {
                    insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.NUTRITION,
                            "Слишком быстрый старт",
                            "Вы съели " + Math.round(totalCals) + " ккал, хотя день только начался. До ночи остался жесткий лимит: всего " + calsLeft + " ккал. Планируйте легкий ужин."));
                }
            }
        }

        if (hour >= 15 && hour <= 18 && steps < (stepsGoal * 0.3f)) {
            insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.ACTIVITY,
                    "Засиделись",
                    "На часах уже " + hour + ":00, а пройдено всего " + steps + " шагов. Самое время размяться!"));
        }

        if (hour >= 19 && hour <= 22) {
            long stepsLeft = stepsGoal - steps;
            if (stepsLeft > 0 && stepsLeft <= 3000) {
                insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.ACTIVITY,
                        "Цель близко!",
                        "Осталось пройти ровно " + stepsLeft + " шагов до дневной нормы. Небольшая вечерняя прогулка поможет закрыть кольцо."));
            }
        }

        if (data.sleep != null && data.sleep.durationMinutes > 0) {
            long sleepMin = data.sleep.durationMinutes;
            if (sleepMin < 360 && steps > (stepsGoal * 0.8f)) {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.RECOVERY,
                        "Риск перетренированности",
                        "Вы прошли " + steps + " шагов на фоне всего " + (sleepMin / 60) + "ч сна. Это сильный стресс. Ложитесь сегодня до полуночи."));
            }
        }
    }

    private void generateSleepInsights(DailyData todayData, List<DailyData> weekHistory, List<InsightItem> insights) {
        if (todayData.sleep == null || todayData.sleep.durationMinutes == 0) return;

        long totalSleep = todayData.sleep.durationMinutes;

        long deepSleep = 0;
        long remSleep = 0;
        if (todayData.sleep.phases != null) {
            deepSleep = todayData.sleep.phases.deep;
            remSleep = todayData.sleep.phases.rem;
        }

        // Чистый и быстрый подсчет пробуждений по цифрам (1, 2, 3, 4)
        int awakenings = 0;
        if (todayData.sleep.hypnogram != null && !todayData.sleep.hypnogram.isEmpty()) {
            int previousStage = -1;
            for (SleepStageItem item : todayData.sleep.hypnogram.values()) {
                int currentStage = item.stage;
                // Если фаза 4 (Awake) и до этого мы спали
                if (currentStage == 4 && previousStage != 4 && previousStage != -1) {
                    awakenings++;
                }
                previousStage = currentStage;
            }
            if (awakenings > 0) awakenings--;
        }

        long totalWeekSleep = 0;
        int validDays = 0;
        if (weekHistory != null) {
            for (DailyData day : weekHistory) {
                if (day.sleep != null && day.sleep.durationMinutes > 0) {
                    totalWeekSleep += day.sleep.durationMinutes;
                    validDays++;
                }
            }
        }
        long avgHistory = validDays > 0 ? totalWeekSleep / validDays : 420;

        float mlSleepScore = mlPredictor.predictSleepQuality(totalSleep, deepSleep, remSleep, awakenings, avgHistory);

        if (mlSleepScore >= 0f) {
            int percent = Math.round(mlSleepScore * 100);

            if (mlSleepScore >= 0.8f) {
                insights.add(new InsightItem(InsightItem.Type.PRAISE, InsightItem.Category.SLEEP,
                        "Индекс сна: " + percent + "% (Отлично)",
                        "Идеальное сочетание фаз! Нервная система и мышцы полностью восстановились. Вы готовы к тяжелой тренировке."));
            } else if (mlSleepScore >= 0.5f) {
                insights.add(new InsightItem(InsightItem.Type.PREDICTION, InsightItem.Category.SLEEP,
                        "Индекс сна: " + percent + "% (Норма)",
                        "Допустимое качество сна. Базовое восстановление пройдено, вы готовы к рабочему дню."));
            } else {
                insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.SLEEP,
                        "Индекс сна: " + percent + "% (Плохо)",
                        "Критически низкое качество отдыха. Если сегодня есть тренировка, сделайте ее легкой, чтобы не перегрузить ЦНС."));
            }
        }

        float deepPercent = totalSleep > 0 ? (float) deepSleep / totalSleep : 0f;
        float remPercent = totalSleep > 0 ? (float) remSleep / totalSleep : 0f;

        if (deepPercent > 0 && deepPercent < 0.15f) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.SLEEP,
                    "Дефицит глубокой фазы",
                    "Ваша глубокая фаза составила всего " + Math.round(deepPercent * 100) + "%. Именно она восстанавливает мышцы после тяжелой работы. Охладите комнату перед сном."));
        }

        if (remPercent > 0 && remPercent < 0.20f) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.SLEEP,
                    "Дефицит REM-фазы",
                    "Нехватка быстрого сна (" + Math.round(remPercent * 100) + "%) снижает фокус внимания. Отложите телефон за час до сна."));
        }

        if (awakenings > 3) {
            insights.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.SLEEP,
                    "Фрагментированный сон",
                    "Судя по графику, вы просыпались " + awakenings + " раз. Проверьте уровень шума и света в спальне (используйте маску или плотные шторы)."));
        }

        if (todayData.sleep.fallingAsleepMin > 30) {
            insights.add(new InsightItem(InsightItem.Type.WARNING, InsightItem.Category.SLEEP,
                    "Долгое засыпание",
                    "Вы ворочались " + todayData.sleep.fallingAsleepMin + " минут прежде чем уснуть. Попробуйте техники дыхания или магний перед сном, чтобы успокоить нервную систему."));
        }
    }

    public void cleanup() {
        if (mlPredictor != null) {
            mlPredictor.close();
        }
    }
}