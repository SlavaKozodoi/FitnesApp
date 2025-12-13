package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.TrainingStat;
import com.example.fitnesapp.utils.ChartHelper; // Ваш хелпер
import com.github.mikephil.charting.charts.LineChart;

import java.util.List;

public class TrainingAdapter extends RecyclerView.Adapter<TrainingAdapter.StatViewHolder> {

    private List<TrainingStat> statsList;
    private Context context;

    public TrainingAdapter(Context context, List<TrainingStat> statsList) {
        this.context = context;
        this.statsList = statsList;
    }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_training_card, parent, false);
        return new StatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        TrainingStat stat = statsList.get(position);
        holder.tvTitle.setText(stat.getTitle());
        holder.tvValue.setText(stat.getValue());
        holder.ivIcon.setImageResource(stat.getIconResId());

        // Покраска фона иконки в начальный цвет градиента
        int startColor = ContextCompat.getColor(context, stat.getStartColorResId());
        holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(startColor));

        // Вызов хелпера с двумя цветами
        ChartHelper.setupUnifiedChart(
                context,
                holder.chart,
                stat.getChartData(),
                stat.getLabelData(),
                stat.getStartColorResId(), // Начальный цвет
                stat.getEndColorResId(),   // Конечный цвет
                false
        );
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    public static class StatViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvValue;
        ImageView ivIcon;
        LineChart chart;

        public StatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvValue = itemView.findViewById(R.id.tvValue);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            chart = itemView.findViewById(R.id.chartStat);
        }
    }
}