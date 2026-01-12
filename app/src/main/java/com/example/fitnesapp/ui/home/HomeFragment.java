package com.example.fitnesapp.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.fitnesapp.Adapters.CalendarAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentHomeBinding;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.models.firebase.WeightHistoryItem;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private CalendarAdapter calendarAdapter;

    // Календарь
    private final Calendar currentCalendar = Calendar.getInstance();
    // Храним загруженные даты ("2026-01-22"), чтобы фильтровать их при смене месяца
    private List<String> cachedActiveDates = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Настройка навигации
        setupNavigation();

        // 2. Настройка UI компонентов (пустые графики и календарь)
        setupHistoryCalendar();

        // 3. ПОДПИСКА НА ДАННЫЕ (OBSERVERS)

        // --- Дневная статистика (ВСЕ ЦИФРЫ) ---
        homeViewModel.getDailyData().observe(getViewLifecycleOwner(), dailyData -> {
            if (dailyData != null) {
                updateDashboardWithRealData(dailyData);
            }
        });

        // --- Профиль (Имя и Фамилия) ---
        homeViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                if (binding.tvName != null) binding.tvName.setText(profile.firstName);
                if (binding.tvSecondName != null) binding.tvSecondName.setText(profile.secondName);
            }
        });

        // --- График Пульса ---
        homeViewModel.getPulseHistory().observe(getViewLifecycleOwner(), pulseList -> {
            if (pulseList != null && !pulseList.isEmpty()) setupPulseChart(pulseList);
        });

        // --- График Веса ---
        homeViewModel.getWeightHistory().observe(getViewLifecycleOwner(), weightList -> {
            if (weightList != null && !weightList.isEmpty()) setupWeightChart(weightList);
        });

        // --- Календарь (Активные дни) ---
        homeViewModel.getActiveDays().observe(getViewLifecycleOwner(), dateStrings -> {
            if (dateStrings != null) {
                cachedActiveDates = dateStrings; // Сохраняем в память
                updateCalendarDisplay(); // Перерисовываем календарь с новыми данными
            }
        });

        return root;
    }

    // === ГЛАВНЫЙ МЕТОД ОБНОВЛЕНИЯ UI ===
    private void updateDashboardWithRealData(DailyData data) {
        // --- ЧАСТЬ 1: ВЕРХНИЙ КРУГ И ЦЕЛИ ---
        float caloriesGoal = (data.caloriesGoal > 0) ? data.caloriesGoal : 2000f;
        float stepsGoal = (data.stepsGoal > 0) ? data.stepsGoal : 10000f;
        float nutritionGoal = (data.nutrition != null && data.nutrition.maxCalories > 0)
                ? data.nutrition.maxCalories : 2000f;

        float caloriesCurrent = data.caloriesBurned;
        float stepsCurrent = data.steps;
        float nutritionCurrent = (data.nutrition != null) ? data.nutrition.totalCalories : 0;

        // Расчет процентов
        float calP = (caloriesGoal > 0) ? (caloriesCurrent / caloriesGoal) : 0f;
        float stepP = (stepsGoal > 0) ? (stepsCurrent / stepsGoal) : 0f;
        float nutP = (nutritionGoal > 0) ? (nutritionCurrent / nutritionGoal) : 0f;

        if (calP > 1f) calP = 1f;
        if (stepP > 1f) stepP = 1f;
        if (nutP > 1f) nutP = 1f;

        float totalPercentVal = ((calP + stepP + nutP) / 3f) * 100f;

        // Обновление Круга
        binding.progressCalories.setProgressMax(100f);
        binding.progressCalories.setProgressWithAnimation(totalPercentVal, 900L);
        binding.tvGoalPercent.setText(Math.round(totalPercentVal) + "%");

        // Обновление Текста под кругом (Шаги, Калории, Питание)
        if (binding.tvCaloriesValue != null) binding.tvCaloriesValue.setText(String.valueOf((int) caloriesCurrent));
        if (binding.tvCaloriesGoal != null) binding.tvCaloriesGoal.setText("/" + (int) caloriesGoal + getString(R.string.short_text_calories));

        if (binding.tvStepsValue != null) binding.tvStepsValue.setText(String.valueOf((int) stepsCurrent));
        if (binding.tvStepsGoal != null) binding.tvStepsGoal.setText("/" + (int) stepsGoal + getString(R.string.short_text_steps));

        if (binding.tvNutritionValue != null) binding.tvNutritionValue.setText(String.valueOf((int) nutritionCurrent));
        if (binding.tvNutritionGoal != null) binding.tvNutritionGoal.setText("/" + (int) nutritionGoal + getString(R.string.short_text_calories));


        // --- ЧАСТЬ 2: НИЖНИЕ КАРТОЧКИ (Сон, Пульс, Вес, Кислород) ---

        // 1. ПУЛЬС (Карточка cvPulse)
        if (data.vitals_summary != null && data.vitals_summary.pulse_avg > 0) {
            // ID из твоего XML: tvPulse
            binding.tvPulse.setText(data.vitals_summary.pulse_avg + " bpm");
        } else {
            binding.tvPulse.setText("-- bpm");
        }

        // 2. ВЕС (Карточка cvWeight)
        if (data.vitals_summary != null && data.vitals_summary.weight_today > 0) {
            // ID из твоего XML: tvWeight
            binding.tvWeight.setText(data.vitals_summary.weight_today + " kg");
        } else {
            binding.tvWeight.setText("-- kg");
        }

        // 3. КИСЛОРОД (Карточка cvOxygen)
        if (data.vitals_summary != null && data.vitals_summary.spo2_avg > 0) {
            // ID из твоего XML: tvOxygen
            binding.tvOxygen.setText(data.vitals_summary.spo2_avg + "%");

            // Статус (Good/Low). ID из XML: textView39
            if (data.vitals_summary.spo2_avg >= 95) {
                binding.tvOxygenStatus.setText("Good");
                binding.tvOxygenStatus.setTextColor(Color.parseColor("#4CAF50")); // Зеленый
            } else {
                binding.tvOxygenStatus.setText("Low");
                binding.tvOxygenStatus.setTextColor(Color.RED); // Красный
            }
        } else {
            binding.tvOxygen.setText("-- %");
        }

        // 4. СОН (Карточка cvSleep)
        if (data.sleep != null && data.sleep.durationMinutes > 0) {
            int hours = data.sleep.durationMinutes / 60;
            int mins = data.sleep.durationMinutes % 60;

            // ID из твоего XML: tvSleepTime
            binding.tvSleepTime.setText(hours + "h " + mins + "m");

            // Качество сна. ID из XML: textView25
            if (data.sleep.quality != null && !data.sleep.quality.isEmpty()) {
                binding.tvSleepStatus.setText(data.sleep.quality);
            }
        } else {
            binding.tvSleepTime.setText("-- h -- m");
        }
    }

    // === ГРАФИКИ ===
    private void setupPulseChart(List<HealthLogItem> dataValues) {
        BarChart chart = binding.chartPulse;
        if (chart == null) return;

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dataValues.size(); i++) {
            entries.add(new BarEntry(i, dataValues.get(i).val));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#FF5252"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();
    }

    private void setupWeightChart(List<WeightHistoryItem> dataValues) {
        LineChart chart = binding.chartWeight;
        if (chart == null) return;

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dataValues.size(); i++) {
            entries.add(new Entry(i, (float) dataValues.get(i).val));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(3f);
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setDrawFilled(false);

        LineData data = new LineData(dataSet);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();
    }

    private void simplifyChart(Chart<?> chart) {
        chart.setTouchEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        if (chart instanceof BarChart) {
            ((BarChart) chart).getAxisLeft().setEnabled(false);
            ((BarChart) chart).getAxisRight().setEnabled(false);
        } else if (chart instanceof LineChart) {
            ((LineChart) chart).getAxisLeft().setEnabled(false);
            ((LineChart) chart).getAxisRight().setEnabled(false);
        }
    }

    // === КАЛЕНДАРЬ ===
    private void setupHistoryCalendar() {
        binding.recyclerCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarAdapter = new CalendarAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), (day, isActive) -> {
            Toast.makeText(getContext(), "Selected day: " + day, Toast.LENGTH_SHORT).show();
            // Тут можно открыть детали дня
        });

        binding.recyclerCalendar.setAdapter(calendarAdapter);
        updateCalendarDisplay();

        binding.btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });
    }

    // Фильтрует cachedActiveDates для текущего отображаемого месяца
    private void updateCalendarDisplay() {
        SimpleDateFormat sdfTitle = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        binding.tvMonthName.setText(sdfTitle.format(currentCalendar.getTime()));

        // 1. Фильтруем активные дни
        List<Integer> activeDaysInThisMonth = new ArrayList<>();
        SimpleDateFormat sdfParse = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        for (String dateStr : cachedActiveDates) {
            try {
                Calendar dateCal = Calendar.getInstance();
                dateCal.setTime(sdfParse.parse(dateStr));

                if (dateCal.get(Calendar.YEAR) == currentYear &&
                        dateCal.get(Calendar.MONTH) == currentMonth) {
                    activeDaysInThisMonth.add(dateCal.get(Calendar.DAY_OF_MONTH));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. Создаем список дней месяца (1..30/31)
        int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        List<Integer> daysList = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) daysList.add(i);

        // 3. Обновляем адаптер
        if (calendarAdapter != null) {
            calendarAdapter.updateData(daysList, activeDaysInThisMonth);
        }
    }

    private void setupNavigation() {
        binding.cvSleep.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.sleepFragment));
        binding.cvPulse.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.pulseFragment));
        binding.cvWeight.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.weightFragment));
        binding.cvOxygen.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.oxygenFragment));
        binding.cvDayActivity.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.dayActivityFragment));
        binding.imageButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.activeTrenFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}