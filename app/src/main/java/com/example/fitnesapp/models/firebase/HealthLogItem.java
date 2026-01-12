package com.example.fitnesapp.models.firebase;

public class HealthLogItem {
    public int val;      // Значение пульса или кислорода
    public long time;    // Timestamp (время в миллисекундах)

    public HealthLogItem() {}

    // Конструктор для удобства добавления
    public HealthLogItem(int val, long time) {
        this.val = val;
        this.time = time;
    }
}
