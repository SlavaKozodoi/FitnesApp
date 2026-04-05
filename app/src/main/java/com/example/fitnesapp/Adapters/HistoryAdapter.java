package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.Achievement;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<Achievement> items;

    public HistoryAdapter(Context context, List<Achievement> items) {
        this.context = context;
        this.items = items;
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

        // ИСПРАВЛЕНИЕ 1: Динамически переводим заголовок
        String translatedTitle = getTranslatedText(item.id, item.title, "_title");
        holder.title.setText(translatedTitle);

        holder.tier.setText(item.tier);

        // ИСПРАВЛЕНИЕ 2: Динамически переводим описание (оно выводится вместо прогресса)
        String translatedDesc = getTranslatedText(item.id, item.description, "_desc");
        holder.goal.setText(translatedDesc);

        int resId = item.getIconResId(context);
        if (resId != 0) {
            holder.icon.setImageResource(resId);
        } else {
            // Картинка по умолчанию, если имя файла не найдено
            holder.icon.setImageResource(R.drawable.ic_achievement);
        }

        holder.prize.setText("+" + item.xpReward + "xp");

        // Для истории всегда показываем дату и скрываем элементы "в процессе"
        holder.date.setVisibility(View.VISIBLE);
        holder.date.setText(item.unlockedDate);

        holder.goal.setVisibility(View.VISIBLE); // Показываем переведенное описание

        holder.progressBar.setVisibility(View.GONE);
        holder.btnCollect.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ПЕРЕВОДОВ
    // ==========================================
    private String getTranslatedText(String achievementId, String fallback, String suffix) {
        if (achievementId == null || achievementId.isEmpty()) {
            return fallback; // Защита от Null
        }

        // Ищем ID строки вида "ach_cal_daily_01_title" или "ach_cal_daily_01_desc"
        int resId = context.getResources().getIdentifier(achievementId + suffix, "string", context.getPackageName());

        if (resId != 0) {
            return context.getString(resId); // Нашли перевод!
        } else {
            return fallback; // Не нашли (берем из базы Firebase)
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