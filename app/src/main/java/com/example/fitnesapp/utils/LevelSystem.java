package com.example.fitnesapp.utils;

import java.util.Arrays;
import java.util.List;

public class LevelSystem {

    // Модель одного ранга
    public static class Rank {
        public String title;
        public int minXP;

        public Rank(String title, int minXP) {
            this.title = title;
            this.minXP = minXP;
        }
    }

    // Настройка уровней (чем дальше, тем сложнее)
    private static final List<Rank> RANKS = Arrays.asList(
            new Rank("Novice", 0),
            new Rank("Beginner", 1000),      // 0 -> 1000
            new Rank("Walker", 3000),        // 1000 -> 3000 (надо набрать 2000)
            new Rank("Hiker", 7000),         // 3000 -> 7000 (надо набрать 4000)
            new Rank("Runner", 15000),       // 7000 -> 15000
            new Rank("Athlete", 30000),
            new Rank("Step Master", 50000),
            new Rank("Elite", 80000),
            new Rank("Legend", 150000)
    );

    public static class LevelInfo {
        public String currentRankTitle;
        public int currentLevelXP;    // Сколько набрано на ЭТОМ уровне (для прогрессбара)
        public int xpToNextLevel;     // Длина ЭТОГО уровня (для max прогрессбара)
        public int totalXP;           // Общий опыт
        public int nextLevelThreshold; // Общая цель (например 3000)

        public boolean isMaxLevel;    // Если дальше уровней нет
    }

    // Главный метод расчета
    public static LevelInfo calculate(int totalXP) {
        LevelInfo info = new LevelInfo();
        info.totalXP = totalXP;

        // 1. Находим текущий ранг
        Rank currentRank = RANKS.get(0);
        Rank nextRank = null;

        for (int i = 0; i < RANKS.size(); i++) {
            if (totalXP >= RANKS.get(i).minXP) {
                currentRank = RANKS.get(i);
                // Проверяем, есть ли следующий уровень
                if (i + 1 < RANKS.size()) {
                    nextRank = RANKS.get(i + 1);
                } else {
                    nextRank = null; // Максимальный уровень
                }
            } else {
                break; // Мы нашли наш текущий уровень, дальше проверять нет смысла
            }
        }

        info.currentRankTitle = currentRank.title;

        if (nextRank != null) {
            info.isMaxLevel = false;
            info.nextLevelThreshold = nextRank.minXP;

            // Математика для прогрессбара:
            // Если я на уровне Walker (3000 - 7000) и у меня 4500 XP.
            // Мой прогресс внутри уровня: 4500 - 3000 = 1500.
            // Длина уровня: 7000 - 3000 = 4000.
            info.currentLevelXP = totalXP - currentRank.minXP;
            info.xpToNextLevel = nextRank.minXP - currentRank.minXP;
        } else {
            // Максимальный уровень
            info.isMaxLevel = true;
            info.currentLevelXP = 100;
            info.xpToNextLevel = 100;
            info.nextLevelThreshold = totalXP;
        }

        return info;
    }
}