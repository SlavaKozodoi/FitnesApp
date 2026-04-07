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
        // ИСПРАВЛЕНИЕ 1: Добавили View clickedView в параметры
        void onCollectClick(View clickedView, Achievement achievement);
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

        String translatedTitle = getTranslatedTitle(item.id, item.title);
        holder.title.setText(translatedTitle);

        holder.tier.setText(item.tier);
        holder.prize.setText("+" + item.xpReward + "xp");

        if (item.isLocked) {
            holder.itemView.setAlpha(0.5f);
            holder.icon.setImageResource(R.drawable.ic_ach_lock);

            String translatedReqTitle = getTranslatedTitle(item.previousId, item.requiredTitle);

            holder.goal.setText(context.getString(R.string.achievements_previous) + ": " + translatedReqTitle);
            holder.goal.setTextColor(Color.parseColor("#FF5252"));

            holder.progressBar.setVisibility(View.GONE);
            holder.btnCollect.setVisibility(View.GONE);
            holder.date.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                String msg = context.getString(R.string.achievements_complete) + " '" +
                        translatedReqTitle + "' " +
                        context.getString(R.string.achievements_first) + "!";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            });
            holder.btnCollect.setOnClickListener(null);

            return;
        }

        holder.itemView.setAlpha(1.0f);
        holder.goal.setTextColor(Color.WHITE);

        int resId = 0;
        if (item.icon != null && !item.icon.isEmpty()) {
            resId = context.getResources().getIdentifier(item.icon, "drawable", context.getPackageName());
        }

        if (resId != 0) {
            holder.icon.setImageResource(resId);
        } else {
            holder.icon.setImageResource(R.drawable.ic_achievement);
        }

        if (item.isCollected) {
            holder.date.setVisibility(View.VISIBLE);
            holder.date.setText(item.unlockedDate);
            holder.btnCollect.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.GONE);

            holder.goal.setText(context.getString(R.string.achievements_completed));

        } else if (item.isCompleted) {
            holder.date.setVisibility(View.GONE);
            holder.btnCollect.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.GONE);
            holder.goal.setText(item.target + "/" + item.target);

        } else {
            holder.date.setVisibility(View.GONE);
            holder.btnCollect.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setMax(item.target);
            holder.progressBar.setProgress(item.currentProgress);
            holder.goal.setText(item.currentProgress + "/" + item.target);
        }

        // --- ОБРАБОТЧИКИ КЛИКОВ ---
        holder.btnCollect.setOnClickListener(v -> {
            // ИСПРАВЛЕНИЕ 2: Передаем v (саму кнопку) в listener
            if (listener != null) listener.onCollectClick(v, item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getTranslatedTitle(String achievementId, String fallbackTitle) {
        if (achievementId == null || achievementId.isEmpty()) {
            return fallbackTitle;
        }

        int titleResId = context.getResources().getIdentifier(achievementId + "_title", "string", context.getPackageName());

        if (titleResId != 0) {
            return context.getString(titleResId);
        } else {
            return fallbackTitle;
        }
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