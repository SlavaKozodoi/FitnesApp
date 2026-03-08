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
import com.example.fitnesapp.ui.base.BaseLoadingFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ActiveTrenFragment extends BaseLoadingFragment {

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

        startFakeLoading(view, 200);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}