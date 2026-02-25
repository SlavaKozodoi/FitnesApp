package com.example.fitnesapp; // Замените на ваш пакет

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesapp.ui.auth.AuthActivity;
import com.example.fitnesapp.ui.auth.RegisterActivity; // Ваш экран входа
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Подключаем верстку с индикатором загрузки
        setContentView(R.layout.activity_splash);

        // Проверяем, есть ли пользователь в системе прямо сейчас
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Небольшая задержка (например, 1 секунда), чтобы пользователь
        // успел увидеть ваш красивый логотип и анимацию загрузки.
        // Если убрать задержку, на мощных телефонах экран просто моргнет черным.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (currentUser != null) {
                // ПОЛЬЗОВАТЕЛЬ В СИСТЕМЕ -> Идем сразу на главный экран (MainActivity)
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                // ПОЛЬЗОВАТЕЛЯ НЕТ -> Идем на экран регистрации/входа
                Intent intent = new Intent(SplashActivity.this, AuthActivity.class);
                startActivity(intent);
            }

            // ВАЖНО: Закрываем SplashActivity, чтобы при нажатии кнопки "Назад"
            // пользователь не вернулся на экран загрузки
            finish();

        }, 1000); // 1000 миллисекунд = 1 секунда
    }
}