package com.example.fitnesapp.ui.oxygenlevel;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentOxygenBinding;
import com.example.fitnesapp.utils.ChartHelper;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class OxygenFragment extends Fragment {

    private OxygenViewModel mViewModel;
    private FragmentOxygenBinding binding;

    public static OxygenFragment newInstance() {
        return new OxygenFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOxygenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(OxygenViewModel.class);
        // Находим SeekBar
        SeekBar statusBar = binding.getRoot().findViewById(R.id.statusBar);

        // Чтобы пользователь не мог двигать его пальцем (если это только отображение),
        // отключаем touch listener:
        statusBar.setOnTouchListener((v, event) -> true);

        // Установка значения (0 - 100)
        // Например, если статус "Норма" (зеленая зона), ставим 15-20
        // Если "Плохо" (красная зона), ставим 90
        int statusValue = 25;
        statusBar.setProgress(statusValue);
        setupOxygenChart();
    }

    private void setupOxygenChart() {
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
                binding.chartOxygenInfo,
                entries,
                labels,
                R.color.oxygen_start,
                R.color.oxygen_end,
                true
        );
    }

}