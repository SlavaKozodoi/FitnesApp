package com.example.fitnesapp.ui.weight;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.ui.base.BaseLoadingFragment;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentWeightBinding;
import com.example.fitnesapp.models.firebase.WeightHistoryItem;
import com.example.fitnesapp.utils.ChartHelper;
import com.github.mikephil.charting.data.Entry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeightFragment extends BaseLoadingFragment {

    private WeightViewModel mViewModel;
    private FragmentWeightBinding binding;

    private List<WeightHistoryItem> allWeightHistory = new ArrayList<>();

    private double userHeightMeters = 0;

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

        // Используем формат строк для правильной интернационализации кнопок
        binding.button.setText(getString(R.string.weight_btn_format, 1, getString(R.string.weight_month)));
        binding.button2.setText(getString(R.string.weight_btn_format, 3, getString(R.string.weight_month)));
        binding.button3.setText(getString(R.string.weight_btn_format, 6, getString(R.string.weight_month)));

        setupTimeFilters();

        // 1. Профиль (для BMI)
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);
                if (profile.height > 0) {
                    userHeightMeters = profile.height / 100.0;
                    recalculateBMI();
                }
            }
        });

        // 2. Текущий вес
        mViewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null) {
                binding.tvWeightScore.setText(String.format(Locale.US, "%.1f", weight));
                recalculateBMI();
            }
        });

        // 3. История веса
        mViewModel.getWeightHistory().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                allWeightHistory = list;
                updateChartForPeriod(1); // По умолчанию 1 месяц
                updateButtonVisuals(binding.button);
            }
        });

        startFakeLoading(view, 200);
    }

    private void recalculateBMI() {
        try {
            String wStr = binding.tvWeightScore.getText().toString().replace(",", ".");
            double weight = Double.parseDouble(wStr);

            if (weight > 0 && userHeightMeters > 0) {
                double bmi = weight / (userHeightMeters * userHeightMeters);
                binding.tvBMI.setText(String.format(Locale.US, "%.1f", bmi));

                // Используем локализованные ресурсы статусов
                String status = getString(R.string.weight_status_normal);
                if (bmi < 18.5) status = getString(R.string.weight_status_underweight);
                else if (bmi >= 25 && bmi < 30) status = getString(R.string.weight_status_overweight);
                else if (bmi >= 30) status = getString(R.string.weight_status_obese);

                binding.tvWeightStat.setText(status);
            }
        } catch (NumberFormatException e) { }
    }

    private void setupTimeFilters() {
        binding.button.setOnClickListener(v -> {
            updateChartForPeriod(1);
            updateButtonVisuals(binding.button);
        });
        binding.button3.setOnClickListener(v -> {
            updateChartForPeriod(3);
            updateButtonVisuals(binding.button3);
        });
        binding.button2.setOnClickListener(v -> {
            updateChartForPeriod(6);
            updateButtonVisuals(binding.button2);
        });
    }

    private void updateChartForPeriod(int months) {
        // 1. Вычисляем дату отсечения
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        long cutoffTime = cal.getTimeInMillis();

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();

        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat chartFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        int index = 0;

        for (WeightHistoryItem record : allWeightHistory) {
            try {
                Date dateObj = dbFormat.parse(record.date);

                if (dateObj != null && dateObj.getTime() >= cutoffTime) {
                    float val = (float) record.val;

                    if (val > max) max = val;
                    if (val < min) min = val;

                    entries.add(new Entry(index, val));
                    labelsList.add(chartFormat.format(dateObj));
                    index++;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // ЗАЩИТА ТЕКСТА ОТ СУМАСШЕДШИХ ЦИФР
        if (entries.isEmpty()) {
            // Используем пустой формат из HomeFragment
            binding.tvHighestWeight.setText(getString(R.string.format_kg_empty));
            binding.tvLowestWeight.setText(getString(R.string.format_kg_empty));
        } else {
            // Используем формат кг из HomeFragment
            binding.tvHighestWeight.setText(getString(R.string.format_kg, max));
            binding.tvLowestWeight.setText(getString(R.string.format_kg, min));
        }

        String[] labels = labelsList.toArray(new String[0]);

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

    private void updateButtonVisuals(Button activeButton) {
        resetButtonStyle(binding.button);
        resetButtonStyle(binding.button3);
        resetButtonStyle(binding.button2);
        activeButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_color));
        activeButton.setTextColor(Color.BLACK);
    }

    private void resetButtonStyle(Button btn) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.background));
        btn.setTextColor(Color.WHITE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}