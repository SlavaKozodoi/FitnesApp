package com.example.fitnesapp;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager; // Добавить импорт
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.fitnesapp.databinding.ActivityMainBinding;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    final int[] currentDestinationId = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. ВКЛЮЧАЕМ РЕЖИМ "БЕЗ ГРАНИЦ" (Чтобы градиент залез под статус-бар)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. УДАЛЯЕМ старый код с getActionBar() - он больше не нужен и вызовет ошибку

        // 3. Настраиваем наш новый Toolbar из XML как системный
        setSupportActionBar(binding.toolbar);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        ImageButton btnSettings = findViewById(R.id.ibSettings);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.achievementFragment, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // Связываем навигацию с нашим новым тулбаром
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // 2. Слушаем смену экранов ТОЛЬКО для смены иконки и видимости
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

            // Запоминаем, где мы сейчас находимся
            currentDestinationId[0] = destination.getId();

            if (destination.getId() == R.id.navigation_home) {
                // На Главной -> Иконка настройки
                btnSettings.setImageResource(R.drawable.ic_settings);
                btnSettings.setVisibility(View.VISIBLE);
            }
            else if (destination.getId() == R.id.weightFragment) {
                // На Весе -> Иконка плюсика
                btnSettings.setImageResource(R.drawable.ic_add_new_weight);
                btnSettings.setVisibility(View.VISIBLE);
            }
            else if (destination.getId() == R.id.navigation_notifications) {
                btnSettings.setImageResource(R.drawable.ic_achievement);
                btnSettings.setVisibility(View.VISIBLE);

            }
            else {
                // В других местах -> Прячем
                btnSettings.setVisibility(View.GONE);
            }
        });

// 3. Обработка нажатия (Задаем ОДИН РАЗ, снаружи)
        btnSettings.setOnClickListener(v -> {

            // Проверяем сохраненный ID
            if (currentDestinationId[0] == R.id.navigation_home) {
                // Логика для настроек
                Toast.makeText(MainActivity.this, "Settings Clicked", Toast.LENGTH_SHORT).show();
                // Navigation.findNavController(this, R.id.nav_host...).navigate(R.id.action_home_to_settings);
            }
            else if (currentDestinationId[0] == R.id.weightFragment) {
                // Логика для добавления веса
                Toast.makeText(MainActivity.this, "Add Weight Clicked", Toast.LENGTH_SHORT).show();
                // Открыть диалог добавления веса
            }
            else if (currentDestinationId[0] == R.id.navigation_notifications) {
                // Логика для добавления веса
                Toast.makeText(MainActivity.this, "Achievement window Clicked", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(this,R.id.nav_host_fragment_activity_main).navigate(R.id.historyAchievementsFragment);
                // Открыть диалог добавления веса
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        // Этот код заставляет стрелку "Назад" работать
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}