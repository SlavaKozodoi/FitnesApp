package com.example.fitnesapp.models;

import java.util.ArrayList;
import com.github.mikephil.charting.data.Entry;

public class TrainingStat {
    private String title;
    private String value;
    private int iconResId;
    private int startColorResId;
    private int endColorResId;
    private ArrayList<Entry> chartData;
    private String[] labelData;

    public TrainingStat(String title, String value, int iconResId, int startColorResId, int endColorResId, ArrayList<Entry> chartData, String[] labels) {        this.title = title;
        this.value = value;
        this.iconResId = iconResId;
        this.startColorResId = startColorResId;
        this.endColorResId = endColorResId;
        this.chartData = chartData;
        this.labelData = labels;

    }

    // Геттеры
    public String getTitle() { return title; }
    public String getValue() { return value; }
    public int getIconResId() { return iconResId; }
    public int getStartColorResId() { return startColorResId; }
    public int getEndColorResId() { return endColorResId; }
    public ArrayList<Entry> getChartData() { return chartData; }
    public String[] getLabelData() { return labelData; }
}