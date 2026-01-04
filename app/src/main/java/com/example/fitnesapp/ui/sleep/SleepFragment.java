package com.example.fitnesapp.ui.sleep;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.Adapters.CalendarPagerAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentSleepBinding;
import com.example.fitnesapp.models.CalendarDate;
import com.example.fitnesapp.utils.ChartHelper;
import com.example.fitnesapp.utils.DateHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public class SleepFragment extends Fragment {

    private SleepViewModel mViewModel;
    private FragmentSleepBinding binding;// Используем Binding для доступа к View

    public static SleepFragment newInstance() {
        return new SleepFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Инициализация Binding
        binding = FragmentSleepBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализация ViewModel
        mViewModel = new ViewModelProvider(this).get(SleepViewModel.class);

        // Подписываемся на данные (Наблюдатели)
        observeData();
        setupCalendar();
        setupSleepHistoryChart();
    }

    private void observeData() {
        // Текстовые поля
        mViewModel.getSleepQuality().observe(getViewLifecycleOwner(), quality -> {
             binding.tvSleepQuality.setText(quality); // Раскомментируйте, если есть ID в XML
        });

        // Графики фаз сна (обновляем при изменении данных)
        mViewModel.getDeepSleepPercent().observe(getViewLifecycleOwner(), percent -> {
            // Фиолетовый (#9C27B0) для глубокого сна
            setupMiniPie(binding.chartDeep, percent, Color.parseColor("#9C27B0"));
            binding.tvDeepPercent.setText(percent + "%");
        });

        mViewModel.getSurfaceSleepPercent().observe(getViewLifecycleOwner(), percent -> {
            // Бирюзовый (#4DD0E1) для поверхностного сна
            setupMiniPie(binding.chartSurface, percent, Color.parseColor("#4DD0E1"));
            binding.tvSurfacePercent.setText(percent + "%");
        });

        mViewModel.getFastSleepPercent().observe(getViewLifecycleOwner(), percent -> {
            // Оранжевый (#FF9800) для быстрого сна
            setupMiniPie(binding.chartFast, percent, Color.parseColor("#FF9800"));
            binding.tvFastPercent.setText(percent + "%");
        });

        mViewModel.getAwakeSleepPercent().observe(getViewLifecycleOwner(), percent -> {
            // Красный (#F44336) для бодрствования
            setupMiniPie(binding.chartAwake, percent, Color.parseColor("#F44336"));
            binding.tvAwakePercent.setText(percent + "%");
        });
    }

    private void setupMiniPie(PieChart chart, int percentage, int color) {
        if (chart == null) return;

        List<PieEntry> entries = new ArrayList<>();
        // 1. Заполненная часть (Фаза сна)
        entries.add(new PieEntry((float) percentage));
        // 2. Пустая часть (Остаток до 100%)
        entries.add(new PieEntry((float) (100 - percentage)));

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Цвета: [Основной, Темный фон]
        // #232D36 - это темно-серый фон "пустой" части круга (подберите под ваш дизайн)
        dataSet.setColors(color, Color.parseColor("#232D36"));

        dataSet.setDrawValues(false); // Убираем цифры с самого графика

        PieData data = new PieData(dataSet);
        chart.setData(data);

        // СТИЛИЗАЦИЯ (Минимализм)
        chart.setDescription(null);          // Убрать описание "Description Label"
        chart.getLegend().setEnabled(false); // Убрать легенду (квадратики внизу)
        chart.setTouchEnabled(false);        // Убрать вращение пальцем

        // Делаем "Пончик" (Дырка внутри)
        chart.setDrawHoleEnabled(true);
//        chart.setHoleColor(Color.TRANSPARENT); // Прозрачная дырка
        chart.setHoleRadius(0f);              // Размер дырки (чем больше, тем тоньше кольцо)
        chart.setTransparentCircleRadius(0f);  // Убрать полупрозрачную обводку

        // Анимация
        chart.animateY(1000); // Плавное появление

        chart.invalidate(); // Обновить
    }


    private void setupSleepHistoryChart() {
        // 1. Готовим данные
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 2f));
        entries.add(new Entry(1, 1f));
        entries.add(new Entry(2, 0f));
        entries.add(new Entry(3, 0f));
        entries.add(new Entry(4, 1f));
        entries.add(new Entry(5, 0f));
        entries.add(new Entry(6, 1f));
        entries.add(new Entry(7, 2f));
        entries.add(new Entry(8, 1f));
        entries.add(new Entry(9, 0f));
        entries.add(new Entry(10, 1f));
        entries.add(new Entry(11, 2f));
        // ... заполнение ...

        final String[] labels = new String[]{
                "23:00", "23:30", "00:00", "00:30",
                "01:00", "01:30", "02:00", "02:30",
                "03:00", "04:00", "05:00", "07:00"
        };

        // 2. ВЫЗЫВАЕМ УТИЛИТУ (Всего одна строка!)
        // requireContext() передает контекст, нужный для получения цветов
        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartSleepInfo,
                entries,
                labels,
                R.color.sleep_start,
                R.color.sleep_end,
                true
        );
    }

    private void setupCalendar() {
        DateHelper.setupHistoryCalendar(
                requireContext(),
                binding.recyclerViewSleep,
                date -> {
                    // Логика клика именно для СНА
                    Toast.makeText(getContext(), "Сон за: " + date.getDayNumber(), Toast.LENGTH_SHORT).show();
                    // TODO: 03.01.2026 Добавить обновление єкрана
                }
        );
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Очищаем binding для предотвращения утечек памяти
    }
}