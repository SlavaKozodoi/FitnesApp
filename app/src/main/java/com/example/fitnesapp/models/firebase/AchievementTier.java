package com.example.fitnesapp.models.firebase;

public class AchievementTier {
    public int currentTear;     // Текущий уровень (3)
    public int maxTear;         // Макс уровень (5)
    public int xpReward;
    public boolean isClaimed;   // Забрана ли награда

    // Поля прогресса (в разных ачивках они разные)
    public double currentSteps;
    public double targetSteps;

    public double currentKm;
    public double targetKm;

    public double currentMin;
    public double targetMin;

    public AchievementTier() {}
}
