package com.example.fitnesapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.example.fitnesapp.R;
import com.example.fitnesapp.models.Edvice;
import com.example.fitnesapp.models.TrainingStat;
import com.example.fitnesapp.models.firebase.HealthLogItem;
import com.example.fitnesapp.models.WorkoutSessionUI;
import com.github.mikephil.charting.data.Entry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutsFeedAdapter extends RecyclerView.Adapter<WorkoutsFeedAdapter.ViewHolder> {

    private final Context context;
    private final List<WorkoutSessionUI> sessions;

    // Пул для переиспользования вложенных RecyclerView (оптимизация)
    private final RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();

    public WorkoutsFeedAdapter(Context context, List<WorkoutSessionUI> sessions) {
        this.context = context;
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_workout_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutSessionUI session = sessions.get(position);

        // 1. Заполняем тексты (Тип, Время)
        holder.tvType.setText(session.workoutItem.type);
        holder.tvCalories.setText(String.valueOf(session.workoutItem.calories));

        int h = session.workoutItem.durationMin / 60;
        int m = session.workoutItem.durationMin % 60;
        holder.tvDuration.setText(String.format(Locale.US, "%02d:%02d:00", h, m));

        // 2. Расчет Дистанции и Темпа (логика из фрагмента переехала сюда)
        double avgPaceVal = calculateAvgPaceVal(session.paceLogs);

        // Если логов темпа нет, используем грубый расчет по калориям
        double distance;
        if (avgPaceVal > 0) {
            distance = session.workoutItem.durationMin / avgPaceVal;
            // Форматируем темп
            int pMin = (int) avgPaceVal;
            int pSec = (int) ((avgPaceVal - pMin) * 60);
            holder.tvPace.setText(String.format(Locale.US, "%d:%02d", pMin, pSec));
        } else {
            distance = session.workoutItem.calories / 65.0; // Запасная формула
            holder.tvPace.setText("--:--");
        }
        holder.tvDistance.setText(String.format(Locale.US, "%.2f", distance));

        // 3. Время старта/конца
        long start = session.workoutItem.timestamp;
        long end = start + (session.workoutItem.durationMin * 60000L);
        SimpleDateFormat sdf = new SimpleDateFormat("H:mm", Locale.US);
        holder.tvStart.setText("Start " + sdf.format(new Date(start)));
        holder.tvEnd.setText("End " + sdf.format(new Date(end)));

        // 4. Внутренний RecyclerView для Графиков (Карусель)
        setupStatsCarousel(holder.recyclerStats, session);

        // 5. Внутренний RecyclerView для Советов
        setupAdviceList(holder.recyclerAdvice);
    }

    private void setupStatsCarousel(RecyclerView recycler, WorkoutSessionUI session) {
        List<TrainingStat> data = new ArrayList<>();

        // Пульс
        ChartDataResult pRes = convert(session.pulseLogs);
        String avgPulse = calcAvg(session.pulseLogs);
        data.add(new TrainingStat("Pulse", avgPulse, R.drawable.ic_heart_icon, R.color.pulse_start, R.color.pulse_end, pRes.entries, pRes.labels));

        // Кислород
        ChartDataResult oxRes = convert(session.oxygenLogs);
        String avgOx = calcAvg(session.oxygenLogs);
        data.add(new TrainingStat("Blood oxygen", avgOx, R.drawable.ic_heart_oxygen_icon, R.color.oxygen_start, R.color.oxygen_end, oxRes.entries, oxRes.labels));

        // Темп
        ChartDataResult pcRes = convert(session.paceLogs);
        data.add(new TrainingStat("Temp", "Avg", R.drawable.ic_temp, R.color.temp_start, R.color.temp_end, pcRes.entries, pcRes.labels));

        TrainingAdapter adapter = new TrainingAdapter(context, data);
        recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(adapter);
        recycler.setRecycledViewPool(viewPool); // Оптимизация

        // SnapHelper (один раз)
        if (recycler.getOnFlingListener() == null) {
            new PagerSnapHelper().attachToRecyclerView(recycler);
        }
    }

    private void setupAdviceList(RecyclerView recycler) {
        List<Edvice> dummyList = new ArrayList<>();
        dummyList.add(new Edvice(R.drawable.ic_heart_oxygen_icon, "Workout Finished!", "End"));
        dummyList.add(new Edvice(R.drawable.ic_advice_history, "Good job!", "Middle"));

        AdviceHistoryAdapter adapter = new AdviceHistoryAdapter(context, dummyList);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(adapter);
        recycler.setRecycledViewPool(viewPool);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    // --- Хелперы расчетов ---

    private double calculateAvgPaceVal(List<HealthLogItem> logs) {
        if (logs == null || logs.isEmpty()) return 0;
        double sum = 0;
        for (HealthLogItem item : logs) sum += item.val;
        return sum / logs.size();
    }

    private String calcAvg(List<HealthLogItem> logs) {
        if (logs == null || logs.isEmpty()) return "--";
        double sum = 0;
        for (HealthLogItem i : logs) sum += i.val;
        return String.valueOf((int)(sum / logs.size()));
    }

    private ChartDataResult convert(List<HealthLogItem> logs) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        if (logs != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            for (int i = 0; i < logs.size(); i++) {
                entries.add(new Entry(i, (float) logs.get(i).val));
                labels.add(sdf.format(new Date(logs.get(i).time)));
            }
        }
        return new ChartDataResult(entries, labels.toArray(new String[0]));
    }

    static class ChartDataResult {
        ArrayList<Entry> entries;
        String[] labels;
        ChartDataResult(ArrayList<Entry> e, String[] l) { entries = e; labels = l; }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDuration, tvDistance, tvPace, tvCalories, tvStart, tvEnd;
        RecyclerView recyclerStats, recyclerAdvice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvTrainingType);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvPace = itemView.findViewById(R.id.tvTemp);
            tvCalories = itemView.findViewById(R.id.tvCaloriesBurned);
            tvStart = itemView.findViewById(R.id.tvStartTran);
            tvEnd = itemView.findViewById(R.id.tvEndTran);
            recyclerStats = itemView.findViewById(R.id.recyclerTrainingStats);
            recyclerAdvice = itemView.findViewById(R.id.recyclerAdviceHistory);
        }
    }
}