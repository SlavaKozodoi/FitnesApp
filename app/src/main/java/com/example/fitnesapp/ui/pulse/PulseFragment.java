package com.example.fitnesapp.ui.pulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.ui.base.BaseLoadingFragment;
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

public class PulseFragment extends BaseLoadingFragment {

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

            // 2. Обновляем цифру пульса последним значением
            if (logs != null && !logs.isEmpty()) {
                HealthLogItem lastItem = logs.get(logs.size() - 1);
                binding.tvPulseScore.setText(String.valueOf((int) lastItem.val));
                binding.tvPulseQuality.setText(getPulseStatus((int) lastItem.val));
            } else {
                binding.tvPulseScore.setText("--");
                binding.tvPulseQuality.setText("--");
            }
        });

        // 5. ВЫЧИСЛЕННЫЕ ДАННЫЕ (Мин, Макс, Периоды)
        mViewModel.getAnalysisData().observe(getViewLifecycleOwner(), analysis -> {
            if (analysis != null) {
                // Используем format_bpm и format_bpm_empty из HomeFragment
                binding.tvHighestPulse.setText(analysis.maxPulse > 0
                        ? getString(R.string.format_bpm, String.valueOf(analysis.maxPulse))
                        : getString(R.string.format_bpm_empty));

                binding.tvLowestPulse.setText(analysis.minPulse > 0
                        ? getString(R.string.format_bpm, String.valueOf(analysis.minPulse))
                        : getString(R.string.format_bpm_empty));

                binding.tvActivePeriod.setText(analysis.activePeriod);
                binding.tvRestPeriod.setText(analysis.restPeriod);
            }
        });

        // ==========================================
        // 3. МАГИЯ ЗАГРУЗКИ (Вызываем в самом конце)
        // ==========================================
        startFakeLoading(view, 200);
    }

    private void updateChartUI(List<HealthLogItem> logs) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (logs != null) {
            for (int i = 0; i < logs.size(); i++) {
                HealthLogItem item = logs.get(i);
                entries.add(new Entry(i, (float) item.val));
                try {
                    labelsList.add(timeFormat.format(new Date(item.time)));
                } catch (Exception e) {
                    labelsList.add("");
                }
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
                calendarDate -> mViewModel.loadDataForDate(calendarDate.getDate())
        );
    }

    private String getPulseStatus(int pulse) {
        if (pulse <= 0) return "--";

        // Спортивное сердце (Брадикардия здорового человека)
        if (pulse < 55) {
            return getString(R.string.pulse_status_athletic);
        }

        // Идеальный пульс здорового человека в покое
        if (pulse >= 55 && pulse <= 70) {
            return getString(R.string.pulse_status_excellent);
        }

        // Абсолютная медицинская норма
        if (pulse > 70 && pulse <= 85) {
            return getString(R.string.pulse_status_normal);
        }

        // Повышенный (после еды, кофе, легкий стресс или ходьба)
        if (pulse > 85 && pulse <= 100) {
            return getString(R.string.pulse_status_elevated);
        }

        // Выше 100 (Тахикардия в покое или легкая разминка)
        if (pulse > 100 && pulse <= 120) {
            return getString(R.string.pulse_status_high);
        }

        // Явная физическая нагрузка или сильный стресс
        return getString(R.string.pulse_status_intense);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}