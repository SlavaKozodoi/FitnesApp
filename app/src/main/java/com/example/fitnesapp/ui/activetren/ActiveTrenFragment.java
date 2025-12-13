package com.example.fitnesapp.ui.activetren;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.fitnesapp.Adapters.AdviceHistoryAdapter;
import com.example.fitnesapp.Adapters.TrainingAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentActiveTrenBinding;
import com.example.fitnesapp.models.Edvice;
import com.example.fitnesapp.models.TrainingStat;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

public class ActiveTrenFragment extends Fragment {

    private ActiveTrenViewModel mViewModel;
    FragmentActiveTrenBinding binding;

    public static ActiveTrenFragment newInstance() {
        return new ActiveTrenFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentActiveTrenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ActiveTrenViewModel.class);
        setupStatsCarousel();
        setupAdviceList();
    }

    private void setupAdviceList(){
        RecyclerView recycler = binding.recyclerAdviceHistory;

        List<Edvice> dummyList = new ArrayList<>();
        dummyList.add(new Edvice(R.drawable.ic_heart_oxygen_icon,"Slow down","7:28"));
        dummyList.add(new Edvice(R.drawable.ic_advice_history,"Good job","7:30"));
        dummyList.add(new Edvice(R.drawable.ic_advice_bottle,"Stooop!!!","7:35"));
        dummyList.add(new Edvice(R.drawable.ic_advice_bottle,"Stooop!!!","7:35"));
        dummyList.add(new Edvice(R.drawable.ic_advice_bottle,"Stooop!!!","7:35"));
        dummyList.add(new Edvice(R.drawable.ic_advice_bottle,"Stooop!!!","7:35"));
        dummyList.add(new Edvice(R.drawable.ic_advice_bottle,"Stooop!!!","7:35"));
        dummyList.add(new Edvice(R.drawable.ic_advice_bottle,"Stooop!!!","7:35"));
        dummyList.add(new Edvice(R.drawable.ic_heart_oxygen_icon,"Take a rest","7:40"));

        AdviceHistoryAdapter adviceHistoryAdapter = new AdviceHistoryAdapter(getContext(),dummyList);
        recycler.setAdapter(adviceHistoryAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

    }


    private void setupStatsCarousel() {
        RecyclerView recycler = binding.recyclerTrainingStats;

        List<TrainingStat> data = new ArrayList<>();

// 1. ПУЛЬС (Красный)
// Генерируем данные: 15 точек, база 60 + рандом
        ChartDataResult pulseResult = generateDummyData(15, 60, 50);
        data.add(new TrainingStat(
                "Pulse",
                "107",
                R.drawable.ic_heart_icon,
                R.color.pulse_start,
                R.color.pulse_end,
                pulseResult.entries,
                pulseResult.labels // Передаем подписи
        ));

// 2. КИСЛОРОД (Розовый)
// Генерируем данные: 15 точек, база 90 + рандом
        ChartDataResult oxResult = generateDummyData(15, 90, 8);
        data.add(new TrainingStat(
                "Blood oxygen",
                "98",
                R.drawable.ic_heart_oxygen_icon,
                R.color.oxygen_start,
                R.color.oxygen_end,
                oxResult.entries,
                oxResult.labels
        ));

// 3. ТЕМПЕРАТУРА / ТЕМП (Оранжевый)
// Генерируем данные: 15 точек, база 4 + рандом (для темпа бега)
        ChartDataResult tempResult = generateDummyData(15, 4, 2);
        data.add(new TrainingStat(
                "Temp",
                "4:28",
                R.drawable.ic_temp,
                R.color.temp_start,
                R.color.temp_end,
                tempResult.entries,
                tempResult.labels
        ));


        // 2. Настройка RecyclerView
        TrainingAdapter adapter = new TrainingAdapter(getContext(), data);
        recycler.setAdapter(adapter);

        // Делаем список горизонтальным
        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 3. САМОЕ ГЛАВНОЕ: PagerSnapHelper
        // Эта штука заставляет карточки "прилипать" к центру, как ViewPager
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recycler);
    }
    // Простой класс для хранения пары "Данные + Подписи"
    private static class ChartDataResult {
        ArrayList<Entry> entries;
        String[] labels;

        public ChartDataResult(ArrayList<Entry> entries, String[] labels) {
            this.entries = entries;
            this.labels = labels;
        }
    }

    // Метод генерации
    private ChartDataResult generateDummyData(int count, float baseValue, float randomRange) {
        ArrayList<Entry> entries = new ArrayList<>();
        String[] labels = new String[count];

        for (int i = 0; i < count; i++) {
            // Значение Y
            float val = (float) (baseValue + Math.random() * randomRange);
            entries.add(new Entry(i, val));

            // Значение X (Подпись) - просто числа 0, 1, 2...
            // Или можно сделать время: i + "m" -> "1m", "2m"...
            labels[i] = String.valueOf(i);
        }

        return new ChartDataResult(entries, labels);
    }


}