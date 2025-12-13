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

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentWeightBinding;
import com.example.fitnesapp.utils.ChartHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class WeightFragment extends Fragment {

    private WeightViewModel mViewModel;
    private FragmentWeightBinding binding;

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

//    private void setupWeightHistoryChart() {
//        LineChart chart = binding.chartWeightInfo;
//        if (chart == null) return;
//
//        // 1. ГЕНЕРАЦИЯ ДАННЫХ
//        ArrayList<Entry> entries = new ArrayList<>();
//        entries.add(new Entry(0, 2f));
//        entries.add(new Entry(1, 1f));
//        entries.add(new Entry(2, 0f));
//        entries.add(new Entry(3, 0f));
//        entries.add(new Entry(4, 1f));
//        entries.add(new Entry(5, 0f));
//        entries.add(new Entry(6, 1f));
//        entries.add(new Entry(7, 2f));
//        entries.add(new Entry(8, 1f));
//        entries.add(new Entry(9, 0f));
//        entries.add(new Entry(10, 1f));
//        entries.add(new Entry(11, 2f));
//
//        // Массив подписей времени, соответствующий точкам выше (с шагом 30 мин)
//        final String[] timeLabels = new String[]{
//                "4.11", "6.11", "9.11", "13.11",
//                "14.11", "16.11", "20.11", "23.11",
//                "26.11", "29.11", "30.11", "4.12"
//        };
//
//        // 2. НАСТРОЙКА ЛИНИИ
//        LineDataSet dataSet = new LineDataSet(entries, "Sleep Stages");
//        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//        dataSet.setCubicIntensity(0.2f);
//        dataSet.setDrawCircles(false);
//        dataSet.setDrawValues(false);
//        dataSet.setLineWidth(2f);
//
//        int sleepColor = ContextCompat.getColor(requireContext(), R.color.weight_green);
//        dataSet.setColor(sleepColor);
//        dataSet.setDrawFilled(true);
//        dataSet.setFillColor(sleepColor);
//        dataSet.setFillAlpha(100);
//
//        LineData data = new LineData(dataSet);
//        chart.setData(data);
//
//        // 3. НАСТРОЙКА ОСЕЙ Y (Вертикальная)
//        YAxis leftAxis = chart.getAxisLeft();
//        leftAxis.setAxisMinimum(0f);
//        leftAxis.setAxisMaximum(2.5f);
//        leftAxis.setDrawGridLines(false);
//        leftAxis.setDrawAxisLine(false);
//        leftAxis.setDrawLabels(false); // Скрываем цифры слева
//
//        chart.getAxisRight().setEnabled(false);
//
//        // 4. НАСТРОЙКА ОСИ X (ВРЕМЯ СНИЗУ)
//        com.github.mikephil.charting.components.XAxis xAxis = chart.getXAxis();
//        xAxis.setEnabled(true); // Включаем ось!
//        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM); // Позиция снизу
//        xAxis.setDrawGridLines(false); // Без сетки
//        xAxis.setDrawAxisLine(false);  // Без линии оси
//        xAxis.setTextColor(Color.parseColor("#E0E0E0")); // Цвет текста (светло-серый)
//        xAxis.setTextSize(10f);
//        xAxis.setGranularity(1f); // Чтобы метки не дублировались
//
//        // Форматтер: превращает число 0 в "23:00", 1 в "23:30" и т.д.
//        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
//            @Override
//            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
//                int index = (int) value;
//                if (index >= 0 && index < timeLabels.length) {
//                    return timeLabels[index];
//                }
//                return "";
//            }
//        });
//
//        // Общие настройки
//        chart.getLegend().setEnabled(false);
//        chart.getDescription().setEnabled(false);
//        chart.setTouchEnabled(false);
//
//        /// === ИЗМЕНЕНИЕ ФОНА ===
//
//        // 1. ОТКЛЮЧАЕМ встроенный прямоугольный фон (если был включен)
//        chart.setDrawGridBackground(false);
//
//        // 2. ВКЛЮЧАЕМ наш скругленный фон
//        chart.setBackgroundResource(R.drawable.bg_chart_dark);
//
//
//        chart.invalidate();
//    }



}