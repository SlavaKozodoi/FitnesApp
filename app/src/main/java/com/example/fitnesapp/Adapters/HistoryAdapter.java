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

        holder.title.setText(item.title);
        holder.tier.setText(item.tier);
        holder.goal.setText(item.description);
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

        holder.goal.setVisibility(View.VISIBLE); // Скрываем цель (2000/2000), оставляем только дату
        // Или можно написать holder.goal.setText("Completed");

        holder.progressBar.setVisibility(View.GONE);
        holder.btnCollect.setVisibility(View.GONE);
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