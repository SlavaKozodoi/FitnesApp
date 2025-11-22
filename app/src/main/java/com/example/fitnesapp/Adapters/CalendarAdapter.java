package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private List<Integer> days;
    private List<Integer> activeDays;
    private Context context;
    private OnDayClickListener listener; // 1. Переменная для слушателя

    // 2. Интерфейс для обработки кликов
    public interface OnDayClickListener {
        void onDayClick(int day, boolean isActive);
    }

    // 3. Обновленный конструктор: теперь принимаем listener
    public CalendarAdapter(Context context, List<Integer> days, List<Integer> activeDays, OnDayClickListener listener) {
        this.context = context;
        this.days = days;
        this.activeDays = activeDays;
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

        boolean isActive = activeDays.contains(day);

        // Проверяем, активный ли день (стилизация)
        if (isActive) {
            holder.tvDayNumber.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_color)));
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvDayNumber.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background)));
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        // 4. Обработка нажатия на элемент
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Передаем номер дня и флаг активности в фрагмент
                listener.onDayClick(day, isActive);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void updateData(List<Integer> newDays, List<Integer> newActiveDays) {
        this.days = newDays;
        this.activeDays = newActiveDays;
        notifyDataSetChanged();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
        }
    }
}