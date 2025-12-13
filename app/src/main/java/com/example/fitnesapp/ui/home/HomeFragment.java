package com.example.fitnesapp.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.Adapters.CalendarAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentHomeBinding;
import com.github.mikephil.charting.charts.BarChart;
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

    private CardView cvSleep,cvPulse,cvWeight,cvOxygen,cvDayActivity;
    private Calendar currentCalendar = Calendar.getInstance();
    private ImageButton activeTrenImageBTN;
    private CalendarAdapter calendarAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        cvSleep = binding.cvSleep;
        cvPulse = binding.cvPulse;
        cvWeight = binding.cvWeight;
        cvOxygen = binding.cvOxygen;
        cvDayActivity = binding.cvDayActivity;
        activeTrenImageBTN = binding.imageButton;
        cvSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.sleepFragment);
            }
        });
        cvPulse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.pulseFragment);
            }
        });
        cvWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.weightFragment);
            }
        });
        cvOxygen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.oxygenFragment);
            }
        });
        activeTrenImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.activeTrenFragment);
            }
        });
        cvDayActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.dayActivityFragment);
            }
        });
        //todo обработать переходы на другие окна


        // Запускаем настройку данных и анимацию
        setupDashboard();
        setupMiniCharts();
        setupHistoryCalendar();
        return root;

    }


    private void setupDashboard() {
        // === 1. ЗАДАЕМ ЦЕЛИ И ТЕКУЩИЕ ЗНАЧЕНИЯ ===
        // (В будущем берите их из ViewModel или базы данных)
        float caloriesGoal = 2000f;
        float caloriesCurrent = 1000f;

        float stepsGoal = 10000f;
        float stepsCurrent = 5000f;

        float nutritionGoal = 2000f;
        float nutritionCurrent = 1000f;

        // === 2. НАСТРАИВАЕМ ГРАФИКИ (АРКИ) ===
        // Хитрость: так как мы видим только половину круга (верхнюю),
        // мы должны умножить цель на 2. Тогда 50% круга будет выглядеть как 100% шкалы.

        // Калории
        binding.progressCalories.setProgressMax(caloriesGoal );
        binding.progressCalories.setProgressWithAnimation(caloriesCurrent, 1000L); // 1 сек анимация

        // Шаги
        binding.progressSteps.setProgressMax(stepsGoal );
        binding.progressSteps.setProgressWithAnimation(stepsCurrent, 1000L);

        // Питание
        binding.progressNutrition.setProgressMax(nutritionGoal );
        binding.progressNutrition.setProgressWithAnimation(nutritionCurrent, 1000L);

        // === 3. ОБНОВЛЯЕМ ЦИФРЫ ВНИЗУ ===
        // Здесь показываем реальные значения, без умножения
        if (binding.tvCaloriesValue != null) {
            binding.tvCaloriesValue.setText(String.valueOf((int) caloriesCurrent));
        }
        binding.tvCaloriesGoal.setText("/" + (int) caloriesGoal + getString(R.string.short_text_calories) );

        if (binding.tvStepsValue != null) {
            binding.tvStepsValue.setText(String.valueOf((int) stepsCurrent));
        }
        binding.tvStepsGoal.setText("/" + (int) stepsGoal + getString(R.string.short_text_steps) );
        if (binding.tvNutritionValue != null) {
            binding.tvNutritionValue.setText(String.valueOf((int) nutritionCurrent));
        }
        binding.tvNutritionGoal.setText("/" + (int) nutritionGoal + getString(R.string.short_text_calories) );

    }

    // Здесь мы готовим данные (в будущем они придут из БД) и передаем в методы настройки
    private void setupMiniCharts() {
        // 1. Сон (Одно целое число 0-100)
        int sleepScore = 90;
        updateSleepScore(sleepScore);

        // 2. Пульс (Список целых чисел)
        List<Integer> pulseData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            pulseData.add((int) (60 + Math.random() * 40));
        }
        setupPulseChart(pulseData);

        // 3. Вес (Список дробных чисел)
        List<Float> weightData = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            weightData.add((float) (60 + Math.random() * 40));
        }

        setupWeightChart(weightData);

        // 4. Кислород (Список целых чисел)
        List<Integer> oxygenData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            oxygenData.add((int) (95 + Math.random() * 4));
        }
        setupOxygenChart(oxygenData);
    }

    // --- 1. ГРАФИК СНА (Принимает одно число - оценку) ---
    private void updateSleepScore(int score) {
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        View viewProgress = binding.viewSleepBad;
        View viewEmpty = binding.viewSleepGood;

        if (viewProgress == null || viewEmpty == null) return;

        LinearLayout.LayoutParams progressParams = (LinearLayout.LayoutParams) viewProgress.getLayoutParams();
        LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) viewEmpty.getLayoutParams();

        // Распределяем вес пропорционально оценке
        progressParams.weight = score;
        emptyParams.weight = 100 - score;

        viewProgress.setLayoutParams(progressParams);
        viewEmpty.setLayoutParams(emptyParams);
    }

    // --- 2. ГРАФИК ПУЛЬСА (Принимает список значений) ---
    private void setupPulseChart(List<Integer> dataValues) {
        BarChart chart = binding.chartPulse;
        if (chart == null || dataValues == null || dataValues.isEmpty()) return;

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dataValues.size(); i++) {
            // i - это позиция по оси X, dataValues.get(i) - значение по оси Y
            entries.add(new BarEntry(i, dataValues.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#FF5252"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();
    }

    // --- 3. ГРАФИК ВЕСА (Принимает список значений) ---
    private void setupWeightChart(List<Float> dataValues) {
        LineChart chart = binding.chartWeight;
        if (chart == null || dataValues == null || dataValues.isEmpty()) return;

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dataValues.size(); i++) {
            entries.add(new Entry(i, dataValues.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(2f);
        dataSet.setColor(Color.parseColor("#4CAF50"));

        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4CAF50"));
        dataSet.setFillAlpha(100);

        LineData data = new LineData(dataSet);
        // 1. Создаем формат даты (день.месяц, например "22.11")
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

// 2. Получаем сегодняшнюю дату (для End)
        String endDate = sdf.format(calendar.getTime());

// 3. Отнимаем 1 месяц назад (для Start)
        calendar.add(Calendar.MONTH, -1);
        String startDate = sdf.format(calendar.getTime());

// 4. Устанавливаем текст
        binding.tvStartWeight.setText(startDate);
        binding.tvEndWeight.setText(endDate);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();
    }

    // --- 4. ГРАФИК КИСЛОРОДА (Принимает список значений) ---
    private void setupOxygenChart(List<Integer> dataValues) {
        BarChart chart = binding.chartOxygen;
        if (chart == null || dataValues == null || dataValues.isEmpty()) return;

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dataValues.size(); i++) {
            entries.add(new BarEntry(i, dataValues.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#FF5252"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        chart.setData(data);
        simplifyChart(chart);
        chart.invalidate();

    }

    // Вспомогательный метод для очистки стиля (убирает сетку и цифры)
    private void simplifyChart(com.github.mikephil.charting.charts.Chart<?> chart) {
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

    private void setupHistoryCalendar() {
        // 1. Находим Views (лучше использовать binding)
        RecyclerView recyclerCalendar = binding.recyclerCalendar;
        TextView tvMonthName = binding.tvMonthName;
        View btnPrev = binding.btnPrevMonth;
        View btnNext = binding.btnNextMonth;

        // 2. Настраиваем RecyclerView (Сетка из 7 столбцов)
        recyclerCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        // Начальные пустые данные
        calendarAdapter = new CalendarAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), (day, isActive) -> {

            // ЭТОТ КОД СРАБОТАЕТ ПРИ НАЖАТИИ НА ДЕНЬ
            openDayDetails(day);

        });

        recyclerCalendar.setAdapter(calendarAdapter);

        // 3. Отображаем текущий месяц
        updateCalendarDisplay();

        // 4. Обработчики кнопок
        btnPrev.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });

        btnNext.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });
    }
    // Метод для открытия нового экрана
    private void openDayDetails(int day) {
        // Вариант 1: Если используете Activity
    /*
    Intent intent = new Intent(getContext(), DayDetailsActivity.class);
    intent.putExtra("SELECTED_DAY", day);
    intent.putExtra("CURRENT_MONTH", currentCalendar.get(Calendar.MONTH));
    startActivity(intent);
    */

        // Вариант 2: Если используете Navigation Component (Фрагменты) - РЕКОМЕНДУЮ
    /*
    Bundle bundle = new Bundle();
    bundle.putInt("day", day);
    Navigation.findNavController(requireView()).navigate(R.id.action_home_to_details, bundle);
    */

        // Для теста пока можно просто вывести Тост:
        android.widget.Toast.makeText(getContext(), "Нажат день: " + day, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void updateCalendarDisplay() {
        // 1. Обновляем заголовок (Например: "September")
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.ENGLISH); // Или Locale.getDefault()
        binding.tvMonthName.setText(sdf.format(currentCalendar.getTime()));

        // 2. Вычисляем количество дней в месяце
        int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 3. Создаем список дней [1, 2, 3 ... 30]
        List<Integer> daysList = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            daysList.add(i);
        }

        // 4. Имитация активных дней (Здесь нужно брать реальные данные из БД)
        // Например, пусть активными будут 5, 9, 12, 18, 27 числа
        List<Integer> activeDays = new ArrayList<>();
        activeDays.add(5);
        activeDays.add(9);
        activeDays.add(12);
        activeDays.add(14);
        activeDays.add(16);
        activeDays.add(18);
        activeDays.add(27);

        // В реальности вы будете делать запрос: getActiveDaysForMonth(currentCalendar.getTime())

        // 5. Обновляем адаптер
        calendarAdapter.updateData(daysList, activeDays);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}