package com.example.fitnesapp.ui.oxygenlevel;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentOxygenBinding;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.ui.base.BaseLoadingFragment;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.data.Entry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OxygenFragment extends BaseLoadingFragment {

    private OxygenViewModel mViewModel;
    private FragmentOxygenBinding binding;

    public static OxygenFragment newInstance() {
        return new OxygenFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOxygenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(OxygenViewModel.class);

        // Отключаем ручное управление SeekBar (чтобы он был только индикатором)
        binding.statusBarOxygenScore.setOnTouchListener((v, event) -> true);

        // 1. Календарь
        setupCalendar();

        // 2. Имя пользователя
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);
            }
        });

        // 3. История (График)
        mViewModel.getOxygenHistory().observe(getViewLifecycleOwner(), this::updateChartUI);

        // 4. Аналитика (Цифры + SeekBar)
        mViewModel.getAnalysisData().observe(getViewLifecycleOwner(), this::updateAnalysisUI);

        startFakeLoading(view, 200);

    }

    private void updateAnalysisUI(OxygenViewModel.OxygenAnalysis analysis) {
        if (analysis == null || analysis.avg == 0) {
            binding.tvOxygenScore.setText("--");
            binding.tvScoreOfOxygenStat.setText("--");
            binding.tvHighestOxygen.setText("--");
            binding.tvLowestOxygen.setText("--");
            binding.statusBarOxygenScore.setProgress(0);
            return;
        }

        // --- ИСПРАВЛЕНО: Форматируем значения до 1 знака после запятой (00.0) ---
        binding.tvOxygenScore.setText(String.format(Locale.US, "%.1f", analysis.avg));
        binding.tvScoreOfOxygenStat.setText(String.format(Locale.US, "%.1f", analysis.avg));
        binding.tvOxygenQuality.setText(analysis.status);

        binding.tvHighestOxygen.setText(String.format(Locale.US, "%.1f", analysis.max));
        binding.tvLowestOxygen.setText(String.format(Locale.US, "%.1f", analysis.min));

        // --- ЛОГИКА SEEKBAR ---
        // Зеленый слева (0), Красный справа (100).
        // SpO2: 100% -> Идеально (0 на шкале)
        // SpO2: 90% -> Плохо (100 на шкале)

        double oxygen = analysis.avg;
        int progress;

        if (oxygen >= 100) progress = 5; // Самый левый край
        else if (oxygen <= 85) progress = 95; // Самый правый край
        else {
            // Формула: чем меньше кислород, тем больше прогресс
            // Диапазон кислорода 100..85 (разница 15)
            // Диапазон шкалы 0..100
            progress = (int) ((100 - oxygen) * (100.0f / 15.0f));
        }

        // Плавная анимация шкалы
        binding.statusBarOxygenScore.setProgress(progress, true);
    }

    private void updateChartUI(List<HealthLogItem> logs) {


        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < logs.size(); i++) {
            HealthLogItem item = logs.get(i);
            entries.add(new Entry(i, (float) item.val));
            labelsList.add(timeFormat.format(new Date(item.time)));
        }

        String[] labels = labelsList.toArray(new String[0]);

        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartOxygenInfo,
                entries,
                labels,
                R.color.oxygen_start,
                R.color.oxygen_end,
                true
        );
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewOxygen,
                date -> {
                    // Загружаем данные за выбранную дату
                    mViewModel.loadDataForDate(date.getDate());
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}