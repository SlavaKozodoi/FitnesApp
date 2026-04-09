package com.example.fitnesapp.ui.water;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentWaterBinding;
import com.example.fitnesapp.ui.base.BaseLoadingFragment;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;

public class WaterFragment extends BaseLoadingFragment {

    private WaterViewModel viewModel;
    private FragmentWaterBinding binding;

    private int currentAnimLevel = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWaterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WaterViewModel.class);

        viewModel.getConsumedWater().observe(getViewLifecycleOwner(), consumed -> updateUI());
        viewModel.getWaterGoal().observe(getViewLifecycleOwner(), goal -> updateUI());

        viewModel.getAdviceText().observe(getViewLifecycleOwner(), advice -> {
            if (binding.tvAdviceText != null) binding.tvAdviceText.setText(advice);
        });

        binding.tvWaterVolume.setOnClickListener(v -> showEditWaterGoalDialog());

        // Кнопки добавления воды
        binding.IVwater250.setOnClickListener(v -> tryAddWater(250, v));
        binding.IVwater500.setOnClickListener(v -> tryAddWater(500, v));
        binding.IVwater750.setOnClickListener(v -> tryAddWater(750, v));
        binding.IVwater1000.setOnClickListener(v -> tryAddWater(1000, v));

        binding.IVwaterPlus.setOnClickListener(v -> showCustomWaterDialog(v));

        // Отмена воды (долгое нажатие)
        binding.frameSilhouette.setOnLongClickListener(v -> {
            viewModel.returnWater();
            showSplashEffect(binding.frameSilhouette);
            return true;
        });

        startFakeLoading(view, 200);

        // --- ЛОВИМ ШАГ 3 ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        if (prefs.getInt("tutorial_step", 0) == 3 ) {
            // Даем экрану полсекунды на загрузку
            view.postDelayed(this::showTutorial, 600);
        }
    }

    // Вспомогательный метод для склейки заголовка и текста из ресурсов
    private String getTutorialText(int titleResId, int descResId) {
        return getString(titleResId) + "\n\n" + getString(descResId);
    }

    private void showTutorial() {
        if (binding == null) return;

        FancyShowCaseView step1 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_water_balance_title, R.string.tutorial_water_balance_text))
                .focusOn(binding.tvWaterPercent)
                .titleStyle(0, Gravity.CENTER)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseView step2 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.llWaterButtons)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .title(getTutorialText(R.string.tutorial_water_add_title, R.string.tutorial_water_add_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseView step3 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.tvWaterPercent)
                .focusShape(FocusShape.CIRCLE)
                .fitSystemWindows(true)
                .roundRectRadius(20)
                .title(getTutorialText(R.string.tutorial_water_undo_title, R.string.tutorial_water_undo_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseView step4 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.tvWaterVolume)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .roundRectRadius(30)
                .title(getTutorialText(R.string.tutorial_water_goal_title, R.string.tutorial_water_goal_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseView step5 = new FancyShowCaseView.Builder(requireActivity())
                .focusOn(binding.cardAdvice)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .roundRectRadius(30)
                .title(getTutorialText(R.string.tutorial_water_advice_title, R.string.tutorial_water_advice_text))
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseQueue queue = new FancyShowCaseQueue()
                .add(step1)
                .add(step2)
                .add(step3)
                .add(step4)
                .add(step5);

        queue.setCompleteListener(() -> {
            // Передаем эстафету дальше (Шаг 4 или финал)
            requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("tutorial_step", 4)
                    .apply();
        });

        queue.show();
    }


    // ==========================================
    // ЗАЩИТА ОТ СПАМА И БАЛОВСТВА
    // ==========================================
    private void tryAddWater(int amountToAdd, View clickedView) {
        Integer currentConsumed = viewModel.getConsumedWater().getValue();
        if (currentConsumed == null) currentConsumed = 0;

        if (currentConsumed + amountToAdd > 5000) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.water_warning_spam_title))
                    .setMessage(getString(R.string.water_warning_spam_desc))
                    .setPositiveButton(getString(R.string.water_warning_yes_add), (dialog, which) -> {
                        viewModel.addWater(amountToAdd);
                        if (clickedView != null) showFlyingWaterDrop(clickedView);
                    })
                    .setNegativeButton(getString(R.string.water_warning_cancel), null)
                    .show();
            return;
        }

        if (amountToAdd > 1500) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.water_warning_caution_title))
                    .setMessage(getString(R.string.water_warning_caution_desc))
                    .setPositiveButton(getString(R.string.water_warning_yes_drank), (dialog, which) -> {
                        viewModel.addWater(amountToAdd);
                        if (clickedView != null) showFlyingWaterDrop(clickedView);
                    })
                    .setNegativeButton(getString(R.string.water_warning_cancel), null)
                    .show();
            return;
        }

        viewModel.addWater(amountToAdd);
        if (clickedView != null) showFlyingWaterDrop(clickedView);
    }

    // ==========================================
    // ДИАЛОГИ ВВОДА
    // ==========================================
    private void showCustomWaterDialog(View anchorView) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(getString(R.string.water_dialog_custom_volume_title));

        EditText etAmount = customView.findViewById(R.id.etGoalInput);
        TextView btnCancel = customView.findViewById(R.id.btnCancel);
        TextView btnSave = customView.findViewById(R.id.btnSave);

        builder.setView(customView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String input = etAmount.getText().toString().trim();
            if (!input.isEmpty()) {
                try {
                    int customAmount = Integer.parseInt(input);
                    if (customAmount > 0) {
                        tryAddWater(customAmount, anchorView);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.water_error_valid_amount), Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), getString(R.string.water_error_invalid_number), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.water_error_empty_field), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEditWaterGoalDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(getString(R.string.water_dialog_edit_goal_title));

        EditText etAmount = customView.findViewById(R.id.etGoalInput);
        Integer currentGoal = viewModel.getWaterGoal().getValue();
        if (currentGoal != null) etAmount.setText(String.valueOf(currentGoal));

        TextView btnCancel = customView.findViewById(R.id.btnCancel);
        TextView btnSave = customView.findViewById(R.id.btnSave);

        builder.setView(customView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String input = etAmount.getText().toString().trim();
            if (!input.isEmpty()) {
                try {
                    int customGoal = Integer.parseInt(input);
                    if (customGoal >= 500 && customGoal <= 8000) {
                        viewModel.setCustomWaterGoal(customGoal);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.water_toast_goal_updated), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.water_error_realistic_goal), Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), getString(R.string.water_error_invalid_number), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.water_error_empty_field), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    // ==========================================
    // ОБНОВЛЕНИЕ UI
    // ==========================================
    private void updateUI() {
        if (binding == null) return;

        Integer consumed = viewModel.getConsumedWater().getValue();
        Integer goal = viewModel.getWaterGoal().getValue();

        if (consumed == null) consumed = 0;
        if (goal == null || goal == 0) goal = 2500;

        binding.tvWaterVolume.setText(getString(R.string.format_water_volume, consumed, goal));

        int percent = (int) (((float) consumed / goal) * 100);
        if (percent > 100) percent = 100;

        binding.tvWaterPercent.setText(getString(R.string.format_percent, percent));

        int targetLevel = percent * 100;
        ObjectAnimator animator = ObjectAnimator.ofInt(binding.ivBodyFill.getDrawable(), "level", currentAnimLevel, targetLevel);
        animator.setDuration(1200);

        if (targetLevel > currentAnimLevel && currentAnimLevel != 0) {
            animator.setStartDelay(600);
        } else if (targetLevel < currentAnimLevel) {
            animator.setStartDelay(200);
        }

        animator.start();
        currentAnimLevel = targetLevel;
    }

    // ==========================================
    // АНИМАЦИИ
    // ==========================================
    private void showFlyingWaterDrop(View startView) {
        if (getActivity() == null || startView == null || binding == null) return;
        final ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

        final ImageView drop = new ImageView(getContext());
        drop.setImageResource(R.drawable.ic_water_drops_many);
        drop.setLayoutParams(new ViewGroup.LayoutParams(150, 150));
        drop.setVisibility(View.INVISIBLE);
        root.addView(drop);

        int[] startLoc = new int[2];
        startView.getLocationInWindow(startLoc);

        int[] endLoc = new int[2];
        binding.frameSilhouette.getLocationInWindow(endLoc);

        drop.post(() -> {
            float startX = startLoc[0] + (startView.getWidth() / 2f) - (drop.getWidth() / 2f);
            float startY = startLoc[1] + (startView.getHeight() / 2f) - (drop.getHeight() / 2f);

            float endX = endLoc[0] + (binding.frameSilhouette.getWidth() / 2f) - (drop.getWidth() / 2f);
            float endY = endLoc[1] + (binding.frameSilhouette.getHeight() / 2f) - (drop.getHeight() / 2f);

            drop.setX(startX);
            drop.setY(startY);
            drop.setVisibility(View.VISIBLE);

            ObjectAnimator moveX = ObjectAnimator.ofFloat(drop, View.X, startX, endX);
            moveX.setInterpolator(new AccelerateDecelerateInterpolator());

            ObjectAnimator moveY = ObjectAnimator.ofFloat(drop, View.Y, startY, endY);
            moveY.setInterpolator(new AccelerateInterpolator());

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(drop, View.SCALE_X, 0.3f, 1.2f, 0.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(drop, View.SCALE_Y, 0.3f, 1.2f, 0.5f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(drop, View.ALPHA, 0f, 1f, 0.5f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(moveX, moveY, scaleX, scaleY, alpha);
            set.setDuration(600);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    root.removeView(drop);
                    if (binding != null) {
                        ObjectAnimator pulseX = ObjectAnimator.ofFloat(binding.frameSilhouette, View.SCALE_X, 1f, 1.05f, 1f);
                        ObjectAnimator pulseY = ObjectAnimator.ofFloat(binding.frameSilhouette, View.SCALE_Y, 1f, 1.05f, 1f);
                        pulseX.setDuration(200).start();
                        pulseY.setDuration(200).start();
                    }
                }
            });
            set.start();
        });
    }

    private void showSplashEffect(View targetView) {
        if (getActivity() == null || targetView == null) return;
        final ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

        int[] location = new int[2];
        targetView.getLocationInWindow(location);

        int centerX = location[0] + targetView.getWidth() / 2;
        int centerY = location[1] + targetView.getHeight() / 2;

        int numDrops = 8;

        for (int i = 0; i < numDrops; i++) {
            final ImageView drop = new ImageView(getContext());
            drop.setImageResource(R.drawable.ic_water_drop);
            drop.setLayoutParams(new ViewGroup.LayoutParams(50, 50));
            root.addView(drop);

            drop.setX(centerX - 25);
            drop.setY(centerY - 25);

            double angle = Math.random() * 2 * Math.PI;
            int distance = 150 + (int)(Math.random() * 150);

            float endX = centerX + (float) (distance * Math.cos(angle)) - 25;
            float endY = centerY + (float) (distance * Math.sin(angle)) - 25;

            ObjectAnimator animX = ObjectAnimator.ofFloat(drop, View.X, drop.getX(), endX);
            ObjectAnimator animY = ObjectAnimator.ofFloat(drop, View.Y, drop.getY(), endY);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(drop, View.ALPHA, 1f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(drop, View.SCALE_X, 0.5f, 1.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(drop, View.SCALE_Y, 0.5f, 1.5f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(animX, animY, alpha, scaleX, scaleY);
            set.setDuration(400 + (long)(Math.random() * 200));
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    root.removeView(drop);
                }
            });
            set.start();
        }

        ObjectAnimator squeezeX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 1f, 0.9f, 1f);
        ObjectAnimator squeezeY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 1f, 0.9f, 1f);
        squeezeX.setDuration(300).start();
        squeezeY.setDuration(300).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}