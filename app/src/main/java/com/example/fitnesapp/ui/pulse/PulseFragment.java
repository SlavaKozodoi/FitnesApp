package com.example.fitnesapp.ui.pulse;

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
import android.widget.Toast;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentPulseBinding;
import com.example.fitnesapp.ui.sleep.SleepViewModel;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class PulseFragment extends Fragment {

    private PulseViewModel mViewModel;
    private FragmentPulseBinding binding;

    public static PulseFragment newInstance() {
        return new PulseFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding= FragmentPulseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PulseViewModel.class);
        setupPulseHistoryChart();
        setupCalendar();
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewPulse,
                date -> {
                    // Логика клика именно для СНА
                    Toast.makeText(getContext(), "Данные за: " + date.getDayNumber(), Toast.LENGTH_SHORT).show();
                    // TODO: 03.01.2026 Добавить обновление єкрана
                }
        );
    }


    private void setupPulseHistoryChart() {
        // 1. Готовим данные
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 2f));
        entries.add(new Entry(1, 1f));
        entries.add(new Entry(2, 0f));
        entries.add(new Entry(3, 0f));
        entries.add(new Entry(4, 1f));
        entries.add(new Entry(5, 0f));
        entries.add(new Entry(6, 1f));
        entries.add(new Entry(7, 2f));
        entries.add(new Entry(8, 1f));
        entries.add(new Entry(9, 0f));
        entries.add(new Entry(10, 1f));
        entries.add(new Entry(11, 2f));
        // ... заполнение ...

        final String[] labels = new String[]{
                "23:00", "23:30", "00:00", "00:30",
                "01:00", "01:30", "02:00", "02:30",
                "03:00", "04:00", "05:00", "07:00"
        };

        // 2. ВЫЗЫВАЕМ УТИЛИТУ (Всего одна строка!)
        // requireContext() передает контекст, нужный для получения цветов
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartPulseInfo,
                entries,
                labels,
                R.color.pulse_start,
                R.color.pulse_end,
                true
        );
    }

}