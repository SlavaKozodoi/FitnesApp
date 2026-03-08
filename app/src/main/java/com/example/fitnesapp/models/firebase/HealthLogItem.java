package com.example.fitnesapp.models.firebase;

public class HealthLogItem {
    public double val;      // Значение пульса или кислорода
    public long time;    // Timestamp (время в миллисекундах)

    public HealthLogItem() {}

    // Конструктор для удобства добавления
    public HealthLogItem(double val, long time) {
        this.val = val;
        this.time = time;
    }
}
