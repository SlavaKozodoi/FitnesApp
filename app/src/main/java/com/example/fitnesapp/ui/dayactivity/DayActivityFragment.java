package com.example.fitnesapp.ui.dayactivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
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
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.data.Entry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        // 1. Наблюдаем за данными профиля
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);

                // Если данные дня уже загружены, обновляем графики с учетом веса/возраста из профиля
                if (mViewModel.getDailyData().getValue() != null) {
                    updateCharts(mViewModel.getDailyData().getValue());
                }
            }
        });

        // 2. Наблюдаем за данными дня
        mViewModel.getDailyData().observe(getViewLifecycleOwner(), this::updateUI);

        // 3. ОБРАБОТЧИКИ НАЖАТИЙ (Изменение целей)
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Изменение цели шагов
        binding.tvStepsGoal.setOnClickListener(v -> {
            showEditGoalDialog("Steps Goal", "stepsGoal");
        });

        // Изменение цели калорий
        binding.tvCaloriesGoal.setOnClickListener(v -> {
            showEditGoalDialog("Burn Calories Goal", "caloriesGoal");
        });

        // Изменение цели по еде (Максимум калорий)
        binding.tvNutritionMax.setOnClickListener(v -> {
            showEditGoalDialog("Nutrition Max Goal", "nutrition/maxCalories");
        });
    }

    private void updateUI(DailyData data) {
        if (data == null) {
            clearUI();
            return;
        }

        // --- БЛОК 1: ШАГИ И КАЛОРИИ ---
        binding.tvStepsScore.setText(String.valueOf(data.steps));
        binding.tvStepsGoal.setText("Goal: " + (data.stepsGoal >= 0 ? data.stepsGoal : 10000));

        binding.tvCaloriesScore.setText(String.valueOf(data.caloriesBurned));
        binding.tvCaloriesGoal.setText("Goal: " + (data.caloriesGoal >= 0 ? data.caloriesGoal : 2000));

        // --- БЛОК 2: ПИТАНИЕ ---
        if (data.nutrition != null) {
            binding.tvNutritionScore.setText(String.valueOf(data.nutrition.totalCalories));
            binding.tvNutritionMax.setText("Max: " + data.nutrition.maxCalories);

            binding.tvNutritionAllCarbs.setText("Carbs: " + data.nutrition.carbs + "g");
            binding.tvNutritionAllProteins.setText("Protein: " + data.nutrition.protein + "g");
            binding.tvNutritionAllFats.setText("Fats: " + data.nutrition.fat + "g");
        } else {
            binding.tvNutritionScore.setText("0");
            binding.tvNutritionMax.setText("Max: 2000");
            binding.tvNutritionAllCarbs.setText("Carbs: 0g");
            binding.tvNutritionAllProteins.setText("Protein: 0g");
            binding.tvNutritionAllFats.setText("Fats: 0g");
        }

        // --- БЛОК 3: СПИСОК ЕДЫ ---
        updateNutritionList(data.meals);

        // --- БЛОК 4: ГРАФИКИ ---
        updateCharts(data);
    }

    private void updateNutritionList(Map<String, MealItem> mealsMap) {
        List<Nutrition> uiList = new ArrayList<>();

        if (mealsMap != null && !mealsMap.isEmpty()) {
            List<MealItem> firebaseList = new ArrayList<>(mealsMap.values());

            // Сортировка по времени
            Collections.sort(firebaseList, (o1, o2) -> {
                if (o1.time == null) return -1;
                if (o2.time == null) return 1;
                return o1.time.compareTo(o2.time);
            });

            for (MealItem item : firebaseList) {
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
        binding.recyclerViewNutHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewNutHistory.setAdapter(adapter);
    }

    // --- ГРАФИКИ ---
    private void updateCharts(DailyData data) {
        Map<String, HourlyActivityItem> rawData = data.hourly_activity;
        if (rawData == null) rawData = new HashMap<>();
        setup30MinCharts(rawData);
    }

    private void setup30MinCharts(Map<String, HourlyActivityItem> hourlyMap) {
        // 1. Подготовка корзин (48 интервалов по 30 мин)
        float[] stepsBuckets = new float[48];
        long dataTimestamp = 0; // Для проверки даты

        // 2. Раскладываем ШАГИ по корзинам
        for (HourlyActivityItem item : hourlyMap.values()) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(item.time);

            // Сохраняем время любой записи, чтобы понять, какой это день
            if (dataTimestamp == 0) dataTimestamp = item.time;

            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int index = (hour * 2) + (minute >= 30 ? 1 : 0);

            if (index >= 0 && index < 48) {
                stepsBuckets[index] += item.steps;
            }
        }

        // ============================================================
        // 3. ОПРЕДЕЛЯЕМ: ЭТО СЕГОДНЯ ИЛИ ИСТОРИЯ?
        // ============================================================
        boolean isToday;
        if (dataTimestamp > 0) {
            // Если данные есть, проверяем их дату
            isToday = android.text.format.DateUtils.isToday(dataTimestamp);
        } else {
            // Если данных нет (пустой день), считаем по логике:
            // Если мы пришли с календаря, activity знает дату, но здесь мы упростим:
            // Если пусто, нарисуем до текущего момента (безопасный вариант)
            isToday = true;
        }

        // Определяем индекс обрезки графика
        int limitIndex;
        if (isToday) {
            // Если СЕГОДНЯ: рисуем до текущего времени
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            limitIndex = (currentHour * 2) + (currentMinute >= 30 ? 1 : 0);
        } else {
            // Если ПРОШЛОЕ: рисуем весь день до конца (23:30 = индекс 47)
            limitIndex = 47;
        }

        // ============================================================
        // 4. РАСЧЕТ КАЛОРИЙ ВРУЧНУЮ (BMR + Activity)
        // ============================================================

        UserProfile profile = mViewModel.getUserProfile().getValue();

        double weight = (profile != null && profile.weight > 0) ? profile.weight : 75.0;
        double height = (profile != null && profile.height > 0) ? profile.height : 175.0;
        int age = (profile != null) ? profile.getAge() : 25;
        boolean isMale = (profile == null) || "Male".equalsIgnoreCase(profile.gender);

        double bmr;
        if (isMale) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }

        double bmrPer30Min = bmr / 48.0;
        double calsPerStep = 0.045;

        float[] calculatedCals = new float[48];

        for (int i = 0; i < 48; i++) {
            calculatedCals[i] = (float) (bmrPer30Min + (stepsBuckets[i] * calsPerStep));
        }

        // ============================================================
        // 5. ФОРМИРУЕМ ДАННЫЕ ДЛЯ ОТРИСОВКИ
        // ============================================================

        ArrayList<Entry> stepsEntries = new ArrayList<>();
        ArrayList<Entry> calEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Цикл идет от 00:00 до limitIndex
        // Если сегодня - до сейчас. Если вчера - до 23:30.
        for (int i = 0; i <= limitIndex; i++) {
            stepsEntries.add(new Entry(i, stepsBuckets[i]));
            calEntries.add(new Entry(i, calculatedCals[i]));

            int h = i / 2;
            int m = (i % 2) * 30;
            String timeLabel = String.format(Locale.US, "%02d:%02d", h, m);
            labels.add(timeLabel);
        }

        // 6. Отрисовка
        String[] labelsArr = labels.toArray(new String[0]);

        if (stepsEntries.isEmpty()) {
            stepsEntries.add(new Entry(0, 0));
            calEntries.add(new Entry(0, (float) bmrPer30Min));
            labelsArr = new String[]{"00:00"};
        }

        drawChart(binding.chartStepsinfo, stepsEntries, labelsArr, R.color.steps_start, R.color.steps_end);
        drawChart(binding.chartCaloriesnfo, calEntries, labelsArr, R.color.calories_start, R.color.calories_end);
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

    // --- ДИАЛОГ ИЗМЕНЕНИЯ ЦЕЛИ ---
    private void showEditGoalDialog(String title, String databasePath) {
        // 1. Создаем Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // 2. Инфлейтим (создаем) наш кастомный макет
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        // 3. Находим элементы внутри макета
        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        EditText etInput = customView.findViewById(R.id.etGoalInput);
        View btnCancel = customView.findViewById(R.id.btnCancel);
        View btnSave = customView.findViewById(R.id.btnSave);

        // Устанавливаем заголовок
        tvTitle.setText(title);

        // Устанавливаем макет в диалог
        builder.setView(customView);

        // Создаем диалог (но пока не показываем)
        AlertDialog dialog = builder.create();

        // Делаем фон самого окна прозрачным, чтобы видны были только наши закругленные углы
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 4. Обработка нажатий

        // Кнопка Cancel
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Кнопка Save
        btnSave.setOnClickListener(v -> {
            String newValueStr = etInput.getText().toString();
            if (!newValueStr.isEmpty()) {
                try {
                    int newValue = Integer.parseInt(newValueStr);
                    updateGoalInFirebase(databasePath, newValue);
                    dialog.dismiss(); // Закрываем окно после сохранения
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Показываем диалог
        dialog.show();
    }
    private void updateGoalInFirebase(String relativePath, int value) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        String todayDate = LocalDate.now().toString(); // Требует Desugaring в Gradle, или используйте DateHelper

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid)
                .child("daily_data")
                .child(todayDate);

        ref.child(relativePath).setValue(value)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Goal updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error updating goal", Toast.LENGTH_SHORT).show());
    }

    private void clearUI() {
        binding.tvStepsScore.setText("0");
        binding.tvCaloriesScore.setText("0");
        binding.tvNutritionScore.setText("0");
        binding.tvNutritionAllCarbs.setText("Carbs: 0g");
        binding.tvNutritionAllProteins.setText("Protein: 0g");
        binding.tvNutritionAllFats.setText("Fats: 0g");
        if (binding.chartStepsinfo != null) binding.chartStepsinfo.clear();
        if (binding.chartCaloriesnfo != null) binding.chartCaloriesnfo.clear();
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