package com.example.fitnesapp.ui.dayactivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.NutritionAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentDayActivityBinding;
import com.example.fitnesapp.models.Nutrition;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.HourlyActivityItem;
import com.example.fitnesapp.models.firebase.MealItem;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.ui.base.BaseLoadingFragment;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.data.Entry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;

public class DayActivityFragment extends BaseLoadingFragment {

    private DayActivityViewModel mViewModel;
    private FragmentDayActivityBinding binding;

    // Храним текущую выбранную дату (по умолчанию - сегодня)
    private Date selectedDate = new Date();

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

        // 1. Наблюдаем за данными профиля
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);

                if (mViewModel.getDailyData().getValue() != null) {
                    updateCharts(mViewModel.getDailyData().getValue());
                }
            }
        });

        // 2. Наблюдаем за данными дня
        mViewModel.getDailyData().observe(getViewLifecycleOwner(), this::updateUI);

        // 3. ОБРАБОТЧИКИ НАЖАТИЙ (Изменение целей)
        setupClickListeners();
        startFakeLoading(view, 200);

        // --- ЗАПУСК ОБУЧЕНИЯ (ЛОВИМ ШАГ 1) ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        if (prefs.getInt("tutorial_step", 0) == 1) {
            view.postDelayed(this::showTutorial, 600);
        }
    }

    // Вспомогательный метод для склейки заголовка и текста из ресурсов
    private String getTutorialText(int titleResId, int descResId) {
        return getString(titleResId) + "\n\n" + getString(descResId);
    }

    // ==========================================
    // ОБУЧЕНИЕ НА ЭКРАНЕ АКТИВНОСТИ
    // ==========================================
    private void showTutorial() {
        if (binding == null) return; // Защита, если пользователь успел закрыть экран

        // Слайд 1: Приветствие экрана
        FancyShowCaseView step1 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_day_stat_title, R.string.tutorial_day_stat_text))
                .titleStyle(0, Gravity.CENTER)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 2: История/Календарь
        FancyShowCaseView step2 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.recyclerViewDayActivity)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .title(getTutorialText(R.string.tutorial_day_history_title, R.string.tutorial_day_history_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 3: Изменение целей
        FancyShowCaseView step3 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.tvStepsGoal)
                .focusShape(FocusShape.CIRCLE)
                .fitSystemWindows(true)
                .roundRectRadius(20)
                .title(getTutorialText(R.string.tutorial_day_goals_title, R.string.tutorial_day_goals_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 4: Графики
        FancyShowCaseView step4 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.chartStepsinfo)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .roundRectRadius(30)
                .title(getTutorialText(R.string.tutorial_day_charts_title, R.string.tutorial_day_charts_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Собираем всё в очередь
        FancyShowCaseQueue queue = new FancyShowCaseQueue()
                .add(step1)
                .add(step2)
                .add(step3)
                .add(step4);

        queue.setCompleteListener(() -> {
            // Передаем эстафету дальше (Шаг 2)
            requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("tutorial_step", 2)
                    .apply();
        });

        queue.show();
    }

    private void setupClickListeners() {
        binding.tvStepsGoal.setOnClickListener(v -> showEditGoalDialog(getString(R.string.edit_goal_steps_goal), "stepsGoal"));
        binding.tvCaloriesGoal.setOnClickListener(v -> showEditGoalDialog(getString(R.string.edit_goal_burn_calories_goal), "caloriesGoal"));
        binding.tvNutritionMax.setOnClickListener(v -> showEditGoalDialog(getString(R.string.edit_goal_nutrition_max_goal), "nutrition/maxCalories"));
    }

    private void updateUI(DailyData data) {
        if (data == null) {
            clearUI();
            updateCharts(new DailyData());
            return;
        }

        binding.tvStepsScore.setText(String.valueOf(data.steps));
        binding.tvStepsGoal.setText(getString(R.string.day_activity_goal, data.stepsGoal >= 0 ? data.stepsGoal : 10000));

        binding.tvCaloriesScore.setText(String.valueOf((int) data.caloriesBurned));
        binding.tvCaloriesGoal.setText(getString(R.string.day_activity_goal, data.caloriesGoal >= 0 ? data.caloriesGoal : 2000));

        if (data.nutrition != null) {
            binding.tvNutritionScore.setText(String.valueOf((int) data.nutrition.totalCalories));
            binding.tvNutritionMax.setText(getString(R.string.day_activity_max, data.nutrition.maxCalories));
            binding.tvNutritionAllCarbs.setText(getString(R.string.day_activity_carbs_val, (int) data.nutrition.carbs));
            binding.tvNutritionAllProteins.setText(getString(R.string.day_activity_protein_val, (int) data.nutrition.protein));
            binding.tvNutritionAllFats.setText(getString(R.string.day_activity_fats_val, (int) data.nutrition.fat));
        } else {
            binding.tvNutritionScore.setText("0");
            binding.tvNutritionMax.setText(getString(R.string.day_activity_max, 2000));
            binding.tvNutritionAllCarbs.setText(getString(R.string.day_activity_carbs_val, 0));
            binding.tvNutritionAllProteins.setText(getString(R.string.day_activity_protein_val, 0));
            binding.tvNutritionAllFats.setText(getString(R.string.day_activity_fats_val, 0));
        }

        updateNutritionList(data.meals);
        updateCharts(data);
    }

    private void updateNutritionList(Map<String, MealItem> mealsMap) {
        List<Nutrition> uiList = new ArrayList<>();

        if (mealsMap != null && !mealsMap.isEmpty()) {
            List<MealItem> firebaseList = new ArrayList<>(mealsMap.values());
            Collections.sort(firebaseList, (o1, o2) -> {
                if (o1.time == null) return -1;
                if (o2.time == null) return 1;
                return o1.time.compareTo(o2.time);
            });

            for (MealItem item : firebaseList) {
                uiList.add(new Nutrition(
                        item.time != null ? item.time : "--:--",
                        String.valueOf((int) item.carbs),
                        String.valueOf((int) item.protein),
                        String.valueOf((int) item.fat),
                        String.valueOf((int) item.calories)
                ));
            }
        }

        NutritionAdapter adapter = new NutritionAdapter(getContext(), uiList);
        binding.recyclerViewNutHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewNutHistory.setAdapter(adapter);
    }

    private void updateCharts(DailyData data) {
        Map<String, HourlyActivityItem> rawData = data.hourly_activity;
        if (rawData == null) rawData = new HashMap<>();
        setup30MinCharts(rawData);
    }

    private void setup30MinCharts(Map<String, HourlyActivityItem> hourlyMap) {

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);

        Calendar selCal = Calendar.getInstance();
        selCal.setTime(selectedDate);
        selCal.set(Calendar.HOUR_OF_DAY, 0); selCal.set(Calendar.MINUTE, 0); selCal.set(Calendar.SECOND, 0); selCal.set(Calendar.MILLISECOND, 0);

        boolean isFuture = selCal.after(today);
        boolean isToday = selCal.equals(today);

        ArrayList<Entry> stepsEntries = new ArrayList<>();
        ArrayList<Entry> calEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        if (isFuture) {
            stepsEntries.add(new Entry(0, 0f));
            stepsEntries.add(new Entry(1, 0f));
            calEntries.add(new Entry(0, 0f));
            calEntries.add(new Entry(1, 0f));
            labels.add("00:00");
            labels.add("23:59");
        } else {
            int limitIndex = isToday ?
                    (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 2) + (Calendar.getInstance().get(Calendar.MINUTE) >= 30 ? 1 : 0)
                    : 47;

            float[] stepsBuckets = new float[48];
            for (HourlyActivityItem item : hourlyMap.values()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(item.time);
                int index = (cal.get(Calendar.HOUR_OF_DAY) * 2) + (cal.get(Calendar.MINUTE) >= 30 ? 1 : 0);
                if (index >= 0 && index < 48) stepsBuckets[index] += item.steps;
            }

            UserProfile profile = mViewModel.getUserProfile().getValue();
            double weight = (profile != null && profile.weight > 0) ? profile.weight : 75.0;
            double height = (profile != null && profile.height > 0) ? profile.height : 175.0;
            int age = (profile != null) ? profile.getAge() : 25;
            boolean isMale = (profile == null) || "Male".equalsIgnoreCase(profile.gender);

            double bmr = isMale ? (10 * weight) + (6.25 * height) - (5 * age) + 5 : (10 * weight) + (6.25 * height) - (5 * age) - 161;
            double bmrPer30Min = bmr / 48.0;
            double calsPerStep = 0.045;

            for (int i = 0; i <= limitIndex; i++) {
                stepsEntries.add(new Entry(i, stepsBuckets[i]));

                float activeCals = (float) (stepsBuckets[i] * calsPerStep);
                calEntries.add(new Entry(i, activeCals));

                int h = i / 2;
                int m = (i % 2) * 30;
                labels.add(String.format(Locale.US, "%02d:%02d", h, m));
            }
        }

        String[] labelsArr = labels.toArray(new String[0]);

        drawChart(binding.chartStepsinfo, stepsEntries, labelsArr, R.color.steps_start, R.color.steps_end, 0f);
        drawChart(binding.chartCaloriesnfo, calEntries, labelsArr, R.color.calories_start, R.color.calories_end, 0f);
    }

    private void drawChart(com.github.mikephil.charting.charts.LineChart chart,
                           ArrayList<Entry> entries, String[] labels, int colorStart, int colorEnd, float floorValue) {
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

        chart.invalidate();
    }

    private void showEditGoalDialog(String title, String databasePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        EditText etInput = customView.findViewById(R.id.etGoalInput);
        View btnCancel = customView.findViewById(R.id.btnCancel);
        View btnSave = customView.findViewById(R.id.btnSave);

        tvTitle.setText(title);
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newValueStr = etInput.getText().toString();
            if (!newValueStr.isEmpty()) {
                try {
                    int newValue = Integer.parseInt(newValueStr);
                    updateGoalInFirebase(databasePath, newValue);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), getString(R.string.day_activity_invalid_number), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void updateGoalInFirebase(String relativePath, int value) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateKey = sdf.format(selectedDate);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid)
                .child("daily_data")
                .child(dateKey);

        ref.child(relativePath).setValue(value)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), getString(R.string.day_activity_goal_updated), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), getString(R.string.day_activity_error_updating), Toast.LENGTH_SHORT).show());
    }

    private void clearUI() {
        binding.tvStepsScore.setText("0");
        binding.tvCaloriesScore.setText("0");
        binding.tvNutritionScore.setText("0");

        binding.tvStepsGoal.setText(getString(R.string.day_activity_goal, 10000));
        binding.tvCaloriesGoal.setText(getString(R.string.day_activity_goal, 2000));

        binding.tvNutritionMax.setText(getString(R.string.day_activity_max, 2000));
        binding.tvNutritionAllCarbs.setText(getString(R.string.day_activity_carbs_val, 0));
        binding.tvNutritionAllProteins.setText(getString(R.string.day_activity_protein_val, 0));
        binding.tvNutritionAllFats.setText(getString(R.string.day_activity_fats_val, 0));

        binding.recyclerViewNutHistory.setAdapter(null);
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewDayActivity,
                date -> {
                    selectedDate = date.getDate();
                    mViewModel.loadDataForDate(selectedDate);
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}