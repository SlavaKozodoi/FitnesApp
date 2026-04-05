package com.example.fitnesapp.ui.water;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

        // Подписки на UI
        viewModel.getConsumedWater().observe(getViewLifecycleOwner(), consumed -> updateUI());
        viewModel.getWaterGoal().observe(getViewLifecycleOwner(), goal -> updateUI());

        viewModel.getAdviceText().observe(getViewLifecycleOwner(), advice -> {
            if (tvAdviceText != null) tvAdviceText.setText(advice);
        });

        // КЛИКИ
        tvWaterVolume.setOnClickListener(v -> showEditWaterGoalDialog());

        // КЛИКИ ДОБАВЛЕНИЯ
        ivWater250.setOnClickListener(v -> tryAddWater(250));
        ivWater500.setOnClickListener(v -> tryAddWater(500));
        ivWater750.setOnClickListener(v -> tryAddWater(750));
        ivWater1000.setOnClickListener(v -> tryAddWater(1000));
        ivAddOwnWater.setOnClickListener(v -> showCustomWaterDialog());

        // Отмена (Undo) при долгом нажатии на силуэт
        frameSilhouette.setOnLongClickListener(v -> {
            viewModel.returnWater();
            return true;
        });

        startFakeLoading(view, 200);
    }

    // ==========================================
    // ЗАЩИТА ОТ СПАМА И БАЛОВСТВА
    // ==========================================
    private void tryAddWater(int amountToAdd) {
        Integer currentConsumed = viewModel.getConsumedWater().getValue();
        if (currentConsumed == null) currentConsumed = 0;

        if (currentConsumed + amountToAdd > 5000) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.water_warning_spam_title))
                    .setMessage(getString(R.string.water_warning_spam_desc))
                    .setPositiveButton(getString(R.string.water_warning_yes_add), (dialog, which) -> {
                        viewModel.addWater(amountToAdd);
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
                    })
                    .setNegativeButton(getString(R.string.water_warning_cancel), null)
                    .show();
            return;
        }

        viewModel.addWater(amountToAdd);
    }

    // ==========================================
    // ДИАЛОГИ ВВОДА (ОДНА РАЗМЕТКА НА ДВА ДЕЙСТВИЯ)
    // ==========================================

    // 1. Диалог для ввода СВОЕГО ОБЪЕМА выпитой воды
    private void showCustomWaterDialog() {
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
                        tryAddWater(customAmount);
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

    // 2. Диалог для ИЗМЕНЕНИЯ ДНЕВНОЙ НОРМЫ
    private void showEditWaterGoalDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(getString(R.string.water_dialog_edit_goal_title));

        EditText etAmount = customView.findViewById(R.id.etGoalInput);

        Integer currentGoal = viewModel.getWaterGoal().getValue();
        if (currentGoal != null) {
            etAmount.setText(String.valueOf(currentGoal));
        }

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

        // Используем локализованный формат (например: "%1$d / %2$d ml")
        tvWaterVolume.setText(getString(R.string.format_water_volume, consumed, goal));

        int percent = (int) (((float) consumed / goal) * 100);
        if (percent > 100) percent = 100;

        // Используем формат процента из предыдущих настроек (например: "%1$d%%")
        tvWaterPercent.setText(getString(R.string.format_percent, percent));

        // Плавная анимация воды
        int targetLevel = percent * 100;
        ObjectAnimator animator = ObjectAnimator.ofInt(ivBodyFill.getDrawable(), "level", currentAnimLevel, targetLevel);
        animator.setDuration(1200);
        animator.start();

        currentAnimLevel = targetLevel;
    }
}