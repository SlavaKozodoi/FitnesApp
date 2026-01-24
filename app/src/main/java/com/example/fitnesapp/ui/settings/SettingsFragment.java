package com.example.fitnesapp.ui.settings;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentSettingsBinding;
import com.example.fitnesapp.ui.auth.AuthActivity;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SettingsViewModel mViewModel;
    private FragmentSettingsBinding binding;

    // Списки для спиннеров, чтобы можно было найти индекс
    private final String[] goalsArray = {"Lose Weight", "Maintain Weight", "Gain Muscle"};
    private final String[] activityArray = {"Sedentary (No sport)", "Light (1-3 days/week)", "Moderate (3-5 days/week)", "Active (Daily)"};

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupSpinners();
        setupObservers();
        setupListeners();
    }

    private void setupSpinners() {
        // Настройка спиннера целей
        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, goalsArray);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGoal.setAdapter(goalAdapter);

        // Настройка спиннера активности
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, activityArray);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerActivity.setAdapter(activityAdapter);
    }

    private void setupObservers() {
        // 1. Загрузка Профиля
        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.etName.setText(profile.firstName);
                binding.etSecondName.setText(profile.secondName);
                binding.etHeight.setText(String.valueOf((int) profile.height));
                binding.etWeight.setText(String.valueOf(profile.weight));

                // Дата рождения (если есть поле в UI, пока нет ID в коде, но допустим это etBirthDate)
                // binding.etBirthDate.setText(profile.birthDate);

                // Пол
                if ("Male".equalsIgnoreCase(profile.gender)) {
                    binding.rbMale.setChecked(true);
                } else {
                    binding.rbFemale.setChecked(true);
                }

                binding.switchNotif.setChecked(profile.notificationsEnabled);
            }
        });

        // 2. Загрузка Целей
        mViewModel.getUserGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) {
                binding.etTargetWeight.setText(String.valueOf(goals.targetWeight));

                // Устанавливаем спиннеры
                binding.spinnerGoal.setSelection(findSpinnerIndex(goalsArray, goals.mainGoal));
                binding.spinnerActivity.setSelection(findSpinnerIndex(activityArray, goals.activityLevel));

                // Показываем текущий расчет
                binding.tvAutoCalories.setText(goals.dailyCalories + " kcal");
                binding.tvAutoSteps.setText(goals.dailySteps + " steps");
            }
        });

        // 3. Результат сохранения
        mViewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save settings.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // Кнопка Сохранить
        binding.btnSave.setOnClickListener(v -> {
            try {
                String name = binding.etName.getText().toString();
                String surname = binding.etSecondName.getText().toString();
                // String birthDate = binding.etBirthDate.getText().toString();
                String birthDate = "01.01.2000"; // Заглушка, если нет поля ввода даты

                int height = Integer.parseInt(binding.etHeight.getText().toString());
                double weight = Double.parseDouble(binding.etWeight.getText().toString());
                double targetWeight = Double.parseDouble(binding.etTargetWeight.getText().toString());

                String gender = binding.rbMale.isChecked() ? "Male" : "Female";
                boolean notif = binding.switchNotif.isChecked();

                String mainGoal = binding.spinnerGoal.getSelectedItem().toString();
                String activity = binding.spinnerActivity.getSelectedItem().toString();

                mViewModel.saveSettings(name, surname, birthDate, height, weight, gender, notif, mainGoal, targetWeight, activity);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter valid numbers for weight/height", Toast.LENGTH_SHORT).show();
            }
        });

        // Кнопка Выход
        binding.btnLogout.setOnClickListener(v -> {
            mViewModel.logout();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // Вспомогательный метод для поиска индекса в спиннере по строке
    private int findSpinnerIndex(String[] array, String value) {
        if (value == null) return 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}