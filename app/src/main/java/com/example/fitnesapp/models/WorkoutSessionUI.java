package com.example.fitnesapp.models;

import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.WorkoutItem;
import java.util.List;

public class WorkoutSessionUI {
    public WorkoutItem workoutItem;
    public List<HealthLogItem> pulseLogs;
    public List<HealthLogItem> paceLogs;
    public List<HealthLogItem> oxygenLogs;

    public WorkoutSessionUI(WorkoutItem workoutItem, List<HealthLogItem> pulseLogs, List<HealthLogItem> paceLogs, List<HealthLogItem> oxygenLogs) {
        this.workoutItem = workoutItem;
        this.pulseLogs = pulseLogs;
        this.paceLogs = paceLogs;
        this.oxygenLogs = oxygenLogs;
    }
}