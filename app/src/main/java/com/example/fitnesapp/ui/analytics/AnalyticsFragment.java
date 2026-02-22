package com.example.fitnesapp.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.InsightsAdapter;
import com.example.fitnesapp.databinding.FragmentAnalyticsBinding;

import java.util.ArrayList;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private AnalyticsViewModel analyticsViewModel;
    private InsightsAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        analyticsViewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Настраиваем список (RecyclerView)
        // ВАЖНО: Убедитесь, что в fragment_analytics.xml есть <androidx.recyclerview.widget.RecyclerView android:id="@+id/recyclerViewInsights" ... />
        adapter = new InsightsAdapter(new ArrayList<>());
        binding.recyclerViewInsights.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewInsights.setAdapter(adapter);

        // 2. Слушаем изменения из нейросети
        analyticsViewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            if (insights != null) {
                // Отправляем новые карточки в адаптер
                adapter.updateData(insights);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}