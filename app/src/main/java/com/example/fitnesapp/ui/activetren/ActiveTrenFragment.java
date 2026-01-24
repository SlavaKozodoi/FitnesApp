package com.example.fitnesapp.ui.activetren;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.WorkoutsFeedAdapter;
import com.example.fitnesapp.databinding.FragmentActiveTrenBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ActiveTrenFragment extends Fragment {

    private ActiveTrenViewModel mViewModel;
    private FragmentActiveTrenBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentActiveTrenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ActiveTrenViewModel.class);

        // 1. Получаем дату (из Bundle или текущую)
        // ВАЖНО: Календарь должен передавать строку даты "yyyy-MM-dd" в аргумент "dateString"
        // Если передаются старые аргументы (timestamp), конвертируем их
        String dateKey;
        if (getArguments() != null && getArguments().containsKey("timestamp")) {
            long ts = getArguments().getLong("timestamp");
            dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ts));
        } else {
            dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        }
        

        // 2. Инициализируем пустой список
        binding.recyclerWorkoutsFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        WorkoutsFeedAdapter adapter = new WorkoutsFeedAdapter(getContext(), new ArrayList<>());
        binding.recyclerWorkoutsFeed.setAdapter(adapter);

        // 3. Грузим данные
        mViewModel.loadWorkoutsForDate(dateKey);

        mViewModel.getWorkoutSessions().observe(getViewLifecycleOwner(), sessions -> {
            // Обновляем адаптер
            WorkoutsFeedAdapter newAdapter = new WorkoutsFeedAdapter(getContext(), sessions);
            binding.recyclerWorkoutsFeed.setAdapter(newAdapter);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}