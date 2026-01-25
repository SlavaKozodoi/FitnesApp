package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.Achievement;

import java.util.List;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ViewHolder> {

    private final Context context;
    private final List<Achievement> items;
    private final OnAchievementClickListener listener;

    public interface OnAchievementClickListener {
        void onCollectClick(Achievement achievement);
        void onItemClick(Achievement achievement);
    }

    public AchievementsAdapter(Context context, List<Achievement> items, OnAchievementClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement item = items.get(position);

        holder.title.setText(item.title);
        holder.tier.setText(item.tier);
        holder.prize.setText("+" + item.xpReward + "xp");

        // --- ЛОГИКА ЗАБЛОКИРОВАННОГО ЗАДАНИЯ ---
        if (item.isLocked) {
            // Стиль "Недоступно"
            holder.itemView.setAlpha(0.5f);

            // Если есть иконка замка - ставим, если нет - ставим generic
            // Убедитесь, что у вас есть R.drawable.ic_lock
            holder.icon.setImageResource(R.drawable.ic_lock);

            // ВМЕСТО ПРОГРЕССА ПИШЕМ ТРЕБОВАНИЕ
            holder.goal.setText("Req: " + item.requiredTitle);
            holder.goal.setTextColor(Color.parseColor("#FF5252")); // Красный цвет для важности

            // Скрываем лишнее
            holder.progressBar.setVisibility(View.GONE);
            holder.btnCollect.setVisibility(View.GONE);
            holder.date.setVisibility(View.GONE);

            // Клик по замку
            holder.itemView.setOnClickListener(v -> {
                String msg = "Complete '" + item.requiredTitle + "' first!";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            });
            // Отключаем кнопку сбора
            holder.btnCollect.setOnClickListener(null);

            return; // Выходим
        }

        // --- СБРОС СТИЛЕЙ ДЛЯ ОБЫЧНЫХ ЗАДАНИЙ ---
        holder.itemView.setAlpha(1.0f);
        // Возвращаем стандартный цвет текста (замените на свой цвет, если нужно, например @color/text)
        holder.goal.setTextColor(Color.WHITE); // Или Color.parseColor("#B0BEC5")
        holder.goal.setText(item.currentProgress + "/" + item.target);

        int resId = item.getIconResId(context);
        if (resId != 0) {
            holder.icon.setImageResource(resId);
        } else {
            holder.icon.setImageResource(R.drawable.ic_achievement);
        }

        // Логика состояний
        if (item.isCollected) {
            holder.date.setVisibility(View.VISIBLE);
            holder.date.setText(item.unlockedDate);
            holder.btnCollect.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.GONE);
            holder.goal.setText("Completed");

        } else if (item.isCompleted) {
            holder.date.setVisibility(View.GONE);
            holder.btnCollect.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.GONE);

        } else {
            holder.date.setVisibility(View.GONE);
            holder.btnCollect.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setMax(item.target);
            holder.progressBar.setProgress(item.currentProgress);
        }

        // Обработчики
        holder.btnCollect.setOnClickListener(v -> {
            if (listener != null) listener.onCollectClick(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, tier, goal, date, prize;
        ProgressBar progressBar;
        Button btnCollect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.achievementIcon);
            title = itemView.findViewById(R.id.achievementTitle);
            tier = itemView.findViewById(R.id.achievementLevel);
            goal = itemView.findViewById(R.id.achievementGoal);
            date = itemView.findViewById(R.id.achievementDate);
            prize = itemView.findViewById(R.id.achievementPrise);
            progressBar = itemView.findViewById(R.id.achievementProgressBar);
            btnCollect = itemView.findViewById(R.id.achievementButton);
        }
    }
}