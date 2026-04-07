package com.example.fitnesapp.ui.water;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.R;
import com.example.fitnesapp.ui.base.BaseLoadingFragment;

public class WaterFragment extends BaseLoadingFragment {

    private WaterViewModel viewModel;
    private TextView tvWaterVolume, tvWaterPercent, tvAdviceText;
    private ImageView ivBodyFill;
    private ImageView ivWater250, ivWater500, ivWater750, ivWater1000, ivAddOwnWater;
    private FrameLayout frameSilhouette;

    private int currentAnimLevel = 0;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_water, container, false);

        tvWaterVolume = root.findViewById(R.id.tvWaterVolume);
        tvWaterPercent = root.findViewById(R.id.tvWaterPercent);
        tvAdviceText = root.findViewById(R.id.tvAdviceText);
        ivBodyFill = root.findViewById(R.id.ivBodyFill);
        ivWater250 = root.findViewById(R.id.IVwater250);
        ivWater500 = root.findViewById(R.id.IVwater500);
        ivWater750 = root.findViewById(R.id.IVwater750);
        ivWater1000 = root.findViewById(R.id.IVwater1000);
        frameSilhouette = root.findViewById(R.id.frameSilhouette);
        ivAddOwnWater = root.findViewById(R.id.IVwaterPlus);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WaterViewModel.class);

        viewModel.getConsumedWater().observe(getViewLifecycleOwner(), consumed -> updateUI());
        viewModel.getWaterGoal().observe(getViewLifecycleOwner(), goal -> updateUI());

        viewModel.getAdviceText().observe(getViewLifecycleOwner(), advice -> {
            if (tvAdviceText != null) tvAdviceText.setText(advice);
        });

        tvWaterVolume.setOnClickListener(v -> showEditWaterGoalDialog());

        // --- ИСПРАВЛЕНИЕ: Передаем View (кнопку), на которую нажали ---
        ivWater250.setOnClickListener(v -> tryAddWater(250, v));
        ivWater500.setOnClickListener(v -> tryAddWater(500, v));
        ivWater750.setOnClickListener(v -> tryAddWater(750, v));
        ivWater1000.setOnClickListener(v -> tryAddWater(1000, v));

        ivAddOwnWater.setOnClickListener(v -> showCustomWaterDialog(v));

        // --- ИСПРАВЛЕНИЕ: Добавляем анимацию брызг при удержании ---
        frameSilhouette.setOnLongClickListener(v -> {
            viewModel.returnWater();
            showSplashEffect(frameSilhouette); // Запуск брызг
            return true;
        });

        startFakeLoading(view, 200);
    }

    // ==========================================
    // ЗАЩИТА ОТ СПАМА И БАЛОВСТВА
    // ==========================================
    // --- ИСПРАВЛЕНИЕ: Добавили clickedView ---
    private void tryAddWater(int amountToAdd, View clickedView) {
        Integer currentConsumed = viewModel.getConsumedWater().getValue();
        if (currentConsumed == null) currentConsumed = 0;

        if (currentConsumed + amountToAdd > 5000) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.water_warning_spam_title))
                    .setMessage(getString(R.string.water_warning_spam_desc))
                    .setPositiveButton(getString(R.string.water_warning_yes_add), (dialog, which) -> {
                        viewModel.addWater(amountToAdd);
                        // Запускаем полет только после подтверждения
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
                        // Запускаем полет только после подтверждения
                        if (clickedView != null) showFlyingWaterDrop(clickedView);
                    })
                    .setNegativeButton(getString(R.string.water_warning_cancel), null)
                    .show();
            return;
        }

        // Если предупреждений нет - просто добавляем и запускаем полет
        viewModel.addWater(amountToAdd);
        if (clickedView != null) showFlyingWaterDrop(clickedView);
    }

    // ==========================================
    // ДИАЛОГИ ВВОДА
    // ==========================================
    // --- ИСПРАВЛЕНИЕ: Добавили anchorView ---
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
                        tryAddWater(customAmount, anchorView); // Передаем View кнопки
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
        // ... (Без изменений, этот метод просто меняет цель, анимация воды здесь не нужна) ...
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
        Integer consumed = viewModel.getConsumedWater().getValue();
        Integer goal = viewModel.getWaterGoal().getValue();

        if (consumed == null) consumed = 0;
        if (goal == null || goal == 0) goal = 2500;

        // Текст (цифры) обновляем мгновенно, чтобы пользователь сразу видел отклик
        tvWaterVolume.setText(getString(R.string.format_water_volume, consumed, goal));

        int percent = (int) (((float) consumed / goal) * 100);
        if (percent > 100) percent = 100;

        tvWaterPercent.setText(getString(R.string.format_percent, percent));

        // --- ЛОГИКА АНИМАЦИИ ЗАПОЛНЕНИЯ ---
        int targetLevel = percent * 100;
        ObjectAnimator animator = ObjectAnimator.ofInt(ivBodyFill.getDrawable(), "level", currentAnimLevel, targetLevel);
        animator.setDuration(1200); // Само заполнение длится 1.2 секунды

        // МАГИЯ ЗАДЕРЖКИ:
        if (targetLevel > currentAnimLevel && currentAnimLevel != 0) {
            // Если уровень растет (мы выпили воду), ждем ровно 600мс.
            // 600мс — это в точности время полета нашей капельки (setDuration(600) в showFlyingWaterDrop).
            animator.setStartDelay(600);
        } else if (targetLevel < currentAnimLevel) {
            // Если уровень падает (нажали отмену/Undo), ждем 200мс,
            // чтобы вода начала падать в момент появления эффекта брызг.
            animator.setStartDelay(200);
        }
        // Если currentAnimLevel == 0 (пользователь только зашел на экран),
        // задержки не будет, человечек заполнится сразу.

        animator.start();

        currentAnimLevel = targetLevel;
    }
    // ==========================================
    // АНИМАЦИИ
    // ==========================================

    private void showFlyingWaterDrop(View startView) {
        if (getActivity() == null || startView == null || frameSilhouette == null) return;
        final ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

        // Создаем ImageView с капелькой
        final ImageView drop = new ImageView(getContext());
        drop.setImageResource(R.drawable.ic_water_drops_many);
        drop.setLayoutParams(new ViewGroup.LayoutParams(150, 150));
        drop.setVisibility(View.INVISIBLE);
        root.addView(drop);

        int[] startLoc = new int[2];
        startView.getLocationInWindow(startLoc);

        int[] endLoc = new int[2];
        frameSilhouette.getLocationInWindow(endLoc);

        drop.post(() -> {
            // Центр начальной кнопки
            float startX = startLoc[0] + (startView.getWidth() / 2f) - (drop.getWidth() / 2f);
            float startY = startLoc[1] + (startView.getHeight() / 2f) - (drop.getHeight() / 2f);

            // Центр силуэта человека
            float endX = endLoc[0] + (frameSilhouette.getWidth() / 2f) - (drop.getWidth() / 2f);
            float endY = endLoc[1] + (frameSilhouette.getHeight() / 2f) - (drop.getHeight() / 2f);

            drop.setX(startX);
            drop.setY(startY);
            drop.setVisibility(View.VISIBLE);

            // Движение по кривой (отличающиеся интерполяторы для осей X и Y создают дугу)
            ObjectAnimator moveX = ObjectAnimator.ofFloat(drop, View.X, startX, endX);
            moveX.setInterpolator(new AccelerateDecelerateInterpolator());

            ObjectAnimator moveY = ObjectAnimator.ofFloat(drop, View.Y, startY, endY);
            moveY.setInterpolator(new AccelerateInterpolator());

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(drop, View.SCALE_X, 0.3f, 1.2f, 0.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(drop, View.SCALE_Y, 0.3f, 1.2f, 0.5f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(drop, View.ALPHA, 0f, 1f, 0.5f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(moveX, moveY, scaleX, scaleY, alpha);
            set.setDuration(600); // 0.6 секунды на полет
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    root.removeView(drop); // Удаляем капельку

                    // Легкая пульсация силуэта при попадании
                    ObjectAnimator pulseX = ObjectAnimator.ofFloat(frameSilhouette, View.SCALE_X, 1f, 1.05f, 1f);
                    ObjectAnimator pulseY = ObjectAnimator.ofFloat(frameSilhouette, View.SCALE_Y, 1f, 1.05f, 1f);
                    pulseX.setDuration(200).start();
                    pulseY.setDuration(200).start();
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

        // Центр силуэта человека
        int centerX = location[0] + targetView.getWidth() / 2;
        int centerY = location[1] + targetView.getHeight() / 2;

        int numDrops = 8; // Количество разлетающихся капель

        for (int i = 0; i < numDrops; i++) {
            final ImageView drop = new ImageView(getContext());
            drop.setImageResource(R.drawable.ic_water_drop);
            drop.setLayoutParams(new ViewGroup.LayoutParams(50, 50));
            root.addView(drop);

            drop.setX(centerX - 25);
            drop.setY(centerY - 25);

            // Случайный угол и расстояние для каждой капельки
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
            // Случайная длительность (от 400 до 600 мс), чтобы капли разлетались неоднородно
            set.setDuration(400 + (long)(Math.random() * 200));
            set.setInterpolator(new DecelerateInterpolator()); // Замедляются к концу полета
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    root.removeView(drop); // Удаляем с экрана
                }
            });
            set.start();
        }

        // Резкое "сжатие" силуэта при отмене воды
        ObjectAnimator squeezeX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 1f, 0.9f, 1f);
        ObjectAnimator squeezeY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 1f, 0.9f, 1f);
        squeezeX.setDuration(300).start();
        squeezeY.setDuration(300).start();
    }
}