package com.example.fitnesapp.ui.home;

import android.content.Intent;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Проверка авторизации
        homeViewModel.getRequireLogin().observe(getViewLifecycleOwner(), isRequired -> {
            if (isRequired) {
                Intent intent = new Intent(requireActivity(), RegisterActivity.class);
                startActivity(intent);
                requireActivity().finish();
                Toast.makeText(getContext(),"Auto auth failed, pls try again", Toast.LENGTH_SHORT).show();
            }
        });

        setupNavigation();
        setupHistoryCalendar();

        // --- Настройка Pull-to-Refresh ---
        binding.swipeRefreshHome.setColorSchemeColors(Color.parseColor("#4CAF50"), Color.parseColor("#448AFF"));

        binding.swipeRefreshHome.setOnRefreshListener(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).syncHealthData();
                Toast.makeText(getContext(), "Synchronization...", Toast.LENGTH_SHORT).show();
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (binding != null && binding.swipeRefreshHome != null) {
                    binding.swipeRefreshHome.setRefreshing(false);
                }
            }, 2000);
        });

        // --- Дневная статистика ---
        homeViewModel.getDailyData().observe(getViewLifecycleOwner(), dailyData -> {
            if (dailyData != null) updateDashboardWithRealData(dailyData);
        });

        // --- Профиль (Имя) ---
        homeViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                if (binding.tvName != null) binding.tvName.setText(profile.firstName);
                if (binding.tvSecondName != null) binding.tvSecondName.setText(profile.secondName);
            }
        });

        // --- График Пульса ---
        homeViewModel.getPulseHistory().observe(getViewLifecycleOwner(), pulseList -> {
            setupPulseChart(pulseList);
        });

        // --- Текст Пульса ---
        homeViewModel.getTodayPulseValue().observe(getViewLifecycleOwner(), currentPulse -> {
            if (currentPulse != null) binding.tvPulse.setText(currentPulse + " bpm");
            else binding.tvPulse.setText("-- bpm");
        });

        // --- График Веса + Текст ---
        homeViewModel.getWeightHistory().observe(getViewLifecycleOwner(), weightList -> {
            if (weightList != null && !weightList.isEmpty()) {
                Collections.sort(weightList, (o1, o2) -> {
                    if (o1.date == null) return -1;
                    if (o2.date == null) return 1;
                    return o1.date.compareTo(o2.date);
                });

                setupWeightChart(weightList);
                WeightHistoryItem latestWeight = weightList.get(weightList.size() - 1);
                binding.tvWeight.setText(String.format(Locale.US, "%.1f kg", latestWeight.val));
            } else {
                binding.tvWeight.setText("-- kg");
                setupWeightChart(new ArrayList<>());
            }
        });

        // --- КАЛЕНДАРЬ: Тренировки (Фиолетовый фон) ---
        homeViewModel.getActiveDays().observe(getViewLifecycleOwner(), dateStrings -> {
            if (dateStrings != null) {
                cachedActiveDates = dateStrings;
                updateCalendarDisplay();
            }
        });

        // --- КАЛЕНДАРЬ: Кольца прогресса ---
        homeViewModel.getDailyProgressMap().observe(getViewLifecycleOwner(), map -> {
            if (map != null) {
                cachedProgressMap = map;
                updateCalendarDisplay();
            }
        });

        return root;
    }

    private void updateDashboardWithRealData(DailyData data) {
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
        binding.tvGoalPercent.setText(Math.round(finalPercentVal) + "%");

        if (binding.tvCaloriesValue != null) binding.tvCaloriesValue.setText(String.valueOf((int) caloriesCurrent));
        if (binding.tvCaloriesGoal != null) binding.tvCaloriesGoal.setText("/" + (int) caloriesGoal + getString(R.string.short_text_calories));

        if (binding.tvStepsValue != null) binding.tvStepsValue.setText(String.valueOf((int) stepsCurrent));
        if (binding.tvStepsGoal != null) binding.tvStepsGoal.setText("/" + (int) stepsGoal + getString(R.string.short_text_steps));

        if (binding.tvNutritionValue != null) binding.tvNutritionValue.setText(String.valueOf((int) nutritionCurrent));
        if (binding.tvNutritionGoal != null) binding.tvNutritionGoal.setText("/" + (int) nutritionGoal + getString(R.string.short_text_calories));

        // Сон и Кислород
        if (data.oxygen != null && !data.oxygen.isEmpty()) {
            HealthLogItem latestOxygen = null;
            for (HealthLogItem item : data.oxygen.values()) {
                if (latestOxygen == null || item.time > latestOxygen.time) {
                    latestOxygen = item;
                }
            }
            if (latestOxygen != null) {
                binding.tvOxygen.setText(latestOxygen.val + "%");
                if (latestOxygen.val >= 95) {
                    binding.tvOxygenStatus.setText("Good");
                    binding.tvOxygenStatus.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    binding.tvOxygenStatus.setText("Low");
                    binding.tvOxygenStatus.setTextColor(Color.RED);
                }
            }
        } else {
            if (data.vitals_summary != null && data.vitals_summary.spo2_avg > 0) {
                binding.tvOxygen.setText(data.vitals_summary.spo2_avg + "%");
            } else {
                binding.tvOxygen.setText("-- %");
            }
        }

        if (data.sleep != null && data.sleep.durationMinutes > 0) {
            int hours = data.sleep.durationMinutes / 60;
            int mins = data.sleep.durationMinutes % 60;
            binding.tvSleepTime.setText(hours + "h " + mins + "m");

            if (data.sleep.quality != null && !data.sleep.quality.isEmpty()) {
                binding.tvSleepStatus.setText(data.sleep.quality);
                if (data.sleep.score >= 80) binding.tvSleepStatus.setTextColor(Color.parseColor("#4CAF50"));
                else if (data.sleep.score >= 50) binding.tvSleepStatus.setTextColor(Color.parseColor("#448AFF"));
                else if (data.sleep.score > 0) binding.tvSleepStatus.setTextColor(Color.parseColor("#F44336"));
                else binding.tvSleepStatus.setTextColor(Color.parseColor("#B0BEC5"));
            } else {
                binding.tvSleepStatus.setText("--");
                binding.tvSleepStatus.setTextColor(Color.parseColor("#B0BEC5"));
            }
        } else {
            binding.tvSleepTime.setText("-- h -- m");
            if (binding.tvSleepStatus != null) {
                binding.tvSleepStatus.setText("--");
                binding.tvSleepStatus.setTextColor(Color.parseColor("#B0BEC5"));
            }
        }
    }

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

    // === КАЛЕНДАРЬ ===
    private void setupHistoryCalendar() {
        binding.recyclerCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        // Передаем 3 параметра: Дни месяца, Дни с тренировками, Прогресс колец
        calendarAdapter = new CalendarAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), new HashMap<>(), (day, hasWorkout) -> {

            // Если в этот день была тренировка (фиолетовый фон), показываем ее
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
                        bundle.putInt("duration", workout.durationMin);
                        bundle.putLong("timestamp", workout.timestamp);
                        bundle.putBoolean("isHistory", true);

                        Navigation.findNavController(requireView()).navigate(R.id.activeTrenFragment, bundle);
                    }

                    @Override
                    public void onNoWorkout() {
                        Toast.makeText(getContext(), "В этот день тренировок не было", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "В этот день тренировок не было", Toast.LENGTH_SHORT).show();
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
        SimpleDateFormat sdfTitle = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        binding.tvMonthName.setText(sdfTitle.format(currentCalendar.getTime()));

        List<Integer> activeDaysInThisMonth = new ArrayList<>(); // Дни с тренировками
        Map<Integer, Integer> progressInThisMonth = new HashMap<>(); // Кольца

        SimpleDateFormat sdfParse = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        // 1. Извлекаем дни с ТРЕНИРОВКАМИ
        for (String dateStr : cachedActiveDates) {
            try {
                Calendar dateCal = Calendar.getInstance();
                dateCal.setTime(sdfParse.parse(dateStr));
                if (dateCal.get(Calendar.YEAR) == currentYear && dateCal.get(Calendar.MONTH) == currentMonth) {
                    activeDaysInThisMonth.add(dateCal.get(Calendar.DAY_OF_MONTH));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. Извлекаем дни с ПРОГРЕССОМ КОЛЕЦ
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

        // Передаем все в адаптер
        if (calendarAdapter != null) {
            calendarAdapter.updateData(daysList, activeDaysInThisMonth, progressInThisMonth);
        }
    }

    private void setupNavigation() {
        binding.cvSleep.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.sleepFragment));
        binding.cvPulse.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.pulseFragment));
        binding.cvWeight.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.weightFragment));
        binding.cvOxygen.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.oxygenFragment));
        binding.cvDayActivity.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.dayActivityFragment));
        binding.IVwater.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.water));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}