package com.example.fitnesapp.ui.settings;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentSettingsBinding;
import com.example.fitnesapp.ui.auth.AuthActivity;

import androidx.health.connect.client.HealthConnectClient;

public class SettingsFragment extends Fragment {

    private SettingsViewModel mViewModel;
    private FragmentSettingsBinding binding;

    private final String[] goalsDbValues = {"Lose Weight", "Maintain Weight", "Gain Muscle"};
    private final String[] activityDbValues = {"Sedentary (No sport)", "Light (1-3 days/week)", "Moderate (3-5 days/week)", "Active (Daily)"};

    private String[] goalsDisplayValues;
    private String[] activityDisplayValues;

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

        goalsDisplayValues = getResources().getStringArray(R.array.settings_goals_array);
        activityDisplayValues = getResources().getStringArray(R.array.settings_activity_array);

        setupSpinners();
        setupObservers();
        setupListeners();
    }

    private void setupSpinners() {
        // 1. Цілі та Активність
        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, goalsDisplayValues);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGoal.setAdapter(goalAdapter);

        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, activityDisplayValues);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerActivity.setAdapter(activityAdapter);

        // 2. МОВА ДОДАТКУ (Language)
        String[] languages = getResources().getStringArray(R.array.settings_language_array);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, languages);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLanguage.setAdapter(langAdapter);

        // Читаємо поточну мову, щоб правильно встановити Spinner під час відкриття екрану
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        String currentLang = currentLocales.toLanguageTags();

        if (currentLang.startsWith("uk")) {
            binding.spinnerLanguage.setSelection(1); // Українська
        } else if (currentLang.startsWith("da")) {
            binding.spinnerLanguage.setSelection(2); // Данська
        } else {
            binding.spinnerLanguage.setSelection(0); // English (Default)
        }

        // Обробник вибору нової мови
        binding.spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tag = "en";
                if (position == 1) tag = "uk";
                if (position == 2) tag = "da";

                LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
                String activeTag = locales.toLanguageTags();

                // Змінюємо мову ТІЛЬКИ якщо вона відрізняється від поточної
                // (інакше буде нескінченний цикл перезавантажень)
                if (!activeTag.equals(tag) && !(activeTag.isEmpty() && tag.equals("en"))) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupObservers() {
        mViewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null && weight > 0) {
                binding.etWeight.setText(String.valueOf(weight));
            }
        });

        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.etName.setText(profile.firstName);
                binding.etSecondName.setText(profile.secondName);
                binding.etHeight.setText(String.valueOf((int) profile.height));

                if ("Male".equalsIgnoreCase(profile.gender)) {
                    binding.rbMale.setChecked(true);
                } else {
                    binding.rbFemale.setChecked(true);
                }

                binding.switchNotif.setChecked(profile.notificationsEnabled);
            }
        });

        mViewModel.getUserGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) {
                binding.etTargetWeight.setText(String.valueOf(goals.targetWeight));

                binding.spinnerGoal.setSelection(findSpinnerIndex(goalsDbValues, goals.mainGoal));
                binding.spinnerActivity.setSelection(findSpinnerIndex(activityDbValues, goals.activityLevel));

                String eatPrefix = "";
                String burnPrefix = "";

                if (goals.mainGoal != null) {
                    String goalType = goals.mainGoal.toLowerCase();
                    if (goalType.contains("lose")) {
                        eatPrefix = "≤ ";
                        burnPrefix = "≥ ";
                    } else if (goalType.contains("gain")) {
                        eatPrefix = "≥ ";
                        burnPrefix = "≤ ";
                    } else {
                        eatPrefix = "= ";
                        burnPrefix = "≈ ";
                    }
                }

                binding.tvAutoCalories.setText(getString(R.string.settings_format_calories, eatPrefix, goals.dailyCalories));
                binding.tvAutoSteps.setText(getString(R.string.settings_format_steps, burnPrefix, goals.dailySteps));
            }
        });

        mViewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), getString(R.string.settings_save_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), getString(R.string.settings_save_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> {
            try {
                String name = binding.etName.getText().toString();
                String surname = binding.etSecondName.getText().toString();
                String birthDate = "01.01.2000";

                int height = Integer.parseInt(binding.etHeight.getText().toString());
                double weight = Double.parseDouble(binding.etWeight.getText().toString());
                double targetWeight = Double.parseDouble(binding.etTargetWeight.getText().toString());

                String gender = binding.rbMale.isChecked() ? "Male" : "Female";
                boolean notif = binding.switchNotif.isChecked();

                int goalIndex = binding.spinnerGoal.getSelectedItemPosition();
                String mainGoal = goalsDbValues[goalIndex];

                int activityIndex = binding.spinnerActivity.getSelectedItemPosition();
                String activity = activityDbValues[activityIndex];

                mViewModel.saveSettings(name, surname, birthDate, height, weight, gender, notif, mainGoal, targetWeight, activity);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), getString(R.string.settings_error_invalid_numbers), Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            mViewModel.logout();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        binding.btnConnectHealth.setOnClickListener(v -> checkHealthConnectStatus());
    }

    private void checkHealthConnectStatus() {
        int availabilityStatus = HealthConnectClient.getSdkStatus(requireContext(), "com.google.android.apps.healthdata");

        if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
            Toast.makeText(requireContext(), getString(R.string.settings_health_connect_installed), Toast.LENGTH_SHORT).show();
            try {
                Intent intent = new Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS");
                startActivity(intent);
            } catch (Exception e) {
                // Ignore
            }
        } else if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Toast.makeText(requireContext(), getString(R.string.settings_health_connect_update), Toast.LENGTH_LONG).show();
            openPlayStoreForHealthConnect();
        } else {
            Toast.makeText(requireContext(), getString(R.string.settings_health_connect_not_installed), Toast.LENGTH_LONG).show();
            openPlayStoreForHealthConnect();
        }
    }

    private void openPlayStoreForHealthConnect() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.android.vending");
            intent.setData(Uri.parse("market://details?id=com.google.android.apps.healthdata"));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")));
        }
    }

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