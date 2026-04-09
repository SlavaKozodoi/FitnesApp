package com.example.fitnesapp.ui.achievements;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

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

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;

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

        // --- ЗАПУСК ОБУЧЕНИЯ ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int tutorialStep = prefs.getInt("tutorial_step_ach", 0);

        if (tutorialStep == 0) {
            view.postDelayed(() -> showTutorial(view), 500);
        }
    }

    // Вспомогательный метод для склейки заголовка и текста из ресурсов
    private String getTutorialText(int titleResId, int descResId) {
        return getString(titleResId) + "\n\n" + getString(descResId);
    }

    private void showTutorial(View rootFragmentView) {
        if (binding == null) return;

        // Слайд 1: Приветствие
        FancyShowCaseView step1 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_ach_welcome_title, R.string.tutorial_ach_welcome_text))
                .titleStyle(0, Gravity.CENTER)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 2: Зачем собирать (Опыт и иконки)
        FancyShowCaseView step2 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_ach_xp_title, R.string.tutorial_ach_xp_text))
                .titleStyle(0, Gravity.CENTER)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 3: Как посмотреть детали (Фокус на список достижений)
        FancyShowCaseView step3 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_ach_details_title, R.string.tutorial_ach_details_text))
                .focusOn(binding.recyclerAchievements)
                .titleStyle(0, Gravity.BOTTOM)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 4: Фильтры категорий (Фокус на вкладки)
        FancyShowCaseView step4 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_ach_tabs_title, R.string.tutorial_ach_tabs_text))
                .focusOn(binding.tabsContainer)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        // Слайд 5: Просмотр коллекции (Текст по центру)
        FancyShowCaseView step5 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_ach_collection_title, R.string.tutorial_ach_collection_text))
                .titleStyle(0, Gravity.CENTER)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseQueue queue = new FancyShowCaseQueue()
                .add(step1)
                .add(step2)
                .add(step3)
                .add(step4)
                .add(step5);

        queue.setCompleteListener(() -> {
            // Передаем эстафету, чтобы в HomeFragment запустился финальный салют 🎉
            requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("tutorial_step_ach", 1)
                    .apply();
        });

        queue.show();
    }


    private void setupRecyclerView() {
        binding.recyclerAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
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

        String targetCategoryDbKey = "Recommended";
        if (selectedId == R.id.tabCollections) targetCategoryDbKey = "Collections";
        else if (selectedId == R.id.tabMilestones) targetCategoryDbKey = "Milestones";
        else if (selectedId == R.id.tabLegendary) targetCategoryDbKey = "Legendary";

        for (Achievement a : fullList) {
            if (a.category.equalsIgnoreCase(targetCategoryDbKey)) {
                if (targetCategoryDbKey.equals("Collections")) {
                    filtered.add(a); // Показываем и собранные, и несобранные
                } else {
                    if (!a.isCollected) filtered.add(a);
                }
            }
        }

        Collections.sort(filtered, new Comparator<Achievement>() {
            @Override
            public int compare(Achievement o1, Achievement o2) {
                if (o1.isCompleted && !o2.isCompleted && !o1.isCollected) return -1;
                if (!o1.isCompleted && o2.isCompleted && !o2.isCollected) return 1;

                if (!o1.isLocked && o2.isLocked) return -1;
                if (o1.isLocked && !o2.isLocked) return 1;

                return Integer.compare(o1.target, o2.target);
            }
        });

        adapter = new AchievementsAdapter(getContext(), filtered, new AchievementsAdapter.OnAchievementClickListener() {
            @Override
            public void onCollectClick(View clickedView, Achievement achievement) {
                mViewModel.collectAchievement(achievement);
                showFloatingXP(clickedView, String.valueOf(achievement.xpReward));
                Toast.makeText(getContext(), getString(R.string.achievements_completed) + " +" + achievement.xpReward + " XP!", Toast.LENGTH_SHORT).show();
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

        String translatedTitle = getTranslatedText(achievement.id, achievement.title, "_title");
        title.setText(translatedTitle);

        tier.setText(achievement.tier);
        reward.setText("+" + achievement.xpReward + " XP");

        if ("collection".equals(achievement.type)) {
            StringBuilder reqBuilder = new StringBuilder();

            if (achievement.requiredIds != null && !achievement.requiredIds.isEmpty()) {
                for (String reqId : achievement.requiredIds) {
                    Achievement reqAch = findAchievementById(reqId);
                    boolean isDone = (reqAch != null && reqAch.isCompleted);
                    String mark = isDone ? "✅ " : "❌ ";
                    String fallbackTitle = (reqAch != null) ? reqAch.title : reqId;
                    String translatedReqTitle = getTranslatedText(reqId, fallbackTitle, "_title");
                    reqBuilder.append(mark).append(translatedReqTitle).append("\n");
                }
            } else {
                reqBuilder.append(achievement.subItemsStatus);
            }

            desc.setText(getString(R.string.item_achievement_colect_these_badges) + ":\n\n" + reqBuilder.toString().trim());
            desc.setGravity(Gravity.START);

        } else {
            if (achievement.isLocked) {
                String translatedReqTitle = getTranslatedText(achievement.previousId, achievement.requiredTitle, "_title");
                desc.setText(getString(R.string.item_achievement_locked) + " '" + translatedReqTitle + "' " + getString(R.string.achievements_first) + ".");
            } else {
                String translatedDesc = getTranslatedText(achievement.id, achievement.description, "_desc");
                desc.setText(translatedDesc);
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

    private String getTranslatedText(String achievementId, String fallback, String suffix) {
        if (achievementId == null || achievementId.isEmpty() || getContext() == null) {
            return fallback;
        }
        int resId = getContext().getResources().getIdentifier(achievementId + suffix, "string", getContext().getPackageName());
        if (resId != 0) {
            return getString(resId);
        } else {
            return fallback;
        }
    }

    private Achievement findAchievementById(String id) {
        for (Achievement a : fullList) {
            if (a.id != null && a.id.equals(id)) {
                return a;
            }
        }
        return null;
    }

    private void showFloatingXP(View clickedView, String xpReward) {
        if (getActivity() == null || clickedView == null) return;
        final ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

        final View flyer = LayoutInflater.from(getContext()).inflate(R.layout.item_flying_xp, root, false);
        TextView tvXP = flyer.findViewById(R.id.tvFlyerXP);
        tvXP.setText("+" + xpReward + " XP");

        flyer.setVisibility(View.INVISIBLE);
        root.addView(flyer);

        int[] location = new int[2];
        clickedView.getLocationInWindow(location);
        int startX = location[0] + (clickedView.getWidth() / 2);
        int startY = location[1];

        flyer.post(() -> {
            flyer.setX(startX - (flyer.getWidth() / 2f));
            flyer.setY(startY - flyer.getHeight());
            flyer.setVisibility(View.VISIBLE);

            float currentY = flyer.getY();

            ObjectAnimator appearY = ObjectAnimator.ofFloat(flyer, View.Y, currentY, currentY - 50f);
            ObjectAnimator appearAlpha = ObjectAnimator.ofFloat(flyer, View.ALPHA, 0f, 1f);
            ObjectAnimator appearScaleX = ObjectAnimator.ofFloat(flyer, View.SCALE_X, 0.3f, 1.2f, 1f);
            ObjectAnimator appearScaleY = ObjectAnimator.ofFloat(flyer, View.SCALE_Y, 0.3f, 1.2f, 1f);

            AnimatorSet appearSet = new AnimatorSet();
            appearSet.playTogether(appearY, appearAlpha, appearScaleX, appearScaleY);
            appearSet.setDuration(400);
            appearSet.setInterpolator(new OvershootInterpolator());

            float topOfScreenY = -flyer.getHeight() - 100f;

            ObjectAnimator flyUp = ObjectAnimator.ofFloat(flyer, View.Y, currentY - 50f, topOfScreenY);
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(flyer, View.ALPHA, 1f, 0f);

            AnimatorSet flySet = new AnimatorSet();
            flySet.playTogether(flyUp, fadeOut);
            flySet.setDuration(600);
            flySet.setInterpolator(new AccelerateInterpolator());
            flySet.setStartDelay(500);

            AnimatorSet fullAnimation = new AnimatorSet();
            fullAnimation.playSequentially(appearSet, flySet);
            fullAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    root.removeView(flyer);
                }
            });

            fullAnimation.start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}