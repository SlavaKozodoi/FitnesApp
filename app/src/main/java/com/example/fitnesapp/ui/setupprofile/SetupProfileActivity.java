package com.example.fitnesapp.ui.setupprofile;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitnesapp.MainActivity;
import com.example.fitnesapp.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SetupProfileActivity extends AppCompatActivity {

    private EditText etName,etSurname, etWeight, etHeight, etBirthDate;
    private RadioGroup rgGender;
    private Button btnSave;
    private DatabaseReference mDatabase;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid).child("profile");

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etBirthDate = findViewById(R.id.etBirthDate);
        rgGender = findViewById(R.id.rgGender);
        btnSave = findViewById(R.id.btnSaveProfile);

        etBirthDate.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isUpdating = false;
            // Получаем текущий год заранее
            private final int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Защита от бесконечного цикла
                if (isUpdating) {
                    return;
                }
                isUpdating = true;

                String str = s.toString();
                // Оставляем только цифры
                String nums = str.replaceAll("[^\\d]", "");

                // === ВАЛИДАЦИЯ ===

                // 1. Проверка ДНЯ (если введено хотя бы 2 цифры)
                if (nums.length() >= 2) {
                    try {
                        int day = Integer.parseInt(nums.substring(0, 2));
                        if (day > 31) {
                            // Если больше 31, меняем первые две цифры на "31"
                            nums = "31" + nums.substring(2);
                        } else if (day == 0) {
                            // Если 0, можно менять на 1, но пока оставим как есть, чтобы человек мог набрать 01
                        }
                    } catch (NumberFormatException e) {}
                }

                // 2. Проверка МЕСЯЦА (если введено хотя бы 4 цифры: ДДММ)
                if (nums.length() >= 4) {
                    try {
                        int month = Integer.parseInt(nums.substring(2, 4));
                        if (month > 12) {
                            // Если больше 12, меняем на "12"
                            nums = nums.substring(0, 2) + "12" + nums.substring(4);
                        } else if (month == 0) {
                            // Аналогично, если 00 - можно менять на 01, но дадим пользователю дописать
                        }
                    } catch (NumberFormatException e) {}
                }

                // 3. Проверка ГОДА (если введено 8 цифр: ДДММГГГГ)
                if (nums.length() >= 8) {
                    try {
                        int year = Integer.parseInt(nums.substring(4, 8));
                        if (year > currentYear) {
                            // Если год из будущего, ставим текущий год
                            nums = nums.substring(0, 4) + currentYear;
                        }
                        // Можно добавить минимальный год, например 1900
                        if (year < 1900 && nums.length() == 8) {
                            // nums = nums.substring(0, 4) + "1900"; // По желанию
                        }
                    } catch (NumberFormatException e) {}
                }

                // === ФОРМАТИРОВАНИЕ (добавляем точки) ===
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < nums.length(); i++) {
                    sb.append(nums.charAt(i));
                    // Ставим точки после 2-го и 4-го символа
                    if ((i == 1 || i == 3) && i < nums.length() - 1) {
                        sb.append('.');
                    }
                }

                // Обрезаем лишнее (макс 10 символов "ДД.ММ.ГГГГ")
                if (sb.length() > 10) {
                    sb.setLength(10);
                }

                etBirthDate.setText(sb.toString());
                etBirthDate.setSelection(sb.length()); // Курсор в конец

                isUpdating = false;
            }
        });

        btnSave.setOnClickListener(v -> saveAndContinue());
    }

    private void saveAndContinue() {
        String nameStr = etName.getText().toString();
        String surnameStr = etSurname.getText().toString();
        String weightStr = etWeight.getText().toString();
        String heightStr = etHeight.getText().toString();
        String birthDate = etBirthDate.getText().toString();



        // Определяем пол
        int selectedId = rgGender.getCheckedRadioButtonId();
        String gender = "Male"; // По умолчанию
        if (selectedId == R.id.rbFemale) gender = "Female";

        if (weightStr.isEmpty() || heightStr.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: 12.01.2026 добавить добавление большего количевства данных
        // Готовим данные для обновления
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName",nameStr);
        updates.put("secondName",surnameStr);
        updates.put("weight", Double.parseDouble(weightStr));
        updates.put("height", Integer.parseInt(heightStr));
        updates.put("birthDate", birthDate);
        updates.put("gender", gender);
        // Можно сразу выставить дефолтные цели
        updates.put("maxXp", 1000);
        updates.put("totalXP", 0);

        // Обновляем профиль в базе
        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Все готово, идем на главный экран
                startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                finishAffinity();
            } else {
                Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}