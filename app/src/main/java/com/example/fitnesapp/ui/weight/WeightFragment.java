package com.example.fitnesapp.ui.weight;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fitnesapp.ui.base.BaseLoadingFragment;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentWeightBinding;
import com.example.fitnesapp.models.firebase.WeightHistoryItem;
import com.example.fitnesapp.utils.ChartHelper;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeightFragment extends BaseLoadingFragment {

    private WeightViewModel mViewModel;
    private FragmentWeightBinding binding;

    private List<WeightHistoryItem> allWeightHistory = new ArrayList<>();
    private List<WeightHistoryItem> currentChartData = new ArrayList<>();

    private double userHeightMeters = 0;

    private Snackbar actionSnackbar;
    // Змінна для таймера затримки появи
    private Runnable pendingSnackbarRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWeightBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(WeightViewModel.class);

        binding.button.setText(getString(R.string.weight_btn_format, 1, getString(R.string.weight_month)));
        binding.button2.setText(getString(R.string.weight_btn_format, 3, getString(R.string.weight_month)));
        binding.button3.setText(getString(R.string.weight_btn_format, 6, getString(R.string.weight_month)));

        setupTimeFilters();
        setupChartListener();

        mViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.tvName.setText(profile.firstName);
                binding.tvSecondName.setText(profile.secondName);
                if (profile.height > 0) {
                    userHeightMeters = profile.height / 100.0;
                    recalculateBMI();
                }
            }
        });

        mViewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null) {
                binding.tvWeightScore.setText(String.format(Locale.US, "%.1f", weight));
                recalculateBMI();
            }
        });

        mViewModel.getWeightHistory().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                allWeightHistory = list;
                updateChartForPeriod(1);
                updateButtonVisuals(binding.button);
            }
        });

        startFakeLoading(view, 200);
    }

    // ==========================================
    // ЛОГІКА ЗАТРИМКИ ТА ЗНИКНЕННЯ ПАНЕЛІ
    // ==========================================
    private void cancelPendingSnackbar() {
        if (pendingSnackbarRunnable != null && binding != null && binding.getRoot() != null) {
            binding.getRoot().removeCallbacks(pendingSnackbarRunnable);
            pendingSnackbarRunnable = null;
        }
        if (actionSnackbar != null && actionSnackbar.isShown()) {
            actionSnackbar.dismiss();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Якщо користувач згорнув додаток або перейшов на інший екран - ховаємо панель
        cancelPendingSnackbar();
    }

    // ==========================================
    // РОЗУМНІ КЛІКИ ПО ГРАФІКУ (З ЗАТРИМКОЮ)
    // ==========================================
    private void setupChartListener() {
        binding.chartWeightInfo.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < currentChartData.size()) {
                    WeightHistoryItem selectedItem = currentChartData.get(index);

                    // 1. Скасовуємо попередній таймер (якщо палець ще рухається)
                    cancelPendingSnackbar();

                    // 2. Створюємо нове завдання для показу панелі
                    pendingSnackbarRunnable = () -> {
                        if (binding == null) return; // Захист від крашу

                        String msg = selectedItem.date + "  -  " + String.format(Locale.US, "%.1f", selectedItem.val) + " кг";

                        actionSnackbar = Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.weight_action_edit).toUpperCase(), v -> {
                                    showEditWeightDialog(selectedItem);
                                });

                        actionSnackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.accent_color));
                        actionSnackbar.show();
                    };

                    // 3. Запускаємо таймер на 2 секунди (2000 мс)
                    binding.getRoot().postDelayed(pendingSnackbarRunnable, 1000);
                }
            }

            @Override
            public void onNothingSelected() {
                // Якщо користувач натиснув у порожнє місце - скасовуємо таймер і ховаємо панель
                cancelPendingSnackbar();
            }
        });
    }

    private void showEditWeightDialog(WeightHistoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goal, null);

        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        EditText etInput = customView.findViewById(R.id.etGoalInput);
        View btnCancel = customView.findViewById(R.id.btnCancel);
        View btnSave = customView.findViewById(R.id.btnSave);

        tvTitle.setText(getString(R.string.weight_edit_title) + "\n" + getString(R.string.weight_edit_delete));
        tvTitle.setTextSize(16f);
        tvTitle.setGravity(Gravity.CENTER);

        etInput.setText(String.valueOf(item.val));

        builder.setView(customView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String valStr = etInput.getText().toString().trim().replace(",", ".");

            if (valStr.isEmpty()) {
                dialog.dismiss();
                confirmDelete(item);
            } else {
                try {
                    double newWeight = Double.parseDouble(valStr);
                    updateWeightInFirebase(item, newWeight);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), getString(R.string.main_invalid_number_format), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void confirmDelete(WeightHistoryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.weight_delete_title))
                .setMessage(getString(R.string.weight_delete_msg, item.val, item.date))
                .setPositiveButton(getString(R.string.weight_delete_yes), (dialog, which) -> {
                    deleteWeightFromFirebase(item);
                })
                .setNegativeButton(getString(R.string.weight_cancel), null)
                .show();
    }

    // ==========================================
    // РОБОТА З FIREBASE
    // ==========================================
    private void updateWeightInFirebase(WeightHistoryItem item, double newWeight) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("health_logs").child("weight_history");

        ref.orderByChild("timestamp").equalTo(item.timestamp).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    child.getRef().child("val").setValue(newWeight);
                }
                Toast.makeText(getContext(), getString(R.string.weight_toast_updated), Toast.LENGTH_SHORT).show();
                cancelPendingSnackbar();
                binding.chartWeightInfo.highlightValue(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteWeightFromFirebase(WeightHistoryItem item) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("health_logs").child("weight_history");

        ref.orderByChild("timestamp").equalTo(item.timestamp).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    child.getRef().removeValue();
                }
                Toast.makeText(getContext(), getString(R.string.weight_toast_deleted), Toast.LENGTH_SHORT).show();
                cancelPendingSnackbar();
                binding.chartWeightInfo.highlightValue(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================================
    // ЛОГІКА ГРАФІКІВ ТА UI
    // ==========================================
    private void recalculateBMI() {
        try {
            String wStr = binding.tvWeightScore.getText().toString().replace(",", ".");
            double weight = Double.parseDouble(wStr);

            if (weight > 0 && userHeightMeters > 0) {
                double bmi = weight / (userHeightMeters * userHeightMeters);
                binding.tvBMI.setText(String.format(Locale.US, "%.1f", bmi));

                String status = getString(R.string.weight_status_normal);
                if (bmi < 18.5) status = getString(R.string.weight_status_underweight);
                else if (bmi >= 25 && bmi < 30) status = getString(R.string.weight_status_overweight);
                else if (bmi >= 30) status = getString(R.string.weight_status_obese);

                binding.tvWeightStat.setText(status);
            }
        } catch (NumberFormatException e) { }
    }

    private void setupTimeFilters() {
        binding.button.setOnClickListener(v -> {
            updateChartForPeriod(1);
            updateButtonVisuals(binding.button);
        });
        binding.button3.setOnClickListener(v -> {
            updateChartForPeriod(3);
            updateButtonVisuals(binding.button3);
        });
        binding.button2.setOnClickListener(v -> {
            updateChartForPeriod(6);
            updateButtonVisuals(binding.button2);
        });
    }

    private void updateChartForPeriod(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        long cutoffTime = cal.getTimeInMillis();

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labelsList = new ArrayList<>();

        currentChartData.clear();

        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat chartFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        int index = 0;

        for (WeightHistoryItem record : allWeightHistory) {
            try {
                Date dateObj = dbFormat.parse(record.date);

                if (dateObj != null && dateObj.getTime() >= cutoffTime) {
                    float val = (float) record.val;

                    if (val > max) max = val;
                    if (val < min) min = val;

                    entries.add(new Entry(index, val));
                    labelsList.add(chartFormat.format(dateObj));

                    currentChartData.add(record);

                    index++;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (entries.isEmpty()) {
            binding.tvHighestWeight.setText(getString(R.string.format_kg_empty));
            binding.tvLowestWeight.setText(getString(R.string.format_kg_empty));
        } else {
            binding.tvHighestWeight.setText(getString(R.string.format_kg, max));
            binding.tvLowestWeight.setText(getString(R.string.format_kg, min));
        }

        String[] labels = labelsList.toArray(new String[0]);

        ChartHelper.setupUnifiedChart(
                requireContext(),
                binding.chartWeightInfo,
                entries,
                labels,
                R.color.weight_start,
                R.color.weight_end,
                true
        );

        binding.chartWeightInfo.setTouchEnabled(true);
    }

    private void updateButtonVisuals(Button activeButton) {
        resetButtonStyle(binding.button);
        resetButtonStyle(binding.button3);
        resetButtonStyle(binding.button2);
        activeButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_color));
        activeButton.setTextColor(Color.BLACK);

        cancelPendingSnackbar();
        binding.chartWeightInfo.highlightValue(null);
    }

    private void resetButtonStyle(Button btn) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.background));
        btn.setTextColor(Color.WHITE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelPendingSnackbar(); // Обов'язково чистимо при знищенні View
        binding = null;
    }
}