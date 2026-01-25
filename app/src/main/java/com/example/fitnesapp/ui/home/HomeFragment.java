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

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private CalendarAdapter calendarAdapter;

    // Календарь
    private final Calendar currentCalendar = Calendar.getInstance();
    private List<String> cachedActiveDates = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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

        // --- Дневная статистика ---
        homeViewModel.getDailyData().observe(getViewLifecycleOwner(), dailyData -> {
            if (dailyData != null) {
                updateDashboardWithRealData(dailyData);
            }
        });

        // --- Профиль ---
        homeViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                if (binding.tvName != null) binding.tvName.setText(profile.firstName);
                if (binding.tvSecondName != null) binding.tvSecondName.setText(profile.secondName);
            }
        });

        // --- График Пульса ---
        homeViewModel.getPulseHistory().observe(getViewLifecycleOwner(), pulseList -> {
            if (pulseList != null) {
                // Передаем весь список, фильтрация будет внутри метода
                setupPulseChart(pulseList);
            }
        });

        // --- График Веса ---
        homeViewModel.getWeightHistory().observe(getViewLifecycleOwner(), weightList -> {
            if (weightList != null && !weightList.isEmpty()) setupWeightChart(weightList);
        });

        // --- Календарь ---
        homeViewModel.getActiveDays().observe(getViewLifecycleOwner(), dateStrings -> {
            if (dateStrings != null) {
                cachedActiveDates = dateStrings;
                updateCalendarDisplay();
            }
        });

        return root;
    }

    private void updateDashboardWithRealData(DailyData data) {
        // --- ЧАСТЬ 1: КРУГ И ЦЕЛИ ---
        float caloriesGoal = (data.caloriesGoal > 0) ? data.caloriesGoal : 2000f;
        float stepsGoal = (data.stepsGoal > 0) ? data.stepsGoal : 10000f;
        float nutritionGoal = (data.nutrition != null && data.nutrition.maxCalories > 0)
                ? data.nutrition.maxCalories : 2000f;

        float caloriesCurrent = data.caloriesBurned;
        float stepsCurrent = data.steps;
        float nutritionCurrent = (data.nutrition != null) ? data.nutrition.totalCalories : 0;

        float calP = (caloriesGoal > 0) ? (caloriesCurrent / caloriesGoal) : 0f;
        float stepP = (stepsGoal > 0) ? (stepsCurrent / stepsGoal) : 0f;
        float nutP = (nutritionGoal > 0) ? (nutritionCurrent / nutritionGoal) : 0f;

        if (calP > 1f) calP = 1f;
        if (stepP > 1f) stepP = 1f;
        if (nutP > 1f) nutP = 1f;

        float totalPercentVal = ((calP + stepP + nutP) / 3f) * 100f;

        binding.progressCalories.setProgressMax(100f);
        binding.progressCalories.setProgressWithAnimation(totalPercentVal, 900L);
        binding.tvGoalPercent.setText(Math.round(totalPercentVal) + "%");

        if (binding.tvCaloriesValue != null) binding.tvCaloriesValue.setText(String.valueOf((int) caloriesCurrent));
        if (binding.tvCaloriesGoal != null) binding.tvCaloriesGoal.setText("/" + (int) caloriesGoal + getString(R.string.short_text_calories));

        if (binding.tvStepsValue != null) binding.tvStepsValue.setText(String.valueOf((int) stepsCurrent));
        if (binding.tvStepsGoal != null) binding.tvStepsGoal.setText("/" + (int) stepsGoal + getString(R.string.short_text_steps));

        if (binding.tvNutritionValue != null) binding.tvNutritionValue.setText(String.valueOf((int) nutritionCurrent));
        if (binding.tvNutritionGoal != null) binding.tvNutritionGoal.setText("/" + (int) nutritionGoal + getString(R.string.short_text_calories));


        // --- ЧАСТЬ 2: НИЖНИЕ КАРТОЧКИ ---

        // --- 1. ПУЛЬС (Последнее значение) ---
        // --- График Пульса + ТЕКСТ ---
        homeViewModel.getPulseHistory().observe(getViewLifecycleOwner(), pulseList -> {
            if (pulseList != null && !pulseList.isEmpty()) {
                // 1. Сортируем список по времени (от старых к новым)
                Collections.sort(pulseList, (o1, o2) -> Long.compare(o1.time, o2.time));

                // 2. Берем ПОСЛЕДНИЙ элемент (самый свежий) и ставим в текст
                HealthLogItem lastItem = pulseList.get(pulseList.size() - 1);
                binding.tvPulse.setText((int) lastItem.val + " bpm");

                // 3. Строим график (он сам отфильтрует данные за сегодня внутри метода)
                setupPulseChart(pulseList);
            } else {
                binding.tvPulse.setText("-- bpm");
                if (binding.chartPulse != null) binding.chartPulse.clear();
            }
        });

        // --- 2. КИСЛОРОД (Последнее значение) ---
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

        // --- 3. ВЕС (Последнее значение) ---
        if (data.weight_history != null && !data.weight_history.isEmpty()) {
            WeightHistoryItem latestWeight = null;
            for (WeightHistoryItem item : data.weight_history.values()) {
                if (latestWeight == null || (item.date != null && item.date.compareTo(latestWeight.date) > 0)) {
                    latestWeight = item;
                }
            }
            if (latestWeight != null) {
                binding.tvWeight.setText(String.format(Locale.US, "%.1f kg", latestWeight.val));
            }
        } else {
            if (data.vitals_summary != null && data.vitals_summary.weight_today > 0) {
                binding.tvWeight.setText(data.vitals_summary.weight_today + " kg");
            } else {
                binding.tvWeight.setText("-- kg");
            }
        }

        // --- 4. СОН ---
        if (data.sleep != null && data.sleep.durationMinutes > 0) {
            int hours = data.sleep.durationMinutes / 60;
            int mins = data.sleep.durationMinutes % 60;
            binding.tvSleepTime.setText(hours + "h " + mins + "m");
            if (data.sleep.quality != null && !data.sleep.quality.isEmpty()) {
                binding.tvSleepStatus.setText(data.sleep.quality);
            }
        } else {
            binding.tvSleepTime.setText("-- h -- m");
        }
    }

    // === ГРАФИКИ ===

    // ИСПРАВЛЕННЫЙ МЕТОД: Фильтрует данные только за СЕГОДНЯ
    private void setupPulseChart(List<HealthLogItem> allData) {
        BarChart chart = binding.chartPulse;
        if (chart == null) return;

        if (allData == null || allData.isEmpty()) {
            chart.clear();
            return;
        }

        // 1. Определяем начало и конец СЕГОДНЯШНЕГО дня
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

        // 2. Фильтруем список: оставляем только данные за сегодня
        List<HealthLogItem> todaysData = new ArrayList<>();
        for (HealthLogItem item : allData) {
            if (item.time >= startOfDay && item.time <= endOfDay) {
                todaysData.add(item);
            }
        }

        // Если данных за сегодня нет, очищаем график
        if (todaysData.isEmpty()) {
            chart.clear();
            return;
        }

        // 3. Сортируем по времени (на всякий случай)
        Collections.sort(todaysData, (o1, o2) -> Long.compare(o1.time, o2.time));

        // 4. Заполняем график
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < todaysData.size(); i++) {
            // X = индекс, Y = значение пульса
            entries.add(new BarEntry(i, (float) todaysData.get(i).val));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#FF5252"));
        dataSet.setDrawValues(false); // Не показывать цифры над столбиками

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();
    }

    private void setupWeightChart(List<WeightHistoryItem> dataValues) {
        LineChart chart = binding.chartWeight;
        if (chart == null || dataValues == null || dataValues.isEmpty()) return;

        Collections.sort(dataValues, (o1, o2) -> {
            if (o1.date == null) return -1;
            if (o2.date == null) return 1;
            return o1.date.compareTo(o2.date);
        });

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dataValues.size(); i++) {
            entries.add(new Entry(i, (float) dataValues.get(i).val));
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
        chart.invalidate();
    }

    private void simplifyChart(Chart<?> chart) {
        chart.setTouchEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);

        if (chart instanceof BarChart) {
            ((BarChart) chart).getAxisLeft().setEnabled(false);
            ((BarChart) chart).getAxisRight().setEnabled(false);
            ((BarChart) chart).getAxisLeft().setDrawGridLines(false);
        } else if (chart instanceof LineChart) {
            ((LineChart) chart).getAxisLeft().setEnabled(false);
            ((LineChart) chart).getAxisRight().setEnabled(false);
            ((LineChart) chart).getAxisLeft().setDrawGridLines(false);
        }
    }

    // === КАЛЕНДАРЬ ===
    private void setupHistoryCalendar() {
        binding.recyclerCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarAdapter = new CalendarAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), (day, isActive) -> {
            // 1. Формируем правильную дату из нажатого дня и текущего месяца календаря
            Calendar clickCal = (Calendar) currentCalendar.clone();
            clickCal.set(Calendar.DAY_OF_MONTH, day);

            // Важно: формат должен совпадать с ключами в Firebase ("yyyy-MM-dd")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String selectedDate = sdf.format(clickCal.getTime());

            // 2. Спрашиваем у ViewModel данные за этот день
            homeViewModel.getWorkoutForDate(selectedDate, new HomeViewModel.OnWorkoutCheckListener() {
                @Override
                public void onWorkoutFound(WorkoutItem workout) {
                    // 3. Если тренировка есть - собираем данные в Bundle и переходим
                    Bundle bundle = new Bundle();
                    bundle.putString("type", workout.type);
                    bundle.putInt("calories", workout.calories);
                    bundle.putInt("duration", workout.durationMin);
                    bundle.putLong("timestamp", workout.timestamp);
                    bundle.putBoolean("isHistory", true); // Флаг, что это история, а не лайв

                    Navigation.findNavController(requireView())
                            .navigate(R.id.activeTrenFragment, bundle);
                }

                @Override
                public void onNoWorkout() {
                    // 4. Если тренировки нет - тост
                    Toast.makeText(getContext(), "В этот день тренировок не было", Toast.LENGTH_SHORT).show();
                }
            });
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

        int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        List<Integer> daysList = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) daysList.add(i);

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}