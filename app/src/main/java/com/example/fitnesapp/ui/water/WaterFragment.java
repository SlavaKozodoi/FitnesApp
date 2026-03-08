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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.R;
import com.example.fitnesapp.ui.base.BaseLoadingFragment;

public class WaterFragment extends BaseLoadingFragment {

    private WaterViewModel viewModel;
    private TextView tvWaterVolume, tvWaterPercent, tvAdviceText;
    private ImageView ivBodyFill;
    private ImageView ivWater250, ivWater500, ivWater750, ivWater1000, ivAddOwnWater;
    private FrameLayout frameSilhouette;

    // Элементы вечернего фидбека
    private LinearLayout llFeedback;
    private Button btnFeedbackTooMuch, btnFeedbackPerfect, btnFeedbackThirsty;

    private int currentAnimLevel = 0;

    // ==========================================
    // ЛОКАЦИЯ ДЛЯ ПОГОДЫ
    // ==========================================
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getLocationAndFetchWeather();
                } else {
                    viewModel.calculateWithoutWeather();
                }
            });

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

        llFeedback = root.findViewById(R.id.llFeedback);
        btnFeedbackTooMuch = root.findViewById(R.id.btnFeedbackTooMuch);
        btnFeedbackPerfect = root.findViewById(R.id.btnFeedbackPerfect);
        btnFeedbackThirsty = root.findViewById(R.id.btnFeedbackThirsty);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WaterViewModel.class);

        // Проверка разрешений на гео (для погоды)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocationAndFetchWeather();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Подписки на UI
        viewModel.getConsumedWater().observe(getViewLifecycleOwner(), consumed -> updateUI());
        viewModel.getWaterGoal().observe(getViewLifecycleOwner(), goal -> updateUI());

        viewModel.getAdviceText().observe(getViewLifecycleOwner(), advice -> {
            if (tvAdviceText != null) tvAdviceText.setText(advice);
        });

        // Кнопки обратной связи (вечернее обучение ИИ)
        btnFeedbackTooMuch.setOnClickListener(v -> viewModel.submitFeedback(1));
        btnFeedbackPerfect.setOnClickListener(v -> viewModel.submitFeedback(2));
        btnFeedbackThirsty.setOnClickListener(v -> viewModel.submitFeedback(3));

        viewModel.getIsFeedbackGiven().observe(getViewLifecycleOwner(), hasVoted -> {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            // Показываем опрос только после 20:00, если пользователь еще не голосовал
            if (hour >= 20 && !hasVoted) {
                llFeedback.setVisibility(View.VISIBLE);
            } else {
                llFeedback.setVisibility(View.GONE);
            }
        });

        // КЛИКИ ДОБАВЛЕНИЯ (Теперь идут через нашу проверку на "спам" и баловство)
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

    @SuppressLint("MissingPermission")
    private void getLocationAndFetchWeather() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location != null) {
                viewModel.fetchWeatherAndCalculate(location.getLatitude(), location.getLongitude());
            } else {
                viewModel.calculateWithoutWeather();
            }
        } else {
            viewModel.calculateWithoutWeather();
        }
    }

    // ==========================================
    // ЗАЩИТА ОТ СПАМА И БАЛОВСТВА
    // ==========================================
    private void tryAddWater(int amountToAdd) {
        Integer currentConsumed = viewModel.getConsumedWater().getValue();
        if (currentConsumed == null) currentConsumed = 0;

        // Если общая сумма за день превысит 5 литров (опасность интоксикации)
        if (currentConsumed + amountToAdd > 5000) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Перебор с водой?")
                    .setMessage("Вы добавили уже очень много воды за сегодня (больше 5 литров!). Избыток воды может привести к водной интоксикации и вымыванию солей. Добавить всё равно?")
                    .setPositiveButton("Да, добавить", (dialog, which) -> {
                        viewModel.addWater(amountToAdd);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            return;
        }

        // Если добавляет больше 1500 мл за один раз (даже если общая сумма еще норм)
        if (amountToAdd > 1500) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Осторожно!")
                    .setMessage("Выпить более 1.5 литров воды за один раз может быть вредно для почек. Вы уверены, что выпили так много за раз?")
                    .setPositiveButton("Да, я выпил столько", (dialog, which) -> {
                        viewModel.addWater(amountToAdd);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            return;
        }

        // Если всё в порядке — просто добавляем воду
        viewModel.addWater(amountToAdd);
    }

    // ==========================================
    // ДИАЛОГ СВОЕГО ОБЪЕМА
    // ==========================================
    private void showCustomWaterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText("Add custom volume");

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
                        // ПРОПУСКАЕМ ВВЕДЕННОЕ ЗНАЧЕНИЕ ЧЕРЕЗ НАШУ ПРОВЕРКУ НА БАЛОВСТВО
                        tryAddWater(customAmount);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Field cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateUI() {
        Integer consumed = viewModel.getConsumedWater().getValue();
        Integer goal = viewModel.getWaterGoal().getValue();

        if (consumed == null) consumed = 0;
        if (goal == null || goal == 0) goal = 2500;

        tvWaterVolume.setText(consumed + " / " + goal + " ml");

        int percent = (int) (((float) consumed / goal) * 100);
        if (percent > 100) percent = 100;

        tvWaterPercent.setText(percent + "%");

        // Плавная анимация воды
        int targetLevel = percent * 100;
        ObjectAnimator animator = ObjectAnimator.ofInt(ivBodyFill.getDrawable(), "level", currentAnimLevel, targetLevel);
        animator.setDuration(1200);
        animator.start();

        currentAnimLevel = targetLevel;
    }
}