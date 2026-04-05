package com.example.fitnesapp.utils;

import com.example.fitnesapp.R; // Обязательно импортируй свой R-класс

import java.util.Arrays;
import java.util.List;

public class LevelSystem {

    // Модель одного ранга
    public static class Rank {
        public int titleResId; // ИЗМЕНЕНИЕ: Храним ID ресурса, а не строку
        public int minXP;

        public Rank(int titleResId, int minXP) {
            this.titleResId = titleResId;
            this.minXP = minXP;
        }
    }

    // Расширенная настройка уровней (15 штук, плавная экспонента)
    private static final List<Rank> RANKS = Arrays.asList(
            new Rank(R.string.rank_novice, 0),
            new Rank(R.string.rank_beginner, 1000),
            new Rank(R.string.rank_walker, 3000),
            new Rank(R.string.rank_hiker, 6000),
            new Rank(R.string.rank_runner, 10000),
            new Rank(R.string.rank_sprinter, 15000),
            new Rank(R.string.rank_challenger, 22000),
            new Rank(R.string.rank_athlete, 30000),
            new Rank(R.string.rank_warrior, 45000),
            new Rank(R.string.rank_step_master, 65000),
            new Rank(R.string.rank_iron_body, 90000),
            new Rank(R.string.rank_elite, 120000),
            new Rank(R.string.rank_champion, 160000),
            new Rank(R.string.rank_titan, 220000),
            new Rank(R.string.rank_legend, 300000)
    );

    public static class LevelInfo {
        public int currentRankTitleResId; // ИЗМЕНЕНИЕ: Передаем ID наружу
        public int currentLevelXP;    // Сколько набрано на ЭТОМ уровне
        public int xpToNextLevel;     // Длина ЭТОГО уровня
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

        info.currentRankTitleResId = currentRank.titleResId;

        if (nextRank != null) {
            info.isMaxLevel = false;
            info.nextLevelThreshold = nextRank.minXP;

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