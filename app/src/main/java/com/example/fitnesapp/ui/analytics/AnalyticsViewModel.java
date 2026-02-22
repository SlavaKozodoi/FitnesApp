package com.example.fitnesapp.ui.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesapp.models.InsightItem;
import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserGoals;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.utils.AnalyticsEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalyticsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<InsightItem>> insightsList = new MutableLiveData<>();

    private DatabaseReference dailyRef;
    private DatabaseReference profileRef;
    private DatabaseReference goalsRef;

    private AnalyticsEngine analyticsEngine;

    private DailyData currentDailyData = null;
    private UserProfile currentUserProfile = null;
    private UserGoals currentUserGoals = null;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);

        analyticsEngine = new AnalyticsEngine(application);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);

            dailyRef = userRef.child("daily_data").child(todayDate);
            profileRef = userRef.child("profile");
            goalsRef = userRef.child("goals");

            loadAnalytics();
        }
    }

    private void loadAnalytics() {
        if (profileRef != null) {
            profileRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) currentUserProfile = snapshot.getValue(UserProfile.class);
                    generateAndSetInsights();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        if (dailyRef != null) {
            dailyRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentDailyData = snapshot.exists() ? snapshot.getValue(DailyData.class) : null;
                    generateAndSetInsights();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        if (goalsRef != null) {
            goalsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) currentUserGoals = snapshot.getValue(UserGoals.class);
                    generateAndSetInsights();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void generateAndSetInsights() {
        List<InsightItem> newInsights = analyticsEngine.generate(currentDailyData, currentUserProfile, currentUserGoals);
        insightsList.setValue(newInsights);
    }

    public LiveData<List<InsightItem>> getInsights() {
        return insightsList;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        analyticsEngine.cleanup();
    }
}