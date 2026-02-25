package com.example.fitnesapp.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MLPredictor {

    private Interpreter tfliteRecovery;
    private Interpreter tfliteHabits;
    private Interpreter tfliteMacros;
    private Interpreter tfliteSleep;

    public MLPredictor(Context context) {
        try {
            tfliteRecovery = new Interpreter(loadModelFile(context, "model_recovery.tflite"));
            tfliteHabits = new Interpreter(loadModelFile(context, "model_habits.tflite"));
            tfliteMacros = new Interpreter(loadModelFile(context, "model_macros.tflite"));
            tfliteSleep = new Interpreter(loadModelFile(context, "model_sleep.tflite"));
            Log.d("ML", "Все 3 модели успешно загружены!");
        } catch (Exception e) {
            Log.e("ML", "Ошибка загрузки моделей", e);
        }
    }

    public float predictSleepQuality(long totalMinutes, long deepMinutes, long remMinutes, int awakenings, long avgHistoryMinutes) {
        if (tfliteSleep == null) return -1f;

        // 1. Нормализация данных
        float normTotal = (float) totalMinutes / 480f; // 8 часов = 1.0
        float normDeep = totalMinutes > 0 ? (float) deepMinutes / totalMinutes : 0f;
        float normRem = totalMinutes > 0 ? (float) remMinutes / totalMinutes : 0f;
        float normAwakenings = Math.min((float) awakenings / 10f, 1.0f); // Максимум 1.0
        float normHistory = (float) avgHistoryMinutes / 480f; // 8 часов в среднем = 1.0

        // 2. Упаковываем 5 входов
        float[][] inputValues = new float[1][5];
        inputValues[0][0] = normTotal;
        inputValues[0][1] = normDeep;
        inputValues[0][2] = normRem;
        inputValues[0][3] = normAwakenings;
        inputValues[0][4] = normHistory;

        float[][] outputValue = new float[1][1];
        tfliteSleep.run(inputValues, outputValue);

        return outputValue[0][0];
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float predictRecovery(long sleepMinutes, long steps, float caloriesBurned,
                                 long stepsGoal, float caloriesGoal,
                                 int age, float weight, float height) {
        if (tfliteRecovery == null) return -1f;

        float personalStepGoal = (stepsGoal > 0) ? (float) stepsGoal : 10000f;
        float personalCalGoal = (caloriesGoal > 0) ? caloriesGoal : 2000f;

        float normSleep = (float) sleepMinutes / 480f;
        float normSteps = (float) steps / personalStepGoal;
        float normCalories = caloriesBurned / personalCalGoal;
        float normAge = (age > 0) ? (float) age / 100f : 0.25f;
        float normWeight = (weight > 0) ? weight / 200f : 0.375f;
        float normHeight = (height > 0) ? height / 250f : 0.7f;

        float[][] inputValues = new float[1][6];
        inputValues[0][0] = normSleep; inputValues[0][1] = normSteps; inputValues[0][2] = normCalories;
        inputValues[0][3] = normAge; inputValues[0][4] = normWeight; inputValues[0][5] = normHeight;

        float[][] outputValue = new float[1][1];
        tfliteRecovery.run(inputValues, outputValue);
        return outputValue[0][0];
    }

    public float predictHabitSuccess(float currentHour, long steps, long stepsGoal, float calories, float caloriesGoal) {
        if (tfliteHabits == null) return -1f;

        float normTime = currentHour / 24.0f;
        float personalStepGoal = (stepsGoal > 0) ? (float) stepsGoal : 10000f;
        float personalCalGoal = (caloriesGoal > 0) ? caloriesGoal : 2000f;

        float normSteps = (float) steps / personalStepGoal;
        float normCalories = calories / personalCalGoal;

        float[][] inputValues = new float[1][3];
        inputValues[0][0] = normTime;
        inputValues[0][1] = normSteps;
        inputValues[0][2] = normCalories;

        float[][] outputValue = new float[1][1];
        tfliteHabits.run(inputValues, outputValue);

        return outputValue[0][0];
    }

    // Метод для аналитики питания
    public float predictMacros(float currentHour, float calsEaten, float calsGoal, float calsBurned, float burnGoal) {
        if (tfliteMacros == null) return -1f;

        // 1. Нормализуем данные точно так же, как делали в Python!
        // Время (8:30 утра = 8.5 / 24.0 = 0.35)
        float timeNorm = currentHour / 24.0f;

        // Защита от деления на ноль
        float safeCalsGoal = calsGoal > 0 ? calsGoal : 2000f;
        float safeBurnGoal = burnGoal > 0 ? burnGoal : 500f;

        float foodNorm = calsEaten / safeCalsGoal;
        float burnNorm = calsBurned / safeBurnGoal;

        // 2. Упаковываем в массив (3 входа)
        float[][] inputValues = new float[1][3];
        inputValues[0][0] = timeNorm;
        inputValues[0][1] = foodNorm;
        inputValues[0][2] = burnNorm;

        float[][] outputValue = new float[1][1];
        tfliteMacros.run(inputValues, outputValue);

        return outputValue[0][0]; // Возвращает число от 0.0 до 1.0
    }
    public void close() {
        if (tfliteRecovery != null) tfliteRecovery.close();
        if (tfliteHabits != null) tfliteHabits.close();
        if (tfliteMacros != null) tfliteMacros.close();
    }
}