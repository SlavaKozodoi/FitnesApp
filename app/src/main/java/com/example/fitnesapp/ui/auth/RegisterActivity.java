package com.example.fitnesapp.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesapp.MainActivity;
import com.example.fitnesapp.R;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.ui.setupprofile.SetupProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Кнопка регистрации
        btnRegister.setOnClickListener(v -> registerUser());

        // Кнопка "Уже есть аккаунт" -> Возврат на логин
        tvLogin.setOnClickListener(v -> {
            finish(); // Просто закрываем это окно, под ним лежит LoginActivity
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. Простая валидация
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter name");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be > 6 chars");
            return;
        }

        // 2. Создаем пользователя в Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Успех! Теперь создаем профиль в базе данных
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        saveUserToDatabase(firebaseUser.getUid(), name, email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String name, String email) {
        // Создаем объект профиля с начальными данными
        UserProfile userProfile = new UserProfile();
        userProfile.firstName = name;
        userProfile.secondName = ""; // Пока пусто
        userProfile.weight = 0;      // Потом заполнят в настройках
        userProfile.height = 0;
        userProfile.totalXP = 0;
        userProfile.maxXp = 1000;

        mDatabase.child(uid).child("profile").setValue(userProfile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // ИЗМЕНЕНИЕ: Отправляем заполнять данные, а не на главный экран
                        Intent intent = new Intent(RegisterActivity.this, SetupProfileActivity.class);
                        startActivity(intent);
                        finishAffinity();
                    }
                    else {
                        Toast.makeText(RegisterActivity.this, "Не удалось сохранить профиль", Toast.LENGTH_SHORT).show();
                        }
                    });
    }
}