package com.example.fitnesapp;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class FitnesApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Она включает сохранение данных на диск (Disk Persistence)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
