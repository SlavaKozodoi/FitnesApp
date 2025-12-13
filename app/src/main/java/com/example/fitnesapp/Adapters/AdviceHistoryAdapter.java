package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.Edvice;

import java.util.ArrayList;
import java.util.List;

public class AdviceHistoryAdapter extends RecyclerView.Adapter<AdviceHistoryAdapter.AdviceViewHolder> {

    // Временный список данных для заглушки
    private List<Edvice> dummyList;
    private Context context;

    public AdviceHistoryAdapter(Context context , List<Edvice> dummyList) {
        this.context = context;
        this.dummyList = dummyList;

    }

    @NonNull
    @Override
    public AdviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Убедитесь, что создали файл item_advice_history.xml (код ниже)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_advice_history, parent, false);
        return new AdviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdviceViewHolder holder, int position) {
        Edvice adviceText = dummyList.get(position);

        holder.tvTitle.setText(adviceText.getTitle());
        holder.imageView.setImageResource(adviceText.getImageEdvice());
        holder.tvDate.setText(adviceText.getTime()); // Фиктивная дата

        // Обработка нажатия (для теста)
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Нажат элемент: " + position, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return dummyList.size();
    }

    public class AdviceViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView tvTitle,  tvDate;

        public AdviceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Привязываем Views из item_advice_history.xml
            tvTitle = itemView.findViewById(R.id.tvTitleAdvice);
            imageView = itemView.findViewById(R.id.imageViewAdvice);
            tvDate = itemView.findViewById(R.id.tvTimeAdvice);
        }
    }
}