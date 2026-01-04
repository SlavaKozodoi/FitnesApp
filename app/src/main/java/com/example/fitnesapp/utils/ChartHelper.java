package com.example.fitnesapp.utils;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.example.fitnesapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;

public class ChartHelper {


    public static void setupUnifiedChart(Context context, LineChart chart, ArrayList<Entry> entries, String[] timeLabels, int startColorResId, int endColorResId, boolean showBackground) {        if (chart == null || entries == null || entries.isEmpty()) return;

        // 1. ПОЛУЧЕНИЕ ЦВЕТОВ
        int startColor = ContextCompat.getColor(context, startColorResId);
        int endColor = ContextCompat.getColor(context, endColorResId);
        float density = context.getResources().getDisplayMetrics().density;

        // 2. НАСТРОЙКА ДАННЫХ (DATASET)
        LineDataSet dataSet = new LineDataSet(entries, "Data");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Для градиента лучше плавная линия
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(2f);

        // Цвет самой линии (берем начальный цвет градиента)
        dataSet.setColor(startColor);

        // === ГРАДИЕНТНАЯ ЗАЛИВКА ===
        dataSet.setDrawFilled(true);

        if (android.os.Build.VERSION.SDK_INT >= 18) {
            // Создаем градиент (сверху вниз)
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{startColor, endColor} // Массив цветов
            );
            // Применяем градиент к DataSet
            dataSet.setFillDrawable(drawable);
        }

        LineData data = new LineData(dataSet);
        chart.setData(data);

        // 3. АВТОМАТИЧЕСКИЙ МАСШТАБ ОСИ Y
        // Находим мин и макс значения, чтобы график красиво заполнял высоту
        float yMin = entries.get(0).getY();
        float yMax = entries.get(0).getY();
        for (Entry e : entries) {
            if (e.getY() < yMin) yMin = e.getY();
            if (e.getY() > yMax) yMax = e.getY();
        }

        // Добавляем отступы сверху и снизу (20%), чтобы линия не прилипала к краям
        float offset = (yMax - yMin) * 0.2f;
        if (offset == 0) offset = 1f; // Защита от прямой линии

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(yMin - offset);
        leftAxis.setAxisMaximum(yMax + offset);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(false); // Скрываем цифры оси Y

        chart.getAxisRight().setEnabled(false); // Правая ось не нужна

        // 4. НАСТРОЙКА ОСИ X (ПОДПИСИ СНИЗУ)
        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE); // Внутри, снизу
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#E0E0E0")); // Светло-серый текст
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f); // Шаг 1 (чтобы не было дробных чисел 1.5)

        // Эта настройка сдвигает крайние метки внутрь, чтобы они не обрезались экраном
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setXOffset(10f); // Небольшой отступ меток друг от друга

        // Устанавливаем свой форматер (даты или время), если передан массив строк
        if (timeLabels != null && timeLabels.length > 0) {
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                    int index = (int) value;
                    if (index >= 0 && index < timeLabels.length) {
                        return timeLabels[index];
                    }
                    return "";
                }
            });
        }

        // 5. ОБЩИЕ НАСТРОЙКИ ГРАФИКА
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);

        // Включаем взаимодействие (для маркера)
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false); // Зум обычно не нужен в таких виджетах
        chart.setPinchZoom(false);

        // Подключаем наш кастомный маркер (всплывающее окошко)
        // Убедитесь, что класс CustomMarkerView создан в пакете utils
        CustomMarkerView mv = new CustomMarkerView(context, R.layout.view_custom_marker, timeLabels);
        mv.setChartView(chart);
        chart.setMarker(mv);

        // 6. ФОН
        chart.setDrawGridBackground(false); // Отключаем встроенный квадратный фон


            // Для больших графиков истории (темный скругленный фон)
            // Для маленьких графиков в карусели (прозрачный фон)
            chart.setBackgroundColor(Color.TRANSPARENT);


        // 7. ОТСТУПЫ (VIEW PORT OFFSETS)
        // Рассчитываем отступы в пикселях, чтобы текст точно влезал

        // Нижний отступ под текст
        float bottomOffsetDp = showBackground ? 0f : 0f;
        float bottomOffsetPx = bottomOffsetDp * density;

        // Боковой отступ, чтобы текст не прилипал к краям экрана
        float sideOffsetDp = 1f;
        float sideOffsetPx = sideOffsetDp * density;

        // Применяем: (Слева, Сверху, Справа, Снизу)
        chart.setViewPortOffsets(sideOffsetPx, 0f, sideOffsetPx, bottomOffsetPx);

        // 8. ЗАПУСК
        chart.animateY(1000); // Анимация роста за 1 секунду
        chart.invalidate();   // Перерисовка
    }
}