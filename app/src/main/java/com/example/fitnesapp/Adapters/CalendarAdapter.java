package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.util.List;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private List<Integer> days;
    private List<Integer> activeDays; // Дни с тренировками (фиолетовый фон)
    private Map<Integer, Integer> progressMap; // Прогресс (кольцо)
    private Context context;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(int day, boolean hasWorkout);
    }

    public CalendarAdapter(Context context, List<Integer> days, List<Integer> activeDays, Map<Integer, Integer> progressMap, OnDayClickListener listener) {
        this.context = context;
        this.days = days;
        this.activeDays = activeDays;
        this.progressMap = progressMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int day = days.get(position);
        holder.tvDayNumber.setText(String.valueOf(day));

        boolean hasWorkout = activeDays != null && activeDays.contains(day);
        boolean hasProgress = progressMap != null && progressMap.containsKey(day);

        // --- 1. ФОН (Тренировки) ---
        if (hasWorkout) {
            // Если была тренировка — красим фон (фиолетовый/активный цвет)
            holder.viewBackground.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.calendar_active)));
        } else {
            // Иначе фон прозрачный/стандартный
            holder.viewBackground.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background)));
        }

        // --- 2. КОЛЬЦО ПРОГРЕССА (Шаги/Калории) ---
        if (hasProgress) {
            int progress = progressMap.get(day);
            holder.progressDay.setProgressWithAnimation((float) progress, 1000L);

            if (progress >= 100) {
                holder.progressDay.setProgressBarColor(ContextCompat.getColor(context, R.color.accent_color)); // Зеленый
            } else {
                holder.progressDay.setProgressBarColor(ContextCompat.getColor(context, R.color.appbar_start)); // Синий
            }
        } else {
            holder.progressDay.setProgress(0f);
        }

        // Клик по дню
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(day, hasWorkout);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void updateData(List<Integer> newDays, List<Integer> newActiveDays, Map<Integer, Integer> newProgressMap) {
        this.days = newDays;
        this.activeDays = newActiveDays;
        this.progressMap = newProgressMap;
        notifyDataSetChanged();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        View viewBackground;
        CircularProgressBar progressDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            viewBackground = itemView.findViewById(R.id.viewBackground);
            progressDay = itemView.findViewById(R.id.progressDay);
        }
    }
}