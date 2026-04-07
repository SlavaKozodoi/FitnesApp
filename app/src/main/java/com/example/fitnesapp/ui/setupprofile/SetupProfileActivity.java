package com.example.fitnesapp.ui.setupprofile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesapp.MainActivity;
import com.example.fitnesapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SetupProfileActivity extends AppCompatActivity {

    private EditText etName, etSurname, etWeight, etHeight, etBirthDate;
    private RadioGroup rgGender;
    private Button btnSave;
    private DatabaseReference mDatabase;
    private final int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ВАЖНО: Ссылка теперь указывает на корень пользователя (users/{uid}),
        // чтобы мы могли писать и в "profile", и в "health_logs"
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etBirthDate = findViewById(R.id.etBirthDate);
        rgGender = findViewById(R.id.rgGender);
        btnSave = findViewById(R.id.btnSaveProfile);

        // Логика форматирования даты (без изменений)
        etBirthDate.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                String str = s.toString();
                String nums = str.replaceAll("[^\\d]", "");

                if (nums.length() >= 2) {
                    try {
                        int day = Integer.parseInt(nums.substring(0, 2));
                        if (day > 31) nums = "31" + nums.substring(2);
                    } catch (NumberFormatException e) {}
                }

                if (nums.length() >= 4) {
                    try {
                        int month = Integer.parseInt(nums.substring(2, 4));
                        if (month > 12) nums = nums.substring(0, 2) + "12" + nums.substring(4);
                    } catch (NumberFormatException e) {}
                }

                if (nums.length() >= 8) {
                    try {
                        int year = Integer.parseInt(nums.substring(4, 8));
                        if (year > currentYear) nums = nums.substring(0, 4) + currentYear;
                    } catch (NumberFormatException e) {}
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < nums.length(); i++) {
                    sb.append(nums.charAt(i));
                    if ((i == 1 || i == 3) && i < nums.length() - 1) {
                        sb.append('.');
                    }
                }

                if (sb.length() > 10) sb.setLength(10);
                etBirthDate.setText(sb.toString());
                etBirthDate.setSelection(sb.length());

                isUpdating = false;
            }
        });

        btnSave.setOnClickListener(v -> saveAndContinue());
    }

    private void saveAndContinue() {
        String nameStr = etName.getText().toString().trim();
        String surnameStr = etSurname.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();

        // 1. Базовые проверки на пустоту
        if (nameStr.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(this, getString(R.string.setup_profile_error_fill_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (birthDate.length() < 10) {
            etBirthDate.setError(getString(R.string.setup_profile_error_invalid_date));
            etBirthDate.requestFocus();
            return;
        }
        String yearOnly = birthDate.substring(6);
        if (Integer.parseInt(yearOnly)  < currentYear-100) {
            etBirthDate.setError(getString(R.string.setup_profile_error_invalid_date_year));
            etBirthDate.requestFocus();
            return;
        }
        if (Integer.parseInt(yearOnly)  > currentYear-13) {
            etBirthDate.setError(getString(R.string.setup_profile_error_invalid_age));
            etBirthDate.requestFocus();
            return;
        }


        double weightVal;
        int heightVal;

        try {
            weightVal = Double.parseDouble(weightStr);
            heightVal = Integer.parseInt(heightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.setup_profile_error_invalid_number), Toast.LENGTH_SHORT).show();
            return;
        }

        // ==========================================
        // 2. ЖЕСТКАЯ ВАЛИДАЦИЯ (Физически невозможно)
        // ==========================================
        if (heightVal < 50 || heightVal > 300) {
            etHeight.setError(getString(R.string.setup_profile_error_impossible_height));
            etHeight.requestFocus();
            return;
        }

        if (weightVal < 20 || weightVal > 500) {
            etWeight.setError(getString(R.string.setup_profile_error_impossible_weight));
            etWeight.requestFocus();
            return;
        }

        // === 3. ПОДГОТОВКА ДАННЫХ ДЛЯ ЗАПИСИ ===
        int selectedId = rgGender.getCheckedRadioButtonId();
        String gender = (selectedId == R.id.rbFemale) ? "Female" : "Male";

        Map<String, Object> allUpdates = new HashMap<>();
        allUpdates.put("profile/firstName", nameStr);
        allUpdates.put("profile/secondName", surnameStr);
        allUpdates.put("profile/height", heightVal);
        allUpdates.put("profile/birthDate", birthDate);
        allUpdates.put("profile/gender", gender);
        allUpdates.put("profile/maxXp", 1000);
        allUpdates.put("profile/totalXP", 0);
        allUpdates.put("profile/notificationsEnabled", true);

        String weightKey = mDatabase.child("health_logs").child("weight_history").push().getKey();
        long timestamp = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Map<String, Object> weightData = new HashMap<>();
        weightData.put("val", weightVal);
        weightData.put("timestamp", timestamp);
        weightData.put("date", dateStr);

        if (weightKey != null) {
            allUpdates.put("health_logs/weight_history/" + weightKey, weightData);
        }

        // ==========================================
        // 4. МЯГКАЯ ВАЛИДАЦИЯ (Подозрительные цифры)
        // ==========================================
        boolean isHeightSuspicious = heightVal >= 220 || heightVal <= 130;
        boolean isWeightSuspicious = weightVal >= 140 || weightVal <= 40;

        if (isHeightSuspicious || isWeightSuspicious) {
            // Формируем текст для уточнения
            String warningMsg = "";
            if (isHeightSuspicious) {
                warningMsg += getString(R.string.setup_profile_confirm_height, heightVal) + "\n";
            }
            if (isWeightSuspicious) {
                warningMsg += getString(R.string.setup_profile_confirm_weight, (int)weightVal) + "\n";
            }
            warningMsg += getString(R.string.setup_profile_confirm_ask);

            // Показываем диалог
            showConfirmationDialog(warningMsg, allUpdates);
        } else {
            // Если все в пределах нормы — просто сохраняем сразу
            executeFirebaseSave(allUpdates);
        }
    }

    // --- МЕТОД ДЛЯ ПОКАЗА ДИАЛОГА УТОЧНЕНИЯ ---
    private void showConfirmationDialog(String message, Map<String, Object> dataToSave) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.setup_profile_confirm_title)) // "Уточнення"
                .setMessage(message)
                .setPositiveButton(getString(R.string.setup_profile_confirm_yes), (dialog, which) -> {
                    // Пользователь подтвердил, что он гигант — сохраняем!
                    executeFirebaseSave(dataToSave);
                })
                .setNegativeButton(getString(R.string.setup_profile_confirm_no), (dialog, which) -> {
                    // Пользователь ошибся — просто закрываем диалог, пусть исправляет
                    dialog.dismiss();
                })
                .setCancelable(false) // Чтобы не закрыли случайным кликом мимо окна
                .show();
    }

    // --- МЕТОД ДЛЯ ФАКТИЧЕСКОЙ ЗАПИСИ В БД ---
    private void executeFirebaseSave(Map<String, Object> allUpdates) {
        mDatabase.updateChildren(allUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                finishAffinity();
            } else {
                Toast.makeText(this, getString(R.string.setup_profile_error_saving, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}