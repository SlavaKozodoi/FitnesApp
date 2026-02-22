package com.example.fitnesapp.models;

public class InsightItem {
    public enum Type {
        WARNING,    // Красный: Предупреждения (переутомление, превышение калорий)
        TIP,        // Синий: Подсказки (пейте воду, ложитесь спать)
        PREDICTION, // Зеленый: ИИ-прогнозы (прогноз шагов, восстановление в норме)
        PRAISE      // Золотой: Похвала (цель выполнена, идеальное восстановление)
    }

    public Type type;
    public String title;
    public String message;

    public InsightItem(Type type, String title, String message) {
        this.type = type;
        this.title = title;
        this.message = message;
    }
}