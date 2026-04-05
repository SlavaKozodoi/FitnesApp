package com.example.fitnesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.fitnesapp.databinding.ActivityMainBinding;
import com.example.fitnesapp.models.firebase.HourlyActivityItem;
import com.example.fitnesapp.utils.ActivityBucket;
import com.example.fitnesapp.utils.HealthConnectManager;
import com.example.fitnesapp.utils.HealthSyncWorker;
import com.example.fitnesapp.utils.MealData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    final int[] currentDestinationId = {0};

    private HealthConnectManager healthManager;
    private ActivityResultLauncher<Set<String>> requestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- HEALTH CONNECT SETUP ---
        healthManager = new HealthConnectManager(this);

        requestPermissions = registerForActivityResult(
                healthManager.getPermissionContract(),
                granted -> {
                    if (!granted.isEmpty()) {
                        syncHealthData();
                    } else {
                        Log.d("HEALTH", "Permissions denied");
                        Toast.makeText(this, getString(R.string.main_health_permissions_required), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (healthManager.isAvailable()) {
            requestPermissions.launch(healthManager.getPermissions());
        }
        // ---------------------------

        setSupportActionBar(binding.toolbar);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        ImageButton btnSettings = findViewById(R.id.ibSettings);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.achievementFragment, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            currentDestinationId[0] = destination.getId();

            if (destination.getId() == R.id.navigation_home) {
                btnSettings.setImageResource(R.drawable.ic_settings);
                btnSettings.setVisibility(View.VISIBLE);
            }
            else if (destination.getId() == R.id.weightFragment) {
                btnSettings.setImageResource(R.drawable.ic_add_new_weight);
                btnSettings.setVisibility(View.VISIBLE);
            }
            else if (destination.getId() == R.id.navigation_notifications) {
                btnSettings.setImageResource(R.drawable.ic_achievement);
                btnSettings.setVisibility(View.VISIBLE);
            }
            else if (destination.getId() == R.id.sleepFragment) {
                btnSettings.setImageResource(R.drawable.ic_add_new_sleep);
                btnSettings.setVisibility(View.VISIBLE);
            }
            else {
                btnSettings.setVisibility(View.GONE);
            }
        });

        btnSettings.setOnClickListener(v -> {
            if (currentDestinationId[0] == R.id.navigation_home) {
                Navigation.findNavController(this,R.id.nav_host_fragment_activity_main).navigate(R.id.settingsFragment);
            }
            else if (currentDestinationId[0] == R.id.weightFragment) {
                showAddWeightDialog();
            }
            else if (currentDestinationId[0] == R.id.sleepFragment) {
                showAddSleepDialog();
            }
            else if (currentDestinationId[0] == R.id.navigation_notifications) {
                Navigation.findNavController(this,R.id.nav_host_fragment_activity_main).navigate(R.id.historyAchievementsFragment);
            }
        });

        scheduleDailyHealthSync();
    }

    private void showAddSleepDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sleep, null);

        android.widget.TextView tvBedDate = customView.findViewById(R.id.tvBedDate);
        android.widget.TextView tvBedTime = customView.findViewById(R.id.tvBedTime);
        android.widget.TextView tvWakeDate = customView.findViewById(R.id.tvWakeDate);
        android.widget.TextView tvWakeTime = customView.findViewById(R.id.tvWakeTime);

        View btnCancel = customView.findViewById(R.id.btnCancel);
        View btnSave = customView.findViewById(R.id.btnSave);

        // --- УСТАНАВЛИВАЕМ УМНЫЕ ДЕФОЛТЫ ---
        java.util.Calendar wakeCalendar = java.util.Calendar.getInstance();
        java.util.Calendar bedCalendar = java.util.Calendar.getInstance();
        bedCalendar.add(java.util.Calendar.HOUR_OF_DAY, -8);

        tvBedDate.setText(getFriendlyDateString(bedCalendar.get(java.util.Calendar.YEAR), bedCalendar.get(java.util.Calendar.MONTH), bedCalendar.get(java.util.Calendar.DAY_OF_MONTH)));
        tvBedTime.setText(String.format(java.util.Locale.US, "%02d:%02d", bedCalendar.get(java.util.Calendar.HOUR_OF_DAY), bedCalendar.get(java.util.Calendar.MINUTE)));

        tvWakeDate.setText(getFriendlyDateString(wakeCalendar.get(java.util.Calendar.YEAR), wakeCalendar.get(java.util.Calendar.MONTH), wakeCalendar.get(java.util.Calendar.DAY_OF_MONTH)));
        tvWakeTime.setText(String.format(java.util.Locale.US, "%02d:%02d", wakeCalendar.get(java.util.Calendar.HOUR_OF_DAY), wakeCalendar.get(java.util.Calendar.MINUTE)));

        builder.setView(customView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // --- 1. ВЫБОР ДАТЫ СНА ---
        tvBedDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                bedCalendar.set(java.util.Calendar.YEAR, year);
                bedCalendar.set(java.util.Calendar.MONTH, month);
                bedCalendar.set(java.util.Calendar.DAY_OF_MONTH, day);
                tvBedDate.setText(getFriendlyDateString(year, month, day));
            }, bedCalendar.get(java.util.Calendar.YEAR), bedCalendar.get(java.util.Calendar.MONTH), bedCalendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // --- 2. ВЫБОР ВРЕМЕНИ СНА ---
        tvBedTime.setOnClickListener(v -> {
            new android.app.TimePickerDialog(this, (view, hour, minute) -> {
                bedCalendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
                bedCalendar.set(java.util.Calendar.MINUTE, minute);
                tvBedTime.setText(String.format(java.util.Locale.US, "%02d:%02d", hour, minute));
            }, bedCalendar.get(java.util.Calendar.HOUR_OF_DAY), bedCalendar.get(java.util.Calendar.MINUTE), true).show();
        });

        // --- 3. ВЫБОР ДАТЫ ПРОБУЖДЕНИЯ ---
        tvWakeDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                wakeCalendar.set(java.util.Calendar.YEAR, year);
                wakeCalendar.set(java.util.Calendar.MONTH, month);
                wakeCalendar.set(java.util.Calendar.DAY_OF_MONTH, day);
                tvWakeDate.setText(getFriendlyDateString(year, month, day));
            }, wakeCalendar.get(java.util.Calendar.YEAR), wakeCalendar.get(java.util.Calendar.MONTH), wakeCalendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // --- 4. ВЫБОР ВРЕМЕНИ ПРОБУЖДЕНИЯ ---
        tvWakeTime.setOnClickListener(v -> {
            new android.app.TimePickerDialog(this, (view, hour, minute) -> {
                wakeCalendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
                wakeCalendar.set(java.util.Calendar.MINUTE, minute);
                tvWakeTime.setText(String.format(java.util.Locale.US, "%02d:%02d", hour, minute));
            }, wakeCalendar.get(java.util.Calendar.HOUR_OF_DAY), wakeCalendar.get(java.util.Calendar.MINUTE), true).show();
        });

        // --- КНОПКИ ---
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            long bedTimeMillis = bedCalendar.getTimeInMillis();
            long wakeTimeMillis = wakeCalendar.getTimeInMillis();

            if (wakeTimeMillis <= bedTimeMillis) {
                android.widget.Toast.makeText(this, getString(R.string.main_wake_after_bedtime), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            long durationMinutes = (wakeTimeMillis - bedTimeMillis) / (1000 * 60);

            String bedTimeStr = tvBedTime.getText().toString();
            String wakeTimeStr = tvWakeTime.getText().toString();

            String selectedDateKey = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(wakeCalendar.getTime());
            saveSleepToFirebase(selectedDateKey, (int) durationMinutes, bedTimeStr, wakeTimeStr);

            dialog.dismiss();
        });

        dialog.show();
    }

    private String getFriendlyDateString(int year, int month, int day) {
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);

        if (year == today.get(java.util.Calendar.YEAR) &&
                month == today.get(java.util.Calendar.MONTH) &&
                day == today.get(java.util.Calendar.DAY_OF_MONTH)) {
            return getString(R.string.main_today);
        } else if (year == yesterday.get(java.util.Calendar.YEAR) &&
                month == yesterday.get(java.util.Calendar.MONTH) &&
                day == yesterday.get(java.util.Calendar.DAY_OF_MONTH)) {
            return getString(R.string.main_yesterday);
        } else {
            return String.format(java.util.Locale.US, "%02d/%02d/%d", day, month + 1, year);
        }
    }

    private void saveSleepToFirebase(String dateKey, int durationMinutes, String bedTime, String wakeTime) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            android.widget.Toast.makeText(this, getString(R.string.main_user_not_authorized), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.database.DatabaseReference sleepRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("daily_data").child(dateKey).child("sleep");

        com.example.fitnesapp.models.firebase.DailyData.Sleep newSleep = new com.example.fitnesapp.models.firebase.DailyData.Sleep();
        newSleep.durationMinutes = durationMinutes;
        newSleep.bedTime = bedTime;
        newSleep.wakeTime = wakeTime;
        newSleep.fallingAsleepMin = 0;

        com.example.fitnesapp.models.firebase.DailyData.SleepPhases phases = new com.example.fitnesapp.models.firebase.DailyData.SleepPhases();
        phases.deep = 0;
        phases.rem = 0;
        phases.surface = 0;
        phases.awake = 0;

        newSleep.phases = phases;

        sleepRef.setValue(newSleep)
                .addOnSuccessListener(aVoid -> android.widget.Toast.makeText(this, getString(R.string.main_sleep_data_saved), android.widget.Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> android.widget.Toast.makeText(this, getString(R.string.main_error_saving_data), android.widget.Toast.LENGTH_SHORT).show());
    }

    private void scheduleDailyHealthSync() {
        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        dueDate.set(Calendar.HOUR_OF_DAY, 23);
        dueDate.set(Calendar.MINUTE, 30);
        dueDate.set(Calendar.SECOND, 0);

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24);
        }

        long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                HealthSyncWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyHealthSyncTask",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    public void syncHealthData() {
        Futures.addCallback(healthManager.readStepsForToday(), new FutureCallback<Long>() {
            @Override
            public void onSuccess(Long result) { updateFirebaseNode("steps", result.intValue()); }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Steps fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readCaloriesForToday(), new FutureCallback<Double>() {
            @Override
            public void onSuccess(Double result) { updateFirebaseNode("caloriesBurned", result.intValue()); }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Calories fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readHeartRateForToday(), new FutureCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                if (result > 0) updateFirebaseNode("vitals_summary/pulse_avg", result.intValue());
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "HR fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readSleepForToday(), new FutureCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                if (result > 0) updateFirebaseNode("sleep/durationMinutes", result.intValue());
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Sleep fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readWeightForToday(), new FutureCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                if (result > 0) updateFirebaseNode("vitals_summary/weight_today", result);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Weight fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readOxygenForToday(), new FutureCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                if (result > 0) updateFirebaseNode("vitals_summary/spo2_avg", result.doubleValue());
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "SPO2 fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readHistoryForToday(), new FutureCallback<List<ActivityBucket>>() {
            @Override
            public void onSuccess(List<ActivityBucket> buckets) {
                saveHourlyDataToFirebase(buckets);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Detailed sync failed", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readMealsForToday(), new FutureCallback<List<MealData>>() {
            @Override
            public void onSuccess(List<MealData> meals) {
                Log.d("HEALTH", "Meals found: " + meals.size());
                saveNutritionToFirebase(meals);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Nutrition sync failed", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readHeartRateHistory(), new FutureCallback<List<com.example.fitnesapp.utils.HeartRateData>>() {
            @Override
            public void onSuccess(List<com.example.fitnesapp.utils.HeartRateData> data) {
                savePulseToFirebase(data);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Pulse sync failed", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readOxygenHistory(), new FutureCallback<List<com.example.fitnesapp.utils.OxygenData>>() {
            @Override
            public void onSuccess(List<com.example.fitnesapp.utils.OxygenData> data) {
                saveOxygenToFirebase(data);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Oxygen sync failed", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readSleepSessions(), new FutureCallback<List<com.example.fitnesapp.utils.SleepSessionData>>() {
            @Override
            public void onSuccess(List<com.example.fitnesapp.utils.SleepSessionData> data) {
                saveSleepSessionsToFirebase(data);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Sleep sessions fail", t); }
        }, Executors.newSingleThreadExecutor());

        Futures.addCallback(healthManager.readWorkoutSessions(), new FutureCallback<List<com.example.fitnesapp.utils.WorkoutSessionData>>() {
            @Override
            public void onSuccess(List<com.example.fitnesapp.utils.WorkoutSessionData> data) {
                saveWorkoutsToFirebase(data);
            }
            @Override
            public void onFailure(Throwable t) { Log.e("HEALTH", "Workouts sync fail", t); }
        }, Executors.newSingleThreadExecutor());
    }

    private void saveNutritionToFirebase(List<com.example.fitnesapp.utils.MealData> meals) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        String todayDate = java.time.LocalDate.now().toString();
        DatabaseReference dayRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("daily_data").child(todayDate);

        double totalCals = 0;
        double totalCarbs = 0;
        double totalProtein = 0;
        double totalFat = 0;

        DatabaseReference mealsRef = dayRef.child("meals");
        mealsRef.removeValue();

        for (com.example.fitnesapp.utils.MealData meal : meals) {
            totalCals += meal.getCalories();
            totalCarbs += meal.getCarbs();
            totalProtein += meal.getProtein();
            totalFat += meal.getFat();

            String key = mealsRef.push().getKey();

            Map<String, Object> mealMap = new HashMap<>();
            mealMap.put("time", convertTime(meal.getTime()));
            mealMap.put("name", meal.getName());
            mealMap.put("calories", (int) meal.getCalories());
            mealMap.put("carbs", (int) meal.getCarbs());
            mealMap.put("protein", (int) meal.getProtein());
            mealMap.put("fat", (int) meal.getFat());

            if (key != null) {
                mealsRef.child(key).setValue(mealMap);
            }
        }

        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("totalCalories", (int) totalCals);
        summaryMap.put("carbs", (int) totalCarbs);
        summaryMap.put("protein", (int) totalProtein);
        summaryMap.put("fat", (int) totalFat);

        dayRef.child("nutrition").updateChildren(summaryMap);
    }

    private void saveHourlyDataToFirebase(List<ActivityBucket> buckets) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        String todayDate = java.time.LocalDate.now().toString();
        DatabaseReference hourlyRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("daily_data").child(todayDate).child("hourly_activity");

        hourlyRef.removeValue();

        for (ActivityBucket bucket : buckets) {
            if (bucket.getSteps() > 0 || bucket.getCalories() > 0) {
                String key = String.valueOf(bucket.getStartTime());
                HourlyActivityItem item = new HourlyActivityItem();
                item.time = bucket.getStartTime();
                item.steps = (int) bucket.getSteps();
                item.calories = (int) bucket.getCalories();
                hourlyRef.child(key).setValue(item);
            }
        }
    }

    private void updateFirebaseNode(String relativePath, Object value) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        String todayDate = java.time.LocalDate.now().toString();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("daily_data").child(todayDate);
        ref.child(relativePath).setValue(value);
    }

    private String convertTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    private void showAddWeightDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_goal, null);

        android.widget.TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        android.widget.EditText etInput = customView.findViewById(R.id.etGoalInput);
        View btnCancel = customView.findViewById(R.id.btnCancel);
        View btnSave = customView.findViewById(R.id.btnSave);

        tvTitle.setText(getString(R.string.main_add_weight_title));

        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etInput.setHint("0.0");

        builder.setView(customView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newValueStr = etInput.getText().toString();
            newValueStr = newValueStr.replace(",", ".");

            if (!newValueStr.isEmpty()) {
                try {
                    double newWeight = Double.parseDouble(newValueStr);
                    saveWeightToFirebase(newWeight);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, getString(R.string.main_invalid_number_format), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void saveWeightToFirebase(double weight) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        DatabaseReference weightRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("health_logs").child("weight_history");

        String key = weightRef.push().getKey();

        if (key != null) {
            long timestamp = System.currentTimeMillis();
            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());

            java.util.Map<String, Object> weightMap = new java.util.HashMap<>();
            weightMap.put("val", weight);
            weightMap.put("timestamp", timestamp);
            weightMap.put("date", dateStr);

            weightRef.child(key).setValue(weightMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, getString(R.string.main_weight_added), Toast.LENGTH_SHORT).show();

                        FirebaseDatabase.getInstance().getReference().child("users").child(uid)
                                .child("profile").child("totalWeightLogs").setValue(com.google.firebase.database.ServerValue.increment(1));
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(MainActivity.this, getString(R.string.main_error_adding_weight), Toast.LENGTH_SHORT).show());
        }
    }

    private void savePulseToFirebase(List<com.example.fitnesapp.utils.HeartRateData> pulseList) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || pulseList.isEmpty()) return;

        DatabaseReference pulseRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("health_logs").child("pulse");

        Map<String, Object> updates = new HashMap<>();
        for (com.example.fitnesapp.utils.HeartRateData item : pulseList) {
            String key = String.valueOf(item.getTime());
            Map<String, Object> map = new HashMap<>();
            map.put("time", item.getTime());
            map.put("val", item.getBpm());
            updates.put(key, map);
        }

        pulseRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            FirebaseDatabase.getInstance().getReference().child("users").child(uid)
                    .child("profile").child("totalPulseLogs").setValue(com.google.firebase.database.ServerValue.increment(pulseList.size()));
        });
    }

    private void saveOxygenToFirebase(List<com.example.fitnesapp.utils.OxygenData> oxygenList) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || oxygenList.isEmpty()) return;

        DatabaseReference oxyRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("health_logs").child("oxygen");

        Map<String, Object> updates = new HashMap<>();
        for (com.example.fitnesapp.utils.OxygenData item : oxygenList) {
            String key = String.valueOf(item.getTime());
            Map<String, Object> map = new HashMap<>();
            map.put("time", item.getTime());
            map.put("val", item.getPercentage());
            updates.put(key, map);
        }

        oxyRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            FirebaseDatabase.getInstance().getReference().child("users").child(uid)
                    .child("profile").child("totalOxygenLogs").setValue(com.google.firebase.database.ServerValue.increment(oxygenList.size()));
        });
    }

    private void saveSleepSessionsToFirebase(List<com.example.fitnesapp.utils.SleepSessionData> sessions) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || sessions.isEmpty()) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());

        Map<String, Object> logsUpdates = new HashMap<>();

        long minStartTime = Long.MAX_VALUE;
        long maxEndTime = Long.MIN_VALUE;
        long totalDeep = 0, totalLight = 0, totalRem = 0, totalAwake = 0;

        for (com.example.fitnesapp.utils.SleepSessionData item : sessions) {
            String key = String.valueOf(item.getStartTime());
            Map<String, Object> map = new HashMap<>();
            map.put("startTime", item.getStartTime());
            map.put("endTime", item.getEndTime());
            map.put("duration", item.getDurationMinutes());
            map.put("stage", 2);
            logsUpdates.put(key, map);

            if (item.getStartTime() < minStartTime) minStartTime = item.getStartTime();
            if (item.getEndTime() > maxEndTime) maxEndTime = item.getEndTime();

            totalDeep += item.getDeepSleepMin();
            totalLight += item.getLightSleepMin();
            totalRem += item.getRemSleepMin();
            totalAwake += item.getAwakeMin();
        }

        userRef.child("health_logs").child("sleep").updateChildren(logsUpdates);

        long totalDuration = 0;
        if (minStartTime != Long.MAX_VALUE && maxEndTime != Long.MIN_VALUE) {
            totalDuration = (maxEndTime - minStartTime) / (1000 * 60);
        }

        Map<String, Object> dailySleepMap = new HashMap<>();
        java.text.SimpleDateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String bedTimeStr = (minStartTime != Long.MAX_VALUE) ? timeFmt.format(new java.util.Date(minStartTime)) : "--:--";
        String wakeTimeStr = (maxEndTime != Long.MIN_VALUE) ? timeFmt.format(new java.util.Date(maxEndTime)) : "--:--";

        dailySleepMap.put("durationMinutes", totalDuration);
        dailySleepMap.put("bedTime", bedTimeStr);
        dailySleepMap.put("wakeTime", wakeTimeStr);
        dailySleepMap.put("fallingAsleepMin", 0);

        int score = 60;
        if (totalDuration >= 420 && totalDuration <= 540) score = 100;
        else if (totalDuration > 540) score = 90;
        else if (totalDuration >= 360) score = 80;
        dailySleepMap.put("score", score);

        String quality = "Fair";
        if (totalDuration >= 420) quality = "Excellent";
        else if (totalDuration >= 360) quality = "Good";
        dailySleepMap.put("quality", quality);

        long totalPhases = totalDeep + totalLight + totalRem + totalAwake;
        if (totalPhases == 0) totalPhases = 1;

        Map<String, Object> phasesMap = new HashMap<>();
        phasesMap.put("deep", (int) ((totalDeep * 100) / totalPhases));
        phasesMap.put("surface", (int) ((totalLight * 100) / totalPhases));
        phasesMap.put("rem", (int) ((totalRem * 100) / totalPhases));
        phasesMap.put("awake", (int) ((totalAwake * 100) / totalPhases));

        dailySleepMap.put("phases", phasesMap);

        userRef.child("daily_data").child(todayDate).child("sleep").updateChildren(dailySleepMap);
    }

    private void saveWorkoutsToFirebase(List<com.example.fitnesapp.utils.WorkoutSessionData> workouts) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || workouts.isEmpty()) return;

        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        DatabaseReference workoutsRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("daily_data").child(todayDate).child("workouts");

        workoutsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                long alreadySavedCount = snapshot.getChildrenCount();
                long newCount = workouts.size();

                Map<String, Object> updates = new HashMap<>();
                for (com.example.fitnesapp.utils.WorkoutSessionData item : workouts) {
                    String key = String.valueOf(item.getStartTime());
                    Map<String, Object> map = new HashMap<>();

                    map.put("type", item.getType());
                    map.put("calories", item.getCalories());
                    map.put("durationSeconds", item.getDurationMinutes());
                    map.put("timestamp", item.getStartTime());
                    map.put("distance", item.getDistanceKm());

                    Map<String, Double> speedMap = new HashMap<>();
                    if (item.getSpeedData() != null) {
                        for (com.example.fitnesapp.utils.SpeedPoint sp : item.getSpeedData()) {
                            speedMap.put(String.valueOf(sp.getTime()), sp.getSpeedKmh());
                        }
                    }
                    map.put("speed_data", speedMap);

                    updates.put(key, map);
                }

                workoutsRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    if (newCount > alreadySavedCount) {
                        long difference = newCount - alreadySavedCount;

                        FirebaseDatabase.getInstance().getReference().child("users").child(uid)
                                .child("profile").child("totalWorkouts")
                                .setValue(com.google.firebase.database.ServerValue.increment(difference));
                    }
                });
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }
}