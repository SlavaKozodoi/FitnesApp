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

    public MLPredictor(Context context) {
        try {
            tfliteRecovery = new Interpreter(loadModelFile(context, "model_recovery.tflite"));
            tfliteHabits = new Interpreter(loadModelFile(context, "model_habits.tflite"));
            tfliteMacros = new Interpreter(loadModelFile(context, "model_macros.tflite"));
            Log.d("ML", "Все 3 модели успешно загружены!");
        } catch (Exception e) {
            Log.e("ML", "Ошибка загрузки моделей", e);
        }
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

    public float predictMacros(float currentHour, float caloriesEaten, float caloriesGoal, float caloriesBurned, float burnGoal) {
        if (tfliteMacros == null) return -1f;

        float normTime = currentHour / 24.0f;
        float personalCalGoal = (caloriesGoal > 0) ? caloriesGoal : 2000f;
        float personalBurnGoal = (burnGoal > 0) ? burnGoal : 500f;

        float normEaten = caloriesEaten / personalCalGoal;
        float normBurned = caloriesBurned / personalBurnGoal;

        float[][] inputValues = new float[1][3];
        inputValues[0][0] = normTime;
        inputValues[0][1] = normEaten;
        inputValues[0][2] = normBurned;

        float[][] outputValue = new float[1][1];
        tfliteMacros.run(inputValues, outputValue);

        return outputValue[0][0];
    }

    public void close() {
        if (tfliteRecovery != null) tfliteRecovery.close();
        if (tfliteHabits != null) tfliteHabits.close();
        if (tfliteMacros != null) tfliteMacros.close();
    }
}