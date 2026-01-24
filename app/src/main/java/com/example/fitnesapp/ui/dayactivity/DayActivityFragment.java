package com.example.fitnesapp.ui.dayactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.NutritionAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentDayActivityBinding;
import com.example.fitnesapp.models.Nutrition; // Ваша UI модель для адаптера
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HourlyActivityItem;
import com.example.fitnesapp.models.firebase.MealItem;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.data.Entry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayActivityFragment extends Fragment {

    private DayActivityViewModel mViewModel;
    private FragmentDayActivityBinding binding;

    public static DayActivityFragment newInstance() {
        return new DayActivityFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDayActivityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DayActivityViewModel.class);

        setupCalendar();

        // 1. Профиль
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);
            }
        });

        // 2. Данные дня
        mViewModel.getDailyData().observe(getViewLifecycleOwner(), this::updateUI);
    }

    private void updateUI(DailyData data) {
        if (data == null) {
            clearUI();
            return;
        }

        // --- БЛОК 1: ШАГИ И КАЛОРИИ (ОБЩИЕ) ---
        binding.tvStepsScore.setText(String.valueOf(data.steps));
        binding.tvStepsGoal.setText("Goal: " + (data.stepsGoal > 0 ? data.stepsGoal : 10000));

        binding.tvCaloriesScore.setText(String.valueOf(data.caloriesBurned));
        binding.tvCaloriesGoal.setText("Goal: " + (data.caloriesGoal > 0 ? data.caloriesGoal : 2000));

        // --- БЛОК 2: ПИТАНИЕ (СВОДКА) ---
        if (data.nutrition != null) {
            binding.tvNutritionScore.setText(String.valueOf(data.nutrition.totalCalories));
            binding.tvNutritionMax.setText("Max: " + data.nutrition.maxCalories);

            binding.tvNutritionAllCarbs.setText("Carbs: " + data.nutrition.carbs + "g");
            binding.tvNutritionAllProteins.setText("Protein: " + data.nutrition.protein + "g");
            binding.tvNutritionAllFats.setText("Fats: " + data.nutrition.fat + "g");
        } else {
            binding.tvNutritionScore.setText("0");
            binding.tvNutritionMax.setText("Max: 2000");
        }

        // --- БЛОК 3: СПИСОК ЕДЫ (RECYCLER) ---
        updateNutritionList(data.meals);

        // --- БЛОК 4: ГРАФИКИ ---
        updateCharts(data);
    }

    private void updateNutritionList(java.util.Map<String, MealItem> mealsMap) {
        List<Nutrition> uiList = new ArrayList<>();

        if (mealsMap != null && !mealsMap.isEmpty()) {
            // Конвертируем из Map Firebase в List для UI
            List<MealItem> firebaseList = new ArrayList<>(mealsMap.values());

            // Сортируем по времени (08:00, 13:00)
            Collections.sort(firebaseList, (o1, o2) -> {
                if (o1.time == null) return -1;
                if (o2.time == null) return 1;
                return o1.time.compareTo(o2.time);
            });

            for (MealItem item : firebaseList) {
                // Маппинг данных в ваш UI класс Nutrition
                // Конструктор Nutrition(time, carbs, protein, fat, calories) - порядок важен!
                uiList.add(new Nutrition(
                        item.time != null ? item.time : "--:--",
                        String.valueOf(item.carbs),
                        String.valueOf(item.protein),
                        String.valueOf(item.fat),
                        String.valueOf(item.calories)
                ));
            }
        }

        NutritionAdapter adapter = new NutritionAdapter(getContext(), uiList);
        binding.recyclerViewNutHistory.setAdapter(adapter);
        binding.recyclerViewNutHistory.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void updateCharts(DailyData data) {
        // Если есть реальные почасовые данные
        if (data.hourly_activity != null && !data.hourly_activity.isEmpty()) {
            setupRealCharts(data.hourly_activity);
        } else {
            // Иначе рисуем "красивые" графики на основе общих сумм (симуляция распределения)
            setupSimulatedCharts(data.steps, data.caloriesBurned);
        }
    }

    private void setupRealCharts(java.util.Map<String, HourlyActivityItem> hourlyMap) {
        List<HourlyActivityItem> list = new ArrayList<>(hourlyMap.values());
        Collections.sort(list, (o1, o2) -> Long.compare(o1.time, o2.time));

        ArrayList<Entry> stepsEntries = new ArrayList<>();
        ArrayList<Entry> calEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);

        for (int i = 0; i < list.size(); i++) {
            HourlyActivityItem item = list.get(i);
            stepsEntries.add(new Entry(i, item.steps));
            calEntries.add(new Entry(i, item.calories));
            labels.add(sdf.format(new Date(item.time)));
        }

        String[] labelsArr = labels.toArray(new String[0]);

        drawChart(binding.chartStepsinfo, stepsEntries, labelsArr, R.color.steps_start, R.color.steps_end);
        drawChart(binding.chartCaloriesnfo, calEntries, labelsArr, R.color.calories_start, R.color.calories_end);
    }

    private void setupSimulatedCharts(int totalSteps, int totalCals) {
        // Создаем искусственный график, распределяя активность по дню
        ArrayList<Entry> sEntries = new ArrayList<>();
        ArrayList<Entry> cEntries = new ArrayList<>();

        // Проценты активности по часам (утро, обед, вечер)
        float[] distribution = {0.05f, 0.1f, 0.05f, 0.05f, 0.15f, 0.05f, 0.1f, 0.25f, 0.1f, 0.05f, 0.05f};
        String[] labels = {"07:00", "08:00", "09:00", "11:00", "13:00", "14:00", "16:00", "18:00", "19:00", "20:00", "22:00"};

        for (int i = 0; i < distribution.length; i++) {
            sEntries.add(new Entry(i, totalSteps * distribution[i]));
            cEntries.add(new Entry(i, totalCals * distribution[i]));
        }

        drawChart(binding.chartStepsinfo, sEntries, labels, R.color.steps_start, R.color.steps_end);
        drawChart(binding.chartCaloriesnfo, cEntries, labels, R.color.calories_start, R.color.calories_end);
    }

    private void drawChart(com.github.mikephil.charting.charts.LineChart chart,
                           ArrayList<Entry> entries, String[] labels, int colorStart, int colorEnd) {
        if (chart == null) return;
        ChartHelper.setupUnifiedChart(
                requireContext(),
                chart,
                entries,
                labels,
                colorStart,
                colorEnd,
                true
        );
    }

    private void clearUI() {
        binding.tvStepsScore.setText("0");
        binding.tvCaloriesScore.setText("0");
        binding.tvNutritionScore.setText("0");
        binding.tvNutritionAllCarbs.setText("Carbs: 0g");
        binding.tvNutritionAllProteins.setText("Protein: 0g");
        binding.tvNutritionAllFats.setText("Fats: 0g");
        binding.chartStepsinfo.clear();
        binding.chartCaloriesnfo.clear();
        binding.recyclerViewNutHistory.setAdapter(null);
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewDayActivity,
                date -> {
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