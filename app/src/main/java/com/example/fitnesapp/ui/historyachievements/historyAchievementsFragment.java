package com.example.fitnesapp.ui.historyachievements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.HistoryAdapter;
import com.example.fitnesapp.databinding.FragmentHistoryAchievementsBinding;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.utils.LevelSystem; // Импортируйте ваш новый класс

import java.util.ArrayList;

public class historyAchievementsFragment extends Fragment {

    private HistoryAchievementsViewModel mViewModel;
    private FragmentHistoryAchievementsBinding binding;

    public static historyAchievementsFragment newInstance() {
        return new historyAchievementsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HistoryAchievementsViewModel.class);

        setupRecyclerView();
        setupObservers();
    }

    private void setupRecyclerView() {
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerHistory.setAdapter(new HistoryAdapter(getContext(), new ArrayList<>()));
    }

    private void setupObservers() {
        mViewModel.getCollectedList().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                HistoryAdapter adapter = new HistoryAdapter(getContext(), list);
                binding.recyclerHistory.setAdapter(adapter);
            }
        });

        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                updateRankUI(profile);
            }
        });
    }

    private void updateRankUI(UserProfile profile) {
        // Используем нашу утилиту для расчета
        LevelSystem.LevelInfo info = LevelSystem.calculate(profile.totalXP);

        // 1. Устанавливаем название ранга
        binding.tvRankTitle.setText(info.currentRankTitle);

        // 2. Настраиваем прогресс-бар
        // Важно: мы ставим границы именно текущего уровня, чтобы полоска была "живой"
        if (info.isMaxLevel) {
            binding.progressBarXP.setMax(100);
            binding.progressBarXP.setProgress(100);
            binding.tvXPCount.setText(info.totalXP + " XP (MAX)");
        } else {
            binding.progressBarXP.setMax(info.xpToNextLevel);
            binding.progressBarXP.setProgress(info.currentLevelXP);

            // Текст: "Текущий XP / Цель следующего уровня"
            binding.tvXPCount.setText(info.totalXP + " / " + info.nextLevelThreshold + " xp");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}