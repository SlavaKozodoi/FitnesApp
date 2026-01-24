package com.example.fitnesapp.ui.sleep;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.models.firebase.DailyData;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SleepViewModel extends ViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData.Sleep> sleepData = new MutableLiveData<>();

    private DatabaseReference userRef;
    private String selectedDateKey;
    private ValueEventListener sleepListener;

    public SleepViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            loadSleepData(new Date()); // Загружаем сегодня
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

        // Удаляем старый слушатель, если был
        if (sleepListener != null) {
            userRef.child("daily_data").removeEventListener(sleepListener); // Удаляем грубо, лучше хранить ref точнее
            // Но проще создать новый путь:
        }

        // Путь к конкретной дате
        DatabaseReference sleepRef = userRef.child("daily_data").child(selectedDateKey).child("sleep");

        sleepListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    sleepData.setValue(snapshot.getValue(DailyData.Sleep.class));
                } else {
                    // Если данных нет, отправляем null, чтобы очистить UI
                    sleepData.setValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        sleepRef.addValueEventListener(sleepListener);
    }
}