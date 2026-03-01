package com.example.fitnesapp.models;

import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.WorkoutItem;
import java.util.List;

public class WorkoutSessionUI {
    public WorkoutItem workout;
    public List<HealthLogItem> pulse;
    public List<HealthLogItem> pace;
    public List<HealthLogItem> oxygen;

    // НОВОЕ: Список умных советов
    public List<WorkoutAdvice> advices;

    public WorkoutSessionUI(WorkoutItem workout, List<HealthLogItem> pulse, List<HealthLogItem> pace, List<HealthLogItem> oxygen, List<WorkoutAdvice> advices) {
        this.workout = workout;
        this.pulse = pulse;
        this.pace = pace;
        this.oxygen = oxygen;
        this.advices = advices;
    }

    // Вложенный класс для карточки совета
    public static class WorkoutAdvice {
        public String icon;        // Эмодзи (или можно использовать int для R.drawable)
        public String title;       // Заголовок (например, "Высокий пульс!")
        public String description; // Сам текст совета
        public String timing;      // "Middle", "End" и т.д.

        public WorkoutAdvice(String icon, String title, String description, String timing) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.timing = timing;
        }
    }
}