package com.example.fitnesapp.models.firebase;

public class SleepStageItem {
    public long time;  // Время точки (timestamp)
    public int stage;  // Фаза: 3=Awake, 2=REM, 1=Light, 0=Deep

    public SleepStageItem() {}

    public SleepStageItem(long time, int stage) {
        this.time = time;
        this.stage = stage;
    }
}
