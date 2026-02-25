package com.example.fitnesapp.models;

public class InsightItem {

    public enum Type {
        PRAISE,
        WARNING,
        TIP,
        PREDICTION
    }

    // НОВОЕ: Категории для фильтрации
    public enum Category {
        RECOVERY,   // Энергия / Батарейка
        NUTRITION,  // Питание / БЖУ
        ACTIVITY,   // Шаги / Тренировки
        SLEEP,      // Сон / Восстановление
        GENERAL     // Общие советы
    }

    public Type type;
    public Category category; // НОВОЕ ПОЛЕ
    public String title;
    public String description;

    // ОБНОВЛЕННЫЙ КОНСТРУКТОР
    public InsightItem(Type type, Category category, String title, String description) {
        this.type = type;
        this.category = category;
        this.title = title;
        this.description = description;
    }
}