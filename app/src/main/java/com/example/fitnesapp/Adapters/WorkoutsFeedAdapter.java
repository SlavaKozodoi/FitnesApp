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
        holder.tvType.setText(session.workout.type);
        holder.tvCalories.setText(String.valueOf(session.workout.calories));

        long totalSecs = session.workout.durationSeconds;
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        holder.tvDuration.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m,s));

        // 2. Расчет Дистанции и Темпа (Берем точные данные из базы!)
        double distance = session.workout.distance;

        holder.tvDistance.setText(String.format(Locale.US, "%.2f", distance));

        // Если дистанция есть, считаем точный темп (в мин/км)
        if (distance > 0 && session.workout.durationSeconds > 0) {

            // 1. Переводим общее количество секунд в минуты с дробной частью (например, 374 сек = 6.233 мин)
            double durationMinutesExact = session.workout.durationSeconds / 60.0;

            // 2. Делим точное время на дистанцию (например, 6.233 / 0.5 = 12.466 мин/км)
            double decimalPace = durationMinutesExact / distance;

            // 3. Выделяем целые минуты и остаток в секундах
            int pMin = (int) decimalPace;
            int pSec = (int) Math.round((decimalPace - pMin) * 60);

            // Защита от 60 секунд (например, 12:60 превратится в 13:00)
            if (pSec == 60) {
                pMin++;
                pSec = 0;
            }

            holder.tvPace.setText(String.format(Locale.US, "%d:%02d", pMin, pSec));
        } else {
            holder.tvPace.setText("--:--");
        }

        // 3. Время старта/конца
        long start = session.workout.timestamp;
        long end = start + (session.workout.durationSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("H:mm", Locale.US);
        holder.tvStart.setText(R.string.item_workout_card_start + " " + sdf.format(new Date(start)));
        holder.tvEnd.setText(R.string.item_workout_card_end + " " + sdf.format(new Date(end)));

        // 4. Внутренний RecyclerView для Графиков (Карусель)
        setupStatsCarousel(holder.recyclerStats, session);

        // 5. Внутренний RecyclerView для Советов
        // ПЕРЕДАЕМ текущую сессию для генерации реальных советов
        setupAdviceList(holder.recyclerAdvice, session);
    }

    private void setupStatsCarousel(RecyclerView recycler, WorkoutSessionUI session) {
        List<TrainingStat> data = new ArrayList<>();

        // Пульс
        ChartDataResult pRes = convert(session.pulse);
        String avgPulse = calcAvg(session.pulse);
        data.add(new TrainingStat(context.getString(R.string.item_workout_card_pulse) , avgPulse, R.drawable.ic_heart_icon, R.color.pulse_start, R.color.pulse_end, pRes.entries, pRes.labels));

        // Кислород
        ChartDataResult oxRes = convert(session.oxygen);
        String avgOx = calcAvg(session.oxygen);
        data.add(new TrainingStat(context.getString(R.string.item_workout_card_oxygen), avgOx, R.drawable.ic_heart_oxygen_icon, R.color.oxygen_start, R.color.oxygen_end, oxRes.entries, oxRes.labels));

        // Темп
        List<HealthLogItem> speedLogs = new ArrayList<>();

        // Проверяем, есть ли данные о скорости внутри самой тренировки
        if (session.workout.speed_data != null) {
            List<String> keys = new ArrayList<>(session.workout.speed_data.keySet());
            java.util.Collections.sort(keys);

            for (String timeKey : keys) {
                HealthLogItem item = new HealthLogItem();
                item.time = Long.parseLong(timeKey);
                item.val = session.workout.speed_data.get(timeKey); // Это скорость в км/ч
                speedLogs.add(item);
            }
        }
        ChartDataResult pcRes = convert(speedLogs);
        String avgSpeed = calcAvg(speedLogs);
        data.add(new TrainingStat(context.getString(R.string.item_workout_card_speed), avgSpeed, R.drawable.ic_temp, R.color.temp_start, R.color.temp_end, pcRes.entries, pcRes.labels));

        TrainingAdapter adapter = new TrainingAdapter(context, data);
        recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(adapter);
        recycler.setRecycledViewPool(viewPool); // Оптимизация

        // SnapHelper
        if (recycler.getOnFlingListener() == null) {
            new PagerSnapHelper().attachToRecyclerView(recycler);
        }
    }

    // ==========================================
    // ЛОГИКА УМНЫХ СОВЕТОВ (ВИРТУАЛЬНЫЙ ТРЕНЕР)
    // ==========================================
    private void setupAdviceList(RecyclerView recycler, WorkoutSessionUI session) {
        List<Edvice> adviceListForAdapter = new ArrayList<>();

        // Проверяем, есть ли сгенерированные советы от ViewModel
        if (session.advices != null && !session.advices.isEmpty()) {
            for (WorkoutSessionUI.WorkoutAdvice advice : session.advices) {
                // Преобразуем эмодзи в красивую иконку из ресурсов
                int iconResId = getIconForAdvice(advice.icon);

                // Создаем объект совета, передавая заголовок, ОПИСАНИЕ и время
                adviceListForAdapter.add(new Edvice(iconResId, advice.title, advice.description, advice.timing));
            }
        } else {
            // Фолбэк-заглушка, если советов почему-то нет
            adviceListForAdapter.add(new Edvice(R.drawable.ic_advice_history, context.getString(R.string.advice_history_workout_finished), context.getString(R.string.advice_history_workout_finished_desc), context.getString(R.string.item_workout_card_end)));
        }

        AdviceHistoryAdapter adapter = new AdviceHistoryAdapter(context, adviceListForAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(adapter);
        recycler.setRecycledViewPool(viewPool);
    }

    // Хелпер для сопоставления эмодзи с вашими векторными иконками
    private int getIconForAdvice(String emoji) {
        switch (emoji) {
            case "⚡": return R.drawable.ic_temp; // Иконка для темпа/скорости/пика
            case "🔥": return R.drawable.ic_heart_icon; // Иконка для пульса/сжигания
            case "🏆": return R.drawable.ic_heart_oxygen_icon; // Иконка успеха/кислорода
            case "💧": return R.drawable.ic_water_plus;
            case "🚶": return R.drawable.ic_ach_steps_bronze;
            case "🌙": return R.drawable.ic_ach_sleep_bear;
            case "☀️": return R.drawable.ic_ach_total_cal_bronze;
            case "🥩": return R.drawable.ic_ach_nutr_bronze;
            default: return R.drawable.ic_advice_history; // Стандартная зеленая иконка
        }
    }

    @Override
    public int getItemCount() {
        return sessions.size();
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