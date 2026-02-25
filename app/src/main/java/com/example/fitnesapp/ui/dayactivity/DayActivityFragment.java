package com.example.fitnesapp.ui.dayactivity;

import android.app.AlertDialog;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DayActivityFragment extends Fragment {

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
    }

    private void setupClickListeners() {
        binding.tvStepsGoal.setOnClickListener(v -> showEditGoalDialog("Steps Goal", "stepsGoal"));
        binding.tvCaloriesGoal.setOnClickListener(v -> showEditGoalDialog("Burn Calories Goal", "caloriesGoal"));
        binding.tvNutritionMax.setOnClickListener(v -> showEditGoalDialog("Nutrition Max Goal", "nutrition/maxCalories"));
    }

    private void updateUI(DailyData data) {
        if (data == null) {
            clearUI();
            // Принудительно рисуем пустые графики
            updateCharts(new DailyData());
            return;
        }

        // --- БЛОК 1: ШАГИ И КАЛОРИИ ---
        binding.tvStepsScore.setText(String.valueOf(data.steps));
        binding.tvStepsGoal.setText("Goal: " + (data.stepsGoal >= 0 ? data.stepsGoal : 10000));

        binding.tvCaloriesScore.setText(String.valueOf((int) data.caloriesBurned));
        binding.tvCaloriesGoal.setText("Goal: " + (data.caloriesGoal >= 0 ? data.caloriesGoal : 2000));

        // --- БЛОК 2: ПИТАНИЕ ---
        if (data.nutrition != null) {
            binding.tvNutritionScore.setText(String.valueOf((int) data.nutrition.totalCalories));
            binding.tvNutritionMax.setText("Max: " + data.nutrition.maxCalories);
            binding.tvNutritionAllCarbs.setText("Carbs: " + (int) data.nutrition.carbs + "g");
            binding.tvNutritionAllProteins.setText("Protein: " + (int) data.nutrition.protein + "g");
            binding.tvNutritionAllFats.setText("Fats: " + (int) data.nutrition.fat + "g");
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

        // 1. ОПРЕДЕЛЯЕМ ВРЕМЯ (Будущее, Сегодня, Прошлое)
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

        // Переменная для "пола" графика калорий
        float baseCalories = 0f;

        if (isFuture) {
            // === ЕСЛИ ЭТО БУДУЩЕЕ: РИСУЕМ СТРОГИЕ НУЛИ ===
            stepsEntries.add(new Entry(0, 0f));
            stepsEntries.add(new Entry(1, 0f));
            calEntries.add(new Entry(0, 0f));
            calEntries.add(new Entry(1, 0f));
            labels.add("00:00");
            labels.add("23:59");
            // baseCalories остается 0f
        } else {
            // === ЕСЛИ СЕГОДНЯ ИЛИ ПРОШЛОЕ: РАССЧИТЫВАЕМ ДАННЫЕ ===
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

                // ИЗМЕНЕНИЕ: Рисуем на графике ТОЛЬКО активные калории
                // Базовый метаболизм уже включен в общую цифру наверху экрана
                float activeCals = (float) (stepsBuckets[i] * calsPerStep);
                calEntries.add(new Entry(i, activeCals));

                int h = i / 2;
                int m = (i % 2) * 30;
                labels.add(String.format(Locale.US, "%02d:%02d", h, m));
            }
        }

        String[] labelsArr = labels.toArray(new String[0]);

        // ТЕПЕРЬ ПОЛ ДЛЯ ОБОИХ ГРАФИКОВ - СТРОГИЙ НОЛЬ (0f)
        drawChart(binding.chartStepsinfo, stepsEntries, labelsArr, R.color.steps_start, R.color.steps_end, 0f);
        drawChart(binding.chartCaloriesnfo, calEntries, labelsArr, R.color.calories_start, R.color.calories_end, 0f);
    }

    // Обратите внимание на новый параметр float floorValue
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

        // Прибиваем график к нашему кастомному полу!
        // Для шагов это будет 0, для калорий - базовый метаболизм

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
                    Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void updateGoalInFirebase(String relativePath, int value) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        // ИСПОЛЬЗУЕМ ВЫБРАННУЮ ДАТУ (а не просто сегодняшний день)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateKey = sdf.format(selectedDate);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid)
                .child("daily_data")
                .child(dateKey);

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
        // Графики не очищаем, они перерисуются сами!
        binding.recyclerViewNutHistory.setAdapter(null);
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewDayActivity,
                date -> {
                    selectedDate = date.getDate(); // ЗАПОМИНАЕМ ВЫБРАННУЮ ДАТУ
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