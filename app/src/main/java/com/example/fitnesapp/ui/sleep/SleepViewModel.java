package com.example.fitnesapp.ui.sleep;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.utils.analytics.AnalyticsEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SleepViewModel extends AndroidViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData.Sleep> sleepData = new MutableLiveData<>();

    private DatabaseReference userRef;

    private DatabaseReference currentSleepRef;
    private ValueEventListener sleepListener;
    private String selectedDateKey;

    // CHANGE: Added an instance of AnalyticsEngine
    private final AnalyticsEngine analyticsEngine;

    public SleepViewModel(@NonNull Application application) {
        super(application);

        // CHANGE: Initialize the AnalyticsEngine with the application context
        analyticsEngine = new AnalyticsEngine(application);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            loadSleepData(new Date());
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData.Sleep> getSleepData() { return sleepData; }

    private void loadProfile() {
        if (userRef == null) return;
        userRef.child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userProfile.setValue(snapshot.getValue(UserProfile.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void loadSleepData(Date date) {
        if (userRef == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String newDateKey = sdf.format(date);

        if (newDateKey.equals(selectedDateKey)) return;
        selectedDateKey = newDateKey;

        if (currentSleepRef != null && sleepListener != null) {
            currentSleepRef.removeEventListener(sleepListener);
        }

        currentSleepRef = userRef.child("daily_data").child(selectedDateKey).child("sleep");

        sleepListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DailyData.Sleep currentSleep = snapshot.getValue(DailyData.Sleep.class);
                    if (currentSleep != null) {

                        // CHANGE: Call evaluateSleep on the instance, not the class
                        analyticsEngine.evaluateSleep(currentSleep);

                        sleepData.setValue(currentSleep);
                    }
                } else {
                    sleepData.setValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        currentSleepRef.addValueEventListener(sleepListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentSleepRef != null && sleepListener != null) {
            currentSleepRef.removeEventListener(sleepListener);
        }
    }
}