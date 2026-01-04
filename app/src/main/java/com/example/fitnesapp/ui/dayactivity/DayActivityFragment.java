package com.example.fitnesapp.ui.dayactivity;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.fitnesapp.Adapters.NutritionAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentDayActivityBinding;
import com.example.fitnesapp.models.Nutrition;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

public class DayActivityFragment extends Fragment {

    private DayActivityViewModel mViewModel;
    private FragmentDayActivityBinding binding;

    public static DayActivityFragment newInstance() {
        return new DayActivityFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDayActivityBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DayActivityViewModel.class);
        setupNutList();;
        setupCaloriesHistoryChart();
        setupStepsHistoryChart();
        setupCalendar();

    }

    private void setupNutList(){
        RecyclerView recyclerView = binding.recyclerViewNutHistory;

        List<Nutrition> nutritionList = new ArrayList<>();

        nutritionList.add(new Nutrition("07:30","220","36","9","1200"));
        nutritionList.add(new Nutrition("10:30","100","16","4","600"));
        nutritionList.add(new Nutrition("12:00","232","43","9","1500"));
        nutritionList.add(new Nutrition("17:30","245","30","10","1700"));

        NutritionAdapter adapter = new NutritionAdapter(getContext(),nutritionList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewDayActivity,
                date -> {
                    // Логика клика именно для СНА
                    Toast.makeText(getContext(), "Данные за: " + date.getDayNumber(), Toast.LENGTH_SHORT).show();
                    // TODO: 03.01.2026 Добавить обновление єкрана
                }
        );
    }

    private void setupCaloriesHistoryChart() {
        // 1. Готовим данные
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 120f));  // 07:00 - Пробуждение, завтрак
        entries.add(new Entry(1, 250f));  // 08:00 - Дорога на работу
        entries.add(new Entry(2, 150f));  // 09:00 - Работа
        entries.add(new Entry(3, 180f));  // 11:00
        entries.add(new Entry(4, 350f));  // 13:00 - Обед, прогулка
        entries.add(new Entry(5, 160f));  // 14:00 - Работа
        entries.add(new Entry(6, 170f));  // 16:00
        entries.add(new Entry(7, 480f));  // 18:00 - Тренировка/Зал (Пик)
        entries.add(new Entry(8, 420f));  // 19:00 - Продолжение активности
        entries.add(new Entry(9, 200f));  // 20:00 - Ужин
        entries.add(new Entry(10, 100f)); // 21:00 - Отдых
        entries.add(new Entry(11, 60f));  // 22:00 - Подготовка ко сну

        // Метки времени
        final String[] labels = new String[]{
                "07:00", "08:00", "09:00", "11:00",
                "13:00", "14:00", "16:00", "18:00",
                "19:00", "20:00", "21:00", "22:00"
        };
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartCaloriesnfo,
                entries,
                labels,
                R.color.calories_start,
                R.color.calories_end,
                true
        );
    }

    private void setupStepsHistoryChart() {
        // 1. Готовим данные
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 300f));   // 07:00 - Дом
        entries.add(new Entry(1, 1500f));  // 08:00 - Идем к транспорту/работе
        entries.add(new Entry(2, 200f));   // 09:00 - Сидим в офисе
        entries.add(new Entry(3, 400f));   // 11:00 - Кофе-брейк
        entries.add(new Entry(4, 2100f));  // 13:00 - Активный обед
        entries.add(new Entry(5, 300f));   // 14:00 - Снова работа
        entries.add(new Entry(6, 500f));   // 16:00
        entries.add(new Entry(7, 3500f));  // 18:00 - Вечерняя пробежка или путь домой (Пик)
        entries.add(new Entry(8, 1200f));  // 19:00 - Магазин/Прогулка
        entries.add(new Entry(9, 600f));   // 20:00 - Дома
        entries.add(new Entry(10, 100f));  // 21:00
        entries.add(new Entry(11, 0f));    // 22:00

        // Те же метки времени
        final String[] labels = new String[]{
                "07:00", "08:00", "09:00", "11:00",
                "13:00", "14:00", "16:00", "18:00",
                "19:00", "20:00", "21:00", "22:00"
        };
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartStepsinfo,
                entries,
                labels,
                R.color.steps_start,
                R.color.steps_end,
                true
        );
    }


}