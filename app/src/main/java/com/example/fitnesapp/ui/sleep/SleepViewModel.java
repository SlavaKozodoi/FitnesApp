package com.example.fitnesapp.ui.sleep;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SleepViewModel extends ViewModel {

    // Основные данные о сне
    private final MutableLiveData<String> sleepQuality = new MutableLiveData<>();
    private final MutableLiveData<String> totalSleepTime = new MutableLiveData<>();
    private final MutableLiveData<String> fallAsleepTime = new MutableLiveData<>();
    private final MutableLiveData<String> wakeUpTime = new MutableLiveData<>();
    private final MutableLiveData<String> bedTime = new MutableLiveData<>();

    // Фазы сна (Проценты)
    private final MutableLiveData<Integer> deepSleepPercent = new MutableLiveData<>();
    private final MutableLiveData<Integer> surfaceSleepPercent = new MutableLiveData<>();
    private final MutableLiveData<Integer> fastSleepPercent = new MutableLiveData<>();
    private final MutableLiveData<Integer> awakeSleepPercent = new MutableLiveData<>();

    public SleepViewModel() {
        // Имитация загрузки данных (в будущем замените на запрос к БД)
        sleepQuality.setValue("Very well");
        totalSleepTime.setValue("10h 02 min");
        fallAsleepTime.setValue("15 min");
        wakeUpTime.setValue("09:02");
        bedTime.setValue("23:00");

        deepSleepPercent.setValue(20);
        surfaceSleepPercent.setValue(51);
        fastSleepPercent.setValue(17);
        awakeSleepPercent.setValue(12);
    }

    // Геттеры для наблюдения
    public LiveData<String> getSleepQuality() { return sleepQuality; }
    public LiveData<String> getTotalSleepTime() { return totalSleepTime; }
    public LiveData<String> getFallAsleepTime() { return fallAsleepTime; }
    public LiveData<String> getWakeUpTime() { return wakeUpTime; }
    public LiveData<String> getBedTime() { return bedTime; }

    public LiveData<Integer> getDeepSleepPercent() { return deepSleepPercent; }
    public LiveData<Integer> getSurfaceSleepPercent() { return surfaceSleepPercent; }
    public LiveData<Integer> getFastSleepPercent() { return fastSleepPercent; }
    public LiveData<Integer> getAwakeSleepPercent() { return awakeSleepPercent; }
}