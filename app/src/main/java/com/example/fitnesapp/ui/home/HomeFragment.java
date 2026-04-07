package com.example.fitnesapp.ui.home;

import android.content.Intent;
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
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.fitnesapp.Adapters.CalendarAdapter;
import com.example.fitnesapp.MainActivity;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentHomeBinding;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.firebase.WeightHistoryItem;
import com.example.fitnesapp.models.firebase.WorkoutItem;
import com.example.fitnesapp.ui.auth.RegisterActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private CalendarAdapter calendarAdapter;

    // Календарь
    private final Calendar currentCalendar = Calendar.getInstance();

    // Храним оба списка:
    private List<String> cachedActiveDates = new ArrayList<>(); // Дни с тренировками
    private Map<String, Integer> cachedProgressMap = new HashMap<>(); // Проценты колец

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupNavigation();
        setupHistoryCalendar();
        setupSwipeRefresh();
        observeViewModelData();
    }

    // ==========================================
    // 1. ИНИЦИАЛИЗАЦИЯ И НАБЛЮДЕНИЕ ЗА ДАННЫМИ
    // ==========================================
    private void observeViewModelData() {
        // Проверка авторизации
        homeViewModel.getRequireLogin().observe(getViewLifecycleOwner(), isRequired -> {
            if (isRequired) {
                Intent intent = new Intent(requireActivity(), RegisterActivity.class);
                startActivity(intent);
                requireActivity().finish();
                Toast.makeText(getContext(), getString(R.string.home_auth_failed), Toast.LENGTH_SHORT).show();
            }
        });

        // Дневная статистика
        homeViewModel.getDailyData().observe(getViewLifecycleOwner(), dailyData -> {
            if (dailyData != null) updateDashboardWithRealData(dailyData);
        });

        // Профиль (Имя)
        homeViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                if (binding.tvName != null) binding.tvName.setText(profile.firstName);
                if (binding.tvSecondName != null) binding.tvSecondName.setText(profile.secondName);
            }
        });

        // График Пульса
        homeViewModel.getPulseHistory().observe(getViewLifecycleOwner(), pulseList -> {
            setupPulseChart(pulseList);
        });

        // Текст Пульса
        homeViewModel.getTodayPulseValue().observe(getViewLifecycleOwner(), currentPulse -> {
            if (currentPulse != null) {
                binding.tvPulse.setText(getString(R.string.format_bpm, String.valueOf(currentPulse)));
            } else {
                binding.tvPulse.setText(getString(R.string.format_bpm_empty));
            }
        });

        // График Веса + Текст
        homeViewModel.getWeightHistory().observe(getViewLifecycleOwner(), weightList -> {
            if (weightList != null && !weightList.isEmpty()) {
                Collections.sort(weightList, (o1, o2) -> {
                    if (o1.date == null) return -1;
                    if (o2.date == null) return 1;
                    return o1.date.compareTo(o2.date);
                });

                setupWeightChart(weightList);
                WeightHistoryItem latestWeight = weightList.get(weightList.size() - 1);
                binding.tvWeight.setText(getString(R.string.format_kg, latestWeight.val));
            } else {
                binding.tvWeight.setText(getString(R.string.format_kg_empty));
                setupWeightChart(new ArrayList<>());
            }
        });

        // КАЛЕНДАРЬ: Тренировки (Фиолетовый фон)
        homeViewModel.getActiveDays().observe(getViewLifecycleOwner(), dateStrings -> {
            if (dateStrings != null) {
                cachedActiveDates = dateStrings;
                updateCalendarDisplay();
            }
        });

        // КАЛЕНДАРЬ: Кольца прогресса
        homeViewModel.getDailyProgressMap().observe(getViewLifecycleOwner(), map -> {
            if (map != null) {
                cachedProgressMap = map;
                updateCalendarDisplay();
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeColors(Color.parseColor("#4CAF50"), Color.parseColor("#448AFF"));
        binding.swipeRefreshHome.setOnRefreshListener(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).syncHealthData();
                Toast.makeText(getContext(), getString(R.string.home_syncing), Toast.LENGTH_SHORT).show();
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (binding != null && binding.swipeRefreshHome != null) {
                    binding.swipeRefreshHome.setRefreshing(false);
                }
            }, 2000);
        });
    }

    private void setupNavigation() {
        // 1. Создаем настройки анимации (те самые 4 файла, которые мы создали в res/anim)
        NavOptions navOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)   // Анимация входа
                .setExitAnim(R.anim.slide_out_left)    // Анимация выхода
                .setPopEnterAnim(R.anim.slide_in_left) // Возврат (кнопка Назад)
                .setPopExitAnim(R.anim.slide_out_right)// Выход нового (кнопка Назад)
                .build();

        // 2. Передаем эти настройки третьим параметром в метод navigate()
        binding.cvSleep.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.sleepFragment, null, navOptions));

        binding.cvPulse.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.pulseFragment, null, navOptions));

        binding.cvWeight.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.weightFragment, null, navOptions));

        binding.cvOxygen.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.oxygenFragment, null, navOptions));

        binding.cvDayActivity.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.dayActivityFragment, null, navOptions));

        binding.IVwater.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.water, null, navOptions));
    }

    // ==========================================
    // 2. ОБНОВЛЕНИЕ ГЛАВНОГО ДАШБОРДА (DailyData)
    // ==========================================
    private void updateDashboardWithRealData(DailyData data) {
        updateRingsAndTexts(data);
        updateOxygen(data);
        updateSleep(data);
    }

    private void updateRingsAndTexts(DailyData data) {
        float caloriesGoal = data.caloriesGoal;
        float stepsGoal = data.stepsGoal;
        float nutritionGoal = (data.nutrition != null) ? data.nutrition.maxCalories : 0f;

        float caloriesCurrent = data.caloriesBurned;
        float stepsCurrent = data.steps;
        float nutritionCurrent = (data.nutrition != null) ? data.nutrition.totalCalories : 0f;

        float totalPercent = 0f;
        int activeGoalsCount = 0;

        if (caloriesGoal > 0) {
            totalPercent += Math.min(caloriesCurrent / caloriesGoal, 1f);
            activeGoalsCount++;
        }
        if (stepsGoal > 0) {
            totalPercent += Math.min(stepsCurrent / stepsGoal, 1f);
            activeGoalsCount++;
        }
        if (nutritionGoal > 0) {
            totalPercent += Math.min(nutritionCurrent / nutritionGoal, 1f);
            activeGoalsCount++;
        }

        float finalPercentVal = 0f;
        if (activeGoalsCount > 0) {
            finalPercentVal = (totalPercent / (float) activeGoalsCount) * 100f;
        }

        binding.progressCalories.setProgressMax(100f);
        if (!homeViewModel.isDashboardAnimated()) {
            binding.progressCalories.setProgressWithAnimation(finalPercentVal, 900L);
            homeViewModel.setDashboardAnimated(true);
        } else {
            binding.progressCalories.setProgress(finalPercentVal);
        }
        binding.tvGoalPercent.setText(getString(R.string.format_percent, Math.round(finalPercentVal)));

        if (binding.tvCaloriesValue != null) binding.tvCaloriesValue.setText(String.valueOf((int) caloriesCurrent));
        if (binding.tvCaloriesGoal != null) binding.tvCaloriesGoal.setText("/" + (int) caloriesGoal + " " + getString(R.string.short_text_calories));

        if (binding.tvStepsValue != null) binding.tvStepsValue.setText(String.valueOf((int) stepsCurrent));
        if (binding.tvStepsGoal != null) binding.tvStepsGoal.setText("/" + (int) stepsGoal + " " + getString(R.string.short_text_steps));

        if (binding.tvNutritionValue != null) binding.tvNutritionValue.setText(String.valueOf((int) nutritionCurrent));
        if (binding.tvNutritionGoal != null) binding.tvNutritionGoal.setText("/" + (int) nutritionGoal + " " + getString(R.string.short_text_calories));
    }

    private void updateOxygen(DailyData data) {
        if (data.vitals_summary != null && data.vitals_summary.spo2_avg > 0) {
            double avgOxygen = data.vitals_summary.spo2_avg;
            binding.tvOxygen.setText(String.format(Locale.US, "%.1f", avgOxygen) + "%");
        }
    }
    private void updateSleep(DailyData data) {
        if (data.sleep != null && data.sleep.durationMinutes > 0) {
            int hours = data.sleep.durationMinutes / 60;
            int mins = data.sleep.durationMinutes % 60;
            binding.tvSleepTime.setText(getString(R.string.format_sleep_time, hours, mins));
        } else {
            binding.tvSleepTime.setText(getString(R.string.format_sleep_time_empty));
        }
    }

    // ==========================================
    // 3. ГРАФИКИ
    // ==========================================
    private void setupPulseChart(List<HealthLogItem> allData) {
        BarChart chart = binding.chartPulse;
        if (chart == null) return;
        ArrayList<BarEntry> entries = new ArrayList<>();

        if (allData == null || allData.isEmpty()) {
            for (int i = 0; i < 5; i++) entries.add(new BarEntry(i, 0f));
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            long endOfDay = cal.getTimeInMillis();

            List<HealthLogItem> todaysData = new ArrayList<>();
            for (HealthLogItem item : allData) {
                if (item.time >= startOfDay && item.time <= endOfDay) {
                    todaysData.add(item);
                }
            }

            if (todaysData.isEmpty()) {
                for (int i = 0; i < 5; i++) entries.add(new BarEntry(i, 0f));
            } else {
                Collections.sort(todaysData, (o1, o2) -> Long.compare(o1.time, o2.time));
                for (int i = 0; i < todaysData.size(); i++) {
                    entries.add(new BarEntry(i, (float) todaysData.get(i).val));
                }
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#FF5252"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();
    }

    private void setupWeightChart(List<WeightHistoryItem> dataValues) {
        LineChart chart = binding.chartWeight;
        if (chart == null) return;
        ArrayList<Entry> entries = new ArrayList<>();

        if (dataValues == null || dataValues.isEmpty()) {
            for (int i = 0; i < 5; i++) entries.add(new Entry(i, 0f));
        } else {
            for (int i = 0; i < dataValues.size(); i++) entries.add(new Entry(i, (float) dataValues.get(i).val));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(2f);
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setDrawFilled(false);

        LineData data = new LineData(dataSet);
        chart.setData(data);
        simplifyChart(chart);

        if (dataValues == null || dataValues.isEmpty()) {
            chart.getAxisLeft().setAxisMinimum(-5f);
            chart.getAxisLeft().setAxisMaximum(10f);
        }
        chart.invalidate();
    }

    private void simplifyChart(Chart<?> chart) {
        chart.setTouchEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);

        YAxis leftAxis = null;
        if (chart instanceof BarChart) {
            leftAxis = ((BarChart) chart).getAxisLeft();
            ((BarChart) chart).getAxisRight().setEnabled(false);
        } else if (chart instanceof LineChart) {
            leftAxis = ((LineChart) chart).getAxisLeft();
            ((LineChart) chart).getAxisRight().setEnabled(false);
        }

        if (leftAxis != null) {
            leftAxis.setEnabled(true);
            leftAxis.setDrawLabels(false);
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(false);
            leftAxis.setSpaceBottom(15f);
            leftAxis.setSpaceTop(15f);
        }
    }

    // ==========================================
    // 4. КАЛЕНДАРЬ
    // ==========================================
    private void setupHistoryCalendar() {
        binding.recyclerCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarAdapter = new CalendarAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), new HashMap<>(), (day, hasWorkout) -> {
            if (hasWorkout) {
                Calendar clickCal = (Calendar) currentCalendar.clone();
                clickCal.set(Calendar.DAY_OF_MONTH, day);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                String selectedDate = sdf.format(clickCal.getTime());

                homeViewModel.getWorkoutForDate(selectedDate, new HomeViewModel.OnWorkoutCheckListener() {
                    @Override
                    public void onWorkoutFound(WorkoutItem workout) {
                        Bundle bundle = new Bundle();
                        bundle.putString("type", workout.type);
                        bundle.putInt("calories", workout.calories);
                        bundle.putLong("duration", workout.durationSeconds);
                        bundle.putLong("timestamp", workout.timestamp);
                        bundle.putBoolean("isHistory", true);

                        Navigation.findNavController(requireView()).navigate(R.id.activeTrenFragment, bundle);
                    }

                    @Override
                    public void onNoWorkout() {
                        Toast.makeText(getContext(), getString(R.string.home_no_workout), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), getString(R.string.home_no_workout), Toast.LENGTH_SHORT).show();
            }
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

    private void updateCalendarDisplay() {
        // Оставляем Locale.ENGLISH, если названия месяцев (January, February) должны оставаться на английском везде.
        // Если хочешь чтобы переводились - поменяй Locale.ENGLISH на Locale.getDefault()
        SimpleDateFormat sdfTitle = new SimpleDateFormat("MMMM", Locale.getDefault());
        binding.tvMonthName.setText(sdfTitle.format(currentCalendar.getTime()));

        List<Integer> activeDaysInThisMonth = new ArrayList<>();
        Map<Integer, Integer> progressInThisMonth = new HashMap<>();

        SimpleDateFormat sdfParse = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        for (String dateStr : cachedActiveDates) {
            try {
                Calendar dateCal = Calendar.getInstance();
                dateCal.setTime(sdfParse.parse(dateStr));
                if (dateCal.get(Calendar.YEAR) == currentYear && dateCal.get(Calendar.MONTH) == currentMonth) {
                    activeDaysInThisMonth.add(dateCal.get(Calendar.DAY_OF_MONTH));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        for (Map.Entry<String, Integer> entry : cachedProgressMap.entrySet()) {
            try {
                Calendar dateCal = Calendar.getInstance();
                dateCal.setTime(sdfParse.parse(entry.getKey()));
                if (dateCal.get(Calendar.YEAR) == currentYear && dateCal.get(Calendar.MONTH) == currentMonth) {
                    progressInThisMonth.put(dateCal.get(Calendar.DAY_OF_MONTH), entry.getValue());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        List<Integer> daysList = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) daysList.add(i);

        if (calendarAdapter != null) {
            calendarAdapter.updateData(daysList, activeDaysInThisMonth, progressInThisMonth);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}