package com.example.fitnesapp.ui.weight;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentWeightBinding;
import com.example.fitnesapp.models.WeightRecord;
import com.example.fitnesapp.utils.ChartHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeightFragment extends Fragment {

    private WeightViewModel mViewModel;
    private FragmentWeightBinding binding;
    private List<WeightRecord> allWeightHistory = new ArrayList<>();

    public static WeightFragment newInstance() {
        return new WeightFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWeightBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(WeightViewModel.class);
        setupWeightHistoryChart();

        generateMockData();

        // 2. Настраиваем кнопки
        setupTimeFilters();

        // 3. По умолчанию показываем 1 месяц
        updateChartForPeriod(1);
        updateButtonVisuals(binding.button);
    }
    private void setupTimeFilters() {
        // Кнопка "1 month"
        binding.button.setOnClickListener(v -> {
            updateChartForPeriod(1);
            updateButtonVisuals(binding.button);
        });

        // Кнопка "3 month"
        binding.button3.setOnClickListener(v -> {
            updateChartForPeriod(3);
            updateButtonVisuals(binding.button3);
        });

        // Кнопка "6 month"
        binding.button2.setOnClickListener(v -> {
            updateChartForPeriod(6);
            updateButtonVisuals(binding.button2);
        });
    }


    private void updateChartForPeriod(int months) {
        // 1. Вычисляем дату отсечения (сегодня минус N месяцев)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        long cutoffTime = cal.getTimeInMillis();

        // 2. Фильтруем данные
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();


        // Формат даты для оси X (день.месяц)
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
        float max = 0;
        float min = 200;
        int index = 0;
        for (WeightRecord record : allWeightHistory) {
            // Если дата записи больше (позже), чем дата отсечения
            if (record.timestamp >= cutoffTime) {
                if (record.weight > max) max = record.weight;
                if (record.weight < min) min = record.weight;
                entries.add(new Entry(index, record.weight));
                labelsList.add(sdf.format(new Date(record.timestamp)));
                index++;
            }
        }

        // Преобразуем список меток в массив
        String[] labels = labelsList.toArray(new String[0]);

        // 3. Рисуем график через ваш ChartHelper
        // Используем цвета, которые у вас уже настроены
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartWeightInfo,
                entries,
                labels,
                R.color.weight_start, // Или любой другой цвет для линии веса
                R.color.weight_end,
                true // showBackground
        );

        binding.tvHighestWeight.setText(String.format(Locale.getDefault(), "%.1f",max));
        binding.tvLowestWeight.setText(String.format(Locale.getDefault(), "%.1f", min));

        // Дополнительно: Обновляем текущий вес (берем последнюю запись)
        if (!entries.isEmpty()) {
            float lastWeight = entries.get(entries.size() - 1).getY();
            binding.tvWeightScore.setText(String.valueOf((int)lastWeight));
        }



    }


    private void updateButtonVisuals(Button activeButton) {
        // Сброс всех кнопок в дефолтное состояние (серый фон, черный текст)
        resetButtonStyle(binding.button);
        resetButtonStyle(binding.button3);
        resetButtonStyle(binding.button2);

        // Активация нажатой кнопки (Акцентный цвет фона, белый текст)
        activeButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_color));
        activeButton.setTextColor(Color.BLACK);
    }

    private void resetButtonStyle(Button btn) {
        // Цвет неактивной кнопки (например, белый фон или прозрачный)
        btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.background));
        btn.setTextColor(Color.WHITE);
    }

    private void generateMockData() {
        allWeightHistory.clear();
        Calendar cal = Calendar.getInstance();

        // Отматываем на 6 месяцев назад
        cal.add(Calendar.MONTH, -6);

        // Генерируем данные каждые 3 дня в течение 6 месяцев
        for (int i = 0; i < 60; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 3);

            // Имитация изменения веса (вокруг 80 кг)
            float randomWeight = 75f + (float)(Math.random() * 10 - 5);

            allWeightHistory.add(new WeightRecord(cal.getTimeInMillis(), randomWeight));
        }
    }

    private void setupWeightHistoryChart() {
        // 1. Готовим данные
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 80f));
        entries.add(new Entry(1, 81f));
        entries.add(new Entry(2, 78f));
        entries.add(new Entry(3, 79f));
        entries.add(new Entry(4, 83f));
        entries.add(new Entry(5, 80f));
        entries.add(new Entry(6, 82f));
        entries.add(new Entry(7, 84f));
        entries.add(new Entry(8, 85f));
        entries.add(new Entry(9, 82f));
        entries.add(new Entry(10, 78f));
        entries.add(new Entry(11, 76f));
        // ... заполнение ...

        final String[] labels = new String[]{
                "4.11", "6.11", "9.11", "13.11",
                "14.11", "16.11", "20.11", "23.11",
                "26.11", "29.11", "30.11", "4.12"
        };

        // 2. ВЫЗЫВАЕМ УТИЛИТУ (Всего одна строка!)
        // requireContext() передает контекст, нужный для получения цветов
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartWeightInfo,
                entries,
                labels,
                R.color.weight_start,
                R.color.weight_end,
                true
        );
    }

}