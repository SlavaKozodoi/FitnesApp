package com.example.fitnesapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Запускаем настройку данных и анимацию
        setupDashboard();

        return root;
    }

    private void setupDashboard() {
        // === 1. ЗАДАЕМ ЦЕЛИ И ТЕКУЩИЕ ЗНАЧЕНИЯ ===
        // (В будущем берите их из ViewModel или базы данных)
        float caloriesGoal = 2000f;
        float caloriesCurrent = 1250f;

        float stepsGoal = 10000f;
        float stepsCurrent = 6500f;

        float nutritionGoal = 2000f;
        float nutritionCurrent = 1350f;

        // === 2. НАСТРАИВАЕМ ГРАФИКИ (АРКИ) ===
        // Хитрость: так как мы видим только половину круга (верхнюю),
        // мы должны умножить цель на 2. Тогда 50% круга будет выглядеть как 100% шкалы.

        // Калории
        binding.progressCalories.setProgressMax(caloriesGoal );
        binding.progressCalories.setProgressWithAnimation(caloriesCurrent, 1000L); // 1 сек анимация

        // Шаги
        binding.progressSteps.setProgressMax(stepsGoal );
        binding.progressSteps.setProgressWithAnimation(stepsCurrent, 1000L);

        // Питание
        binding.progressNutrition.setProgressMax(nutritionGoal );
        binding.progressNutrition.setProgressWithAnimation(nutritionCurrent, 1000L);

        // === 3. ОБНОВЛЯЕМ ЦИФРЫ ВНИЗУ ===
        // Здесь показываем реальные значения, без умножения
        if (binding.tvCaloriesValue != null) {
            binding.tvCaloriesValue.setText(String.valueOf((int) caloriesCurrent));
        }
        binding.tvCaloriesGoal.setText("/" + (int) caloriesGoal + "kcal");

        if (binding.tvStepsValue != null) {
            binding.tvStepsValue.setText(String.valueOf((int) stepsCurrent));
        }
        binding.tvStepsGoal.setText("/" + (int) stepsGoal + "steps");
        if (binding.tvNutritionValue != null) {
            binding.tvNutritionValue.setText(String.valueOf((int) nutritionCurrent));
        }
        binding.tvNutritionGoal.setText("/" + (int) nutritionGoal + "kcal");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}