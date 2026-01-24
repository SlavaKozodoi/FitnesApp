package com.example.fitnesapp.ui.weight;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.models.firebase.UserProfile;
import com.example.fitnesapp.models.firebase.WeightHistoryItem; // Ваша модель
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WeightViewModel extends ViewModel {

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    // Теперь список состоит из WeightHistoryItem
    private final MutableLiveData<List<WeightHistoryItem>> weightHistory = new MutableLiveData<>();
    private final MutableLiveData<Double> currentWeight = new MutableLiveData<>();

    private DatabaseReference userRef;

    public WeightViewModel() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            loadProfile();
            loadWeightHistory();
        }
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<List<WeightHistoryItem>> getWeightHistory() { return weightHistory; }
    public LiveData<Double> getCurrentWeight() { return currentWeight; }

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

    private void loadWeightHistory() {
        if (userRef == null) return;

        // Firebase умеет сортировать строки формата "YYYY-MM-DD" правильно
        userRef.child("health_logs").child("weight_history").orderByChild("date")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<WeightHistoryItem> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Используем вашу модель
                            WeightHistoryItem item = child.getValue(WeightHistoryItem.class);
                            if (item != null) {
                                list.add(item);
                            }
                        }

                        weightHistory.setValue(list);

                        // Берем вес из последней записи как текущий
                        if (!list.isEmpty()) {
                            currentWeight.setValue(list.get(list.size() - 1).val);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}