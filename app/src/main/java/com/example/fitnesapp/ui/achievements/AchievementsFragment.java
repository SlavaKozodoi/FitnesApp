package com.example.fitnesapp.ui.achievements;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.AchievementsAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentAchievementBinding;
import com.example.fitnesapp.models.Achievement;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AchievementsFragment extends Fragment {

    private AchievementsViewModel mViewModel;
    private FragmentAchievementBinding binding;

    private List<Achievement> fullList = new ArrayList<>();
    private AchievementsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAchievementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(AchievementsViewModel.class);

        setupRecyclerView();
        setupFilters();
        setupObservers();
    }

    private void setupRecyclerView() {
        binding.recyclerAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        // Отступ снизу (опционально)
        binding.recyclerAchievements.setPadding(0, 0, 0, 200);
        binding.recyclerAchievements.setClipToPadding(false);
    }

    private void setupFilters() {
        binding.tabsContainer.setOnCheckedChangeListener((group, checkedId) -> {
            filterAndShowList();
        });
    }

    private void setupObservers() {
        mViewModel.getAllAchievementsList().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                fullList = list;
                filterAndShowList();
            }
        });
    }

    private void filterAndShowList() {
        if (fullList == null || fullList.isEmpty()) return;

        List<Achievement> filtered = new ArrayList<>();
        int selectedId = binding.tabsContainer.getCheckedRadioButtonId();

        String targetCategory = "Recommended";
        if (selectedId == R.id.tabCollections) targetCategory = "Collections";
        else if (selectedId == R.id.tabMilestones) targetCategory = "Milestones";
        else if (selectedId == R.id.tabLegendary) targetCategory = "Legendary";

        for (Achievement a : fullList) {
            if (a.category.equalsIgnoreCase(targetCategory)) {
                if (targetCategory.equals("Collections")) {
                    if (a.isCollected) filtered.add(a);
                    else if (!a.isCollected) filtered.add(a); // В коллекциях показываем всё (и собранное, и нет)
                } else {
                    if (!a.isCollected) filtered.add(a);
                }
            }
        }

        Collections.sort(filtered, new Comparator<Achievement>() {
            @Override
            public int compare(Achievement o1, Achievement o2) {
                // 1. Готовые к сбору - выше
                if (o1.isCompleted && !o2.isCompleted && !o1.isCollected) return -1;
                if (!o1.isCompleted && o2.isCompleted && !o2.isCollected) return 1;

                // 2. Открытые выше закрытых
                if (!o1.isLocked && o2.isLocked) return -1;
                if (o1.isLocked && !o2.isLocked) return 1;

                // 3. По порядку (Target)
                return Integer.compare(o1.target, o2.target);
            }
        });

        adapter = new AchievementsAdapter(getContext(), filtered, new AchievementsAdapter.OnAchievementClickListener() {
            @Override
            public void onCollectClick(Achievement achievement) {
                mViewModel.collectAchievement(achievement);
                Toast.makeText(getContext(), "Collected " + achievement.xpReward + " XP!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemClick(Achievement achievement) {
                showDetailsDialog(achievement);
            }
        });

        binding.recyclerAchievements.setAdapter(adapter);
    }

    private void showDetailsDialog(Achievement achievement) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(R.layout.dialog_achievement_details);

        ImageView icon = dialog.findViewById(R.id.ivDetailIcon);
        TextView title = dialog.findViewById(R.id.tvDetailTitle);
        TextView tier = dialog.findViewById(R.id.tvDetailTier);
        TextView desc = dialog.findViewById(R.id.tvDetailDesc);
        TextView reward = dialog.findViewById(R.id.tvDetailReward);
        Button btnClose = dialog.findViewById(R.id.btnCloseDialog);

        title.setText(achievement.title);
        tier.setText(achievement.tier);
        reward.setText("+" + achievement.xpReward + " XP");

        // --- ЛОГИКА ДИАЛОГА ---
        if ("collection".equals(achievement.type)) {
            // ДЛЯ КОЛЛЕКЦИЙ: Показываем список
            desc.setText("Collect these badges:\n\n" + achievement.subItemsStatus);
            desc.setGravity(Gravity.START); // Выравниваем влево
        } else {
            // ДЛЯ ОБЫЧНЫХ: Показываем описание или причину блокировки
            if (achievement.isLocked) {
                desc.setText("LOCKED!\nRequirement: Complete '" + achievement.requiredTitle + "' first.");
            } else {
                desc.setText(achievement.description);
            }
            desc.setGravity(Gravity.CENTER);
        }

        int resId = achievement.getIconResId(getContext());
        if (resId != 0 && icon != null) icon.setImageResource(resId);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}