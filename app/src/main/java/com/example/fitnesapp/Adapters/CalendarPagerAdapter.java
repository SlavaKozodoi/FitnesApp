package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.CalendarDate;

import java.util.List;

public class CalendarPagerAdapter extends RecyclerView.Adapter<CalendarPagerAdapter.WeekViewHolder> {

    private List<List<CalendarDate>> weeks; // Список недель
    private Context context;
    private OnDateClickListener listener;

    // Храним выбранную дату глобально, чтобы обновлять перерисовку
    private CalendarDate selectedDateGlobal = null;

    public interface OnDateClickListener {
        void onDateClick(CalendarDate date);
    }

    public CalendarPagerAdapter(Context context, List<List<CalendarDate>> weeks, OnDateClickListener listener) {
        this.context = context;
        this.weeks = weeks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Берем контейнер для недели
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_week, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        List<CalendarDate> weekDays = weeks.get(position);
        holder.bind(weekDays);
    }

    @Override
    public int getItemCount() {
        return weeks.size();
    }

    public void setSelectedDate(CalendarDate date) {
        this.selectedDateGlobal = date;
        notifyDataSetChanged(); // Перерисовываем, чтобы кружок появился
    }

    public class WeekViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;

        public WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.weekContainer);
        }

        public void bind(List<CalendarDate> days) {
            container.removeAllViews(); // Очищаем старые views перед отрисовкой

            // Программно создаем 7 дней
            for (CalendarDate day : days) {
                // Инфлейтим (надуваем) дизайн одного дня из прошлого урока
                View dayView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, container, false);

                // Настраиваем параметры (вес 1, чтобы растянулись равномерно)
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                dayView.setLayoutParams(params);

                // Находим элементы внутри dayView
                TextView tvDay = dayView.findViewById(R.id.tvDayNumber);
                View bg = dayView.findViewById(R.id.viewBackground);

                tvDay.setText(day.getDayNumber());
                // Внутри bind() цикла for

                boolean isSelected = false;
                if (selectedDateGlobal != null) {
                    // Сравниваем просто по дням (через стандартный Android DateFormat)
                    String currentDayStr = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(day.getDate());
                    String selectedDayStr = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(selectedDateGlobal.getDate());

                    if (currentDayStr.equals(selectedDayStr)) {
                        isSelected = true;
                    }
                }

                bg.setSelected(isSelected);
                tvDay.setTextColor(isSelected ? Color.BLACK : Color.WHITE);

                // Клик
                dayView.setOnClickListener(v -> {
                    selectedDateGlobal = day; // Запоминаем новую выбранную дату
                    notifyDataSetChanged();   // Обновляем ВСЕ недели (чтобы снять выделение с другой недели)
                    listener.onDateClick(day);
                });

                container.addView(dayView);
            }
        }
    }
}