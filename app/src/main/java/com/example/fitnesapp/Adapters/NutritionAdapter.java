package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.fitnesapp.R;
import com.example.fitnesapp.models.Nutrition;

import java.util.List;

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.NutritionViewHolder> {

    Context context;
    List<Nutrition> nutList;
    public NutritionAdapter(Context context,List<Nutrition> nutList){
    this.context = context;
    this.nutList = nutList;
    }
    @NonNull
    @Override
    public NutritionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nutrition_card, parent, false);
        return new NutritionAdapter.NutritionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutritionViewHolder holder, int position) {

        Nutrition nutrition = nutList.get(position);

        holder.tvTime.setText(nutrition.getTime());
        holder.tvCarbs.setText(context.getString(R.string.text_carbs) + ": " + nutrition.getCarbs() + "g");
        holder.tvProteins.setText(context.getString(R.string.text_proteins) + ": " + nutrition.getProteins() + "g");
        holder.tvFats.setText(context.getString(R.string.text_fats) + ": " + nutrition.getFats() + "g");
        holder.tvCalories.setText(context.getString(R.string.text_calories) + ": " + nutrition.getCalories());
    }

    @Override
    public int getItemCount() {
        return nutList.size();
    }

    public class NutritionViewHolder extends RecyclerView.ViewHolder{

        TextView tvTime,tvCarbs,tvProteins,tvFats,tvCalories;
        public NutritionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTimeNut);
            tvCarbs = itemView.findViewById(R.id.tvCarbs);
            tvProteins = itemView.findViewById(R.id.tvProteins);
            tvFats = itemView.findViewById(R.id.tvFats);
            tvCalories = itemView.findViewById(R.id.tvCalories);
        }
    }
}
