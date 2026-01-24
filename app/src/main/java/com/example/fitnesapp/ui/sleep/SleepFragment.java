package com.example.fitnesapp.ui.sleep;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentSleepBinding;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.SleepStageItem; // Ваша новая модель
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SleepFragment extends Fragment {

    private SleepViewModel mViewModel;
    private FragmentSleepBinding binding;

    public static SleepFragment newInstance() {
        return new SleepFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSleepBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(SleepViewModel.class);

        setupCalendar();

        // 1. Имя пользователя
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);
            }
        });

        // 2. Данные о сне (вся логика тут)
        mViewModel.getSleepData().observe(getViewLifecycleOwner(), this::updateSleepUI);
    }

    private void updateSleepUI(DailyData.Sleep sleep) {
        if (sleep == null) {
            clearUI();
            return;
        }

        // --- ТЕКСТОВЫЕ ДАННЫЕ ---
        binding.tvSleepScore.setText(String.valueOf(sleep.score));
        binding.tvSleepQuality.setText(sleep.quality != null ? sleep.quality : "--");

        int h = sleep.durationMinutes / 60;
        int m = sleep.durationMinutes % 60;
        binding.tvSleepTime.setText(String.format(Locale.US, "%dh %02d min", h, m));

        binding.tvGettingIntoBed.setText(sleep.bedTime != null ? sleep.bedTime : "--:--");
        binding.tvAwaking.setText(sleep.wakeTime != null ? sleep.wakeTime : "--:--");
        binding.tvFallAsleep.setText(sleep.fallingAsleepMin + " min");

        // --- МИНИ-ДИАГРАММЫ (PIE) ---
        if (sleep.phases != null) {
            updatePhase(binding.chartDeep, binding.tvDeepPercent, binding.tvDeepTime,
                    sleep.phases.deep, sleep.durationMinutes, Color.parseColor("#9C27B0"));

            updatePhase(binding.chartSurface, binding.tvSurfacePercent, binding.tvSurfaceTime,
                    sleep.phases.surface, sleep.durationMinutes, Color.parseColor("#4DD0E1"));

            updatePhase(binding.chartFast, binding.tvFastPercent, binding.tvFastTime,
                    sleep.phases.rem, sleep.durationMinutes, Color.parseColor("#FF9800"));

            updatePhase(binding.chartAwake, binding.tvAwakePercent, binding.tvAwakeTime,
                    sleep.phases.awake, sleep.durationMinutes, Color.parseColor("#F44336"));
        }

        // --- ГЛАВНЫЙ ГРАФИК (ГИПНОГРАММА) ---
        if (sleep.hypnogram != null && !sleep.hypnogram.isEmpty()) {
            setupRealSleepChart(sleep.hypnogram);
        } else {
            binding.chartSleepInfo.clear(); // Если данных нет, очищаем
        }
    }

    private void setupRealSleepChart(java.util.Map<String, SleepStageItem> hypnogramMap) {
        // 1. Преобразуем Map в List и сортируем по времени
        List<SleepStageItem> dataPoints = new ArrayList<>(hypnogramMap.values());
        Collections.sort(dataPoints, (o1, o2) -> Long.compare(o1.time, o2.time));

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < dataPoints.size(); i++) {
            SleepStageItem item = dataPoints.get(i);

            // Y = Фаза (0, 1, 2, 3)
            entries.add(new Entry(i, item.stage));

            // X Label = Время ("23:00")
            labelsList.add(timeFormat.format(new Date(item.time)));
        }

        // Превращаем список подписей в массив
        String[] labels = labelsList.toArray(new String[0]);

        // 2. ИСПОЛЬЗУЕМ ChartHelper (Как в пульсе)
        // Он сам сделает график плавным, уберет лишние оси и настроит градиент
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartSleepInfo,
                entries,
                labels,
                R.color.sleep_start, // Убедитесь, что эти цвета есть в colors.xml
                R.color.sleep_end,
                true // Включить заливку градиентом
        );
    }
    private void updatePhase(PieChart chart, android.widget.TextView tvPercent, android.widget.TextView tvTime,
                             int percentage, int totalDurationMin, int color) {
        tvPercent.setText(percentage + "%");
        int phaseDuration = (int) (totalDurationMin * (percentage / 100.0f));
        int h = phaseDuration / 60;
        int m = phaseDuration % 60;
        tvTime.setText(h > 0 ? String.format(Locale.US, "%dh %dmin", h, m) : String.format(Locale.US, "%d min", m));
        setupMiniPie(chart, percentage, color);
    }

    private void setupMiniPie(PieChart chart, int percentage, int color) {
        if (chart == null) return;
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) percentage));
        entries.add(new PieEntry((float) (100 - percentage)));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(color, Color.parseColor("#232D36"));
        dataSet.setDrawValues(false);
        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.setDescription(null);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(70f);
        chart.setTransparentCircleRadius(0f);
        chart.setMinOffset(0f);
        chart.invalidate();
    }

    private void clearUI() {
        binding.tvSleepScore.setText("--");
        binding.tvSleepQuality.setText("--");
        binding.chartSleepInfo.clear();
        // ... очистка остальных полей (как в прошлом коде) ...
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewSleep,
                calendarDate -> {
                    mViewModel.loadSleepData(calendarDate.getDate());
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}