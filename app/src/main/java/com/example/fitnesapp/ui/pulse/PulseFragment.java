package com.example.fitnesapp.ui.pulse;

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
import com.example.fitnesapp.databinding.FragmentPulseBinding;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.data.Entry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PulseFragment extends Fragment {

    private PulseViewModel mViewModel;
    private FragmentPulseBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPulseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(PulseViewModel.class);

        // 1. Настройка календаря
        setupCalendar();

        // 2. Имя пользователя
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);
            }
        });

        // 4. Подписка на Историю (График + ТЕКСТ ПОСЛЕДНЕГО ЗНАЧЕНИЯ)
        mViewModel.getPulseHistory().observe(getViewLifecycleOwner(), logs -> {
            // 1. Рисуем график (как и было)
            updateChartUI(logs);

            // 2. НОВАЯ ЛОГИКА: Обновляем цифру пульса последним значением
            if (logs != null && !logs.isEmpty()) {
                // Берем последний элемент списка (самый свежий по времени)
                HealthLogItem lastItem = logs.get(logs.size() - 1);

                // Обновляем большую цифру
                binding.tvPulseScore.setText(String.valueOf((int) lastItem.val));

                // Обновляем статус (Very well / High и т.д.)
                binding.tvPulseQuality.setText(getPulseStatus((int) lastItem.val));
            } else {
                // Если истории нет, ставим прочерки
                binding.tvPulseScore.setText("--");
                binding.tvPulseQuality.setText("--");
            }
        });



        // 5. ВЫЧИСЛЕННЫЕ ДАННЫЕ (Мин, Макс, Периоды)
        mViewModel.getAnalysisData().observe(getViewLifecycleOwner(), analysis -> {
            if (analysis != null) {
                // Максимальный и минимальный
                binding.tvHighestPulse.setText(analysis.maxPulse > 0 ? analysis.maxPulse + " bpm" : "--");
                binding.tvLowestPulse.setText(analysis.minPulse > 0 ? analysis.minPulse + " bpm" : "--");

                // Периоды
                binding.tvActivePeriod.setText(analysis.activePeriod);
                binding.tvRestPeriod.setText(analysis.restPeriod);
            }
        });
    }

    private void updateChartUI(List<HealthLogItem> logs) {
        if (logs == null || logs.isEmpty()) {
            binding.chartPulseInfo.clear();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < logs.size(); i++) {
            HealthLogItem item = logs.get(i);
            entries.add(new Entry(i, (float) item.val));

            // Используем реальное время из timestamp
            try {
                labelsList.add(timeFormat.format(new Date(item.time)));
            } catch (Exception e) {
                labelsList.add("");
            }
        }

        String[] labels = labelsList.toArray(new String[0]);

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

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewPulse,
                calendarDate -> {
                    // При клике обновляем ViewModel
                    // calendarDate.getDate() возвращает объект Date
                    mViewModel.loadDataForDate(calendarDate.getDate());

                    Toast.makeText(getContext(), "Date selected: " + calendarDate.getDayNumber(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    // TODO: 13.01.2026 сделать лучшую оценку
    private String getPulseStatus(int pulse) {
        if (pulse == 0) return "--";
        if (pulse < 60) return "Low";
        if (pulse < 85) return "Normal";
        if (pulse < 100) return "Elevated";
        return "High";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}