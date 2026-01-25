package com.example.fitnesapp.ui.historyachievements;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.Achievement;
import com.example.fitnesapp.models.firebase.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAchievementsViewModel extends ViewModel {

    private DatabaseReference dbRef;
    private String uid;

    private final MutableLiveData<List<Achievement>> collectedList = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();

    private List<Achievement> catalog = new ArrayList<>();
    private Map<String, String> userCollectionMap = new HashMap<>(); // ID -> Date

    public HistoryAchievementsViewModel() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadCatalog();
            loadUserData();
        }
    }

    public LiveData<List<Achievement>> getCollectedList() { return collectedList; }
    public LiveData<UserProfile> getUserProfile() { return userProfile; }

    private void loadCatalog() {
        dbRef.child("all_achievements").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                catalog.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Achievement a = child.getValue(Achievement.class);
                    if (a != null) {
                        a.id = child.getKey();
                        catalog.add(a);
                    }
                }
                matchHistory();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserData() {
        // Профиль
        dbRef.child("users").child(uid).child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userProfile.setValue(snapshot.getValue(UserProfile.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Коллекция
        dbRef.child("users").child(uid).child("achievements_collection").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userCollectionMap.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String date = child.child("unlockedDate").getValue(String.class);
                    userCollectionMap.put(child.getKey(), date);
                }
                matchHistory();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void matchHistory() {
        if (catalog.isEmpty() || userCollectionMap.isEmpty()) return;

        List<Achievement> history = new ArrayList<>();

        for (Achievement template : catalog) {
            if (userCollectionMap.containsKey(template.id)) {
                // Если ачивка есть в коллекции пользователя
                Achievement collected = template; // Тут можно не копировать, мы их не меняем особо, но лучше копировать
                collected.isCollected = true;
                collected.isCompleted = true;
                collected.unlockedDate = userCollectionMap.get(template.id);

                history.add(collected);
            }
        }
        collectedList.setValue(history);
    }
}