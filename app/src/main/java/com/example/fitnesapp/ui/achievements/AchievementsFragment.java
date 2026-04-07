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

        // ИСПРАВЛЕНИЕ 1: Сравниваем строго с АНГЛИЙСКИМИ ключами из базы данных Firebase,
        // иначе при переключении языка фильтр сломается и список будет пустым!
        String targetCategoryDbKey = "Recommended";
        if (selectedId == R.id.tabCollections) targetCategoryDbKey = "Collections";
        else if (selectedId == R.id.tabMilestones) targetCategoryDbKey = "Milestones";
        else if (selectedId == R.id.tabLegendary) targetCategoryDbKey = "Legendary";

        for (Achievement a : fullList) {
            // Проверяем совпадение с ключом из БД
            if (a.category.equalsIgnoreCase(targetCategoryDbKey)) {
                if (targetCategoryDbKey.equals("Collections")) {
                    if (a.isCollected) filtered.add(a);
                    else if (!a.isCollected) filtered.add(a);
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

        // Переводим заголовок для диалога
        String translatedTitle = getTranslatedText(achievement.id, achievement.title, "_title");
        title.setText(translatedTitle);

        tier.setText(achievement.tier);
        reward.setText("+" + achievement.xpReward + " XP");

        // --- ЛОГИКА ДИАЛОГА ---
        if ("collection".equals(achievement.type)) {

            // ИСПРАВЛЕНИЕ: Динамически собираем список требований с ПЕРЕВОДАМИ!
            StringBuilder reqBuilder = new StringBuilder();

            if (achievement.requiredIds != null && !achievement.requiredIds.isEmpty()) {
                for (String reqId : achievement.requiredIds) {
                    // Ищем требуемую ачивку в общем списке
                    Achievement reqAch = findAchievementById(reqId);

                    // Ставим галочку или крестик
                    boolean isDone = (reqAch != null && reqAch.isCompleted);
                    String mark = isDone ? "✅ " : "❌ ";

                    // Переводим её название!
                    String fallbackTitle = (reqAch != null) ? reqAch.title : reqId;
                    String translatedReqTitle = getTranslatedText(reqId, fallbackTitle, "_title");

                    reqBuilder.append(mark).append(translatedReqTitle).append("\n");
                }
            } else {
                // Страховка: если список ID пуст, берем старый текст
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
    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ПЕРЕВОДОВ В ДИАЛОГЕ
    // ==========================================
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
    // ==========================================
    // ПОИСК АЧИВКИ ПО ID ДЛЯ КОЛЛЕКЦИЙ
    // ==========================================
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

            // --- ФАЗА 1: Выпрыгивание (чуть-чуть подлетает над кнопкой) ---
            // ИСПРАВЛЕНИЕ: Используем View.Y вместо TRANSLATION_Y для точного позиционирования
            ObjectAnimator appearY = ObjectAnimator.ofFloat(flyer, View.Y, currentY, currentY - 50f);
            ObjectAnimator appearAlpha = ObjectAnimator.ofFloat(flyer, View.ALPHA, 0f, 1f);
            ObjectAnimator appearScaleX = ObjectAnimator.ofFloat(flyer, View.SCALE_X, 0.3f, 1.2f, 1f);
            ObjectAnimator appearScaleY = ObjectAnimator.ofFloat(flyer, View.SCALE_Y, 0.3f, 1.2f, 1f);

            AnimatorSet appearSet = new AnimatorSet();
            appearSet.playTogether(appearY, appearAlpha, appearScaleX, appearScaleY);
            appearSet.setDuration(400);
            appearSet.setInterpolator(new OvershootInterpolator());

            // --- ФАЗА 2: Улетает в самый верх экрана ---
            // ИСПРАВЛЕНИЕ: Вычисляем координату за пределами верхнего края экрана (минус высота плашки и еще чуть-чуть)
            float topOfScreenY = -flyer.getHeight() - 100f;

            ObjectAnimator flyUp = ObjectAnimator.ofFloat(flyer, View.Y, currentY - 50f, topOfScreenY);
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(flyer, View.ALPHA, 1f, 0f);

            AnimatorSet flySet = new AnimatorSet();
            flySet.playTogether(flyUp, fadeOut);
            // Можешь увеличить время (например, 700), если хочешь чтобы летело чуть медленнее
            flySet.setDuration(600);
            flySet.setInterpolator(new AccelerateInterpolator());
            flySet.setStartDelay(500);

            // --- Запускаем всё ---
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