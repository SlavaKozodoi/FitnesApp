package com.example.fitnesapp.Adapters;

import android.content.Context;
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

import java.util.List;

public class AdviceHistoryAdapter extends RecyclerView.Adapter<AdviceHistoryAdapter.AdviceViewHolder> {

    // Теперь это реальный список советов от ИИ-тренера, а не просто заглушка
    private final List<Edvice> adviceList;
    private final Context context;

    public AdviceHistoryAdapter(Context context, List<Edvice> adviceList) {
        this.context = context;
        this.adviceList = adviceList;
    }

    @NonNull
    @Override
    public AdviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_advice_history, parent, false);
        return new AdviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdviceViewHolder holder, int position) {
        Edvice currentAdvice = adviceList.get(position);

        // Устанавливаем иконку, заголовок и время
        holder.tvTitle.setText(currentAdvice.getTitle());
        holder.imageView.setImageResource(currentAdvice.getImageEdvice());
        holder.tvDate.setText(currentAdvice.getTime());

        // НОВОЕ: Устанавливаем подробное описание совета
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(currentAdvice.getDescription());
        }

        // Обработка нажатия (пока оставим Toast, но в будущем можно открывать детали)
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, currentAdvice.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return adviceList.size();
    }

    public static class AdviceViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView tvTitle, tvDate, tvDescription; // Добавили tvDescription

        public AdviceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Привязываем Views из item_advice_history.xml
            tvTitle = itemView.findViewById(R.id.tvTitleAdvice);
            imageView = itemView.findViewById(R.id.imageViewAdvice);
            tvDate = itemView.findViewById(R.id.tvTimeAdvice);

            // НОВОЕ: Находим TextView описания по ID.
            // ВАЖНО: Убедитесь, что ID (tvDescriptionAdvice) совпадает с тем, что вы написали в XML!
            tvDescription = itemView.findViewById(R.id.tvDescriptionAdvice);
        }
    }
}