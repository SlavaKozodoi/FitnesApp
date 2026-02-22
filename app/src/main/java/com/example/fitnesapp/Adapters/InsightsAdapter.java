package com.example.fitnesapp.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.InsightItem;

import java.util.List;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.InsightViewHolder> {

    private List<InsightItem> insightsList;

    public InsightsAdapter(List<InsightItem> insightsList) {
        this.insightsList = insightsList;
    }

    // Метод для плавного обновления списка
    public void updateData(List<InsightItem> newInsights) {
        this.insightsList.clear();
        this.insightsList.addAll(newInsights);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_insight, parent, false);
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        InsightItem item = insightsList.get(position);

        holder.tvTitle.setText(item.title);
        holder.tvMessage.setText(item.message);

        // Настраиваем дизайн в зависимости от типа подсказки
        switch (item.type) {
            case WARNING:
                holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert); // Стандартная иконка восклицательного знака
                holder.ivIcon.setColorFilter(Color.parseColor("#FF5252")); // Красный цвет
                holder.iconBackground.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#33FF5252"))); // Полупрозрачный красный фон
                break;

            case TIP:
                holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_info); // Стандартная иконка "i"
                holder.ivIcon.setColorFilter(Color.parseColor("#448AFF")); // Синий цвет
                holder.iconBackground.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#33448AFF")));
                break;

            case PREDICTION:
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_compass); // Стандартная иконка компаса/прогноза
                holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50")); // Зеленый цвет
                holder.iconBackground.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334CAF50")));
                break;

            case PRAISE:
                holder.ivIcon.setImageResource(android.R.drawable.star_on); // Стандартная иконка звезды
                holder.ivIcon.setColorFilter(Color.parseColor("#FFC107")); // Золотой/Желтый цвет
                holder.iconBackground.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFC107")));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return insightsList != null ? insightsList.size() : 0;
    }

    public static class InsightViewHolder extends RecyclerView.ViewHolder {
        CardView cardContainer;
        View iconBackground;
        ImageView ivIcon;
        TextView tvTitle, tvMessage;

        public InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}