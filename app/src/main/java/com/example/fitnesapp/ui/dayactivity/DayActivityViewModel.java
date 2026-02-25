package com.example.fitnesapp.ui.dayactivity;

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

public class DayActivityViewModel extends ViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<DailyData> dailyData = new MutableLiveData<>();

    private DatabaseReference userRef;
    private String selectedDateKey;
    private ValueEventListener dataListener;

    public DayActivityViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            loadDataForDate(new Date()); // Сегодня
        }
    }
    public String getSelectedDateKey(){
        return selectedDateKey;
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<DailyData> getDailyData() { return dailyData; }

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

    public void loadDataForDate(Date date) {
        if (userRef == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String newDateKey = sdf.format(date);

        if (newDateKey.equals(selectedDateKey)) return;
        selectedDateKey = newDateKey;

        if (dataListener != null) {
            userRef.child("daily_data").removeEventListener(dataListener);
        }

        dataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Грузим конкретный день
                DataSnapshot daySnapshot = snapshot.child(selectedDateKey);
                if (daySnapshot.exists()) {
                    dailyData.setValue(daySnapshot.getValue(DailyData.class));
                } else {
                    dailyData.setValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Слушаем всю папку daily_data, но берем ребенка внутри (или можно сразу путь строить)
        // Для оптимизации лучше слушать конкретный путь:
        userRef.child("daily_data").addValueEventListener(dataListener);
    }
}