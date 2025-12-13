package com.example.fitnesapp.utils;

import android.content.Context;
import android.widget.TextView;

import com.example.fitnesapp.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private final TextView tvDate;
    private final TextView tvValue;
    private final String[] timeLabels; // Сюда передадим массив дат

    /**
     * @param context Контекст
     * @param layoutResource ID макета (R.layout.view_custom_marker)
     * @param timeLabels Массив подписей оси X, чтобы показывать дату вместо числа
     */
    public CustomMarkerView(Context context, int layoutResource, String[] timeLabels) {
        super(context, layoutResource);
        this.timeLabels = timeLabels;
        tvDate = findViewById(R.id.tvContentDate);
        tvValue = findViewById(R.id.tvContentValue);
    }

    // Этот метод вызывается каждый раз при перерисовке маркера (при движении пальца)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // 1. Устанавливаем Значение (Y)
        // Можно добавить проверку: если это сон, писать "баллы", если вес - "кг"
        tvValue.setText(String.format(Locale.getDefault(), "%.1f", e.getY()));

        // 2. Устанавливаем Дату (X)
        int index = (int) e.getX();
        if (timeLabels != null && index >= 0 && index < timeLabels.length) {
            tvDate.setText(timeLabels[index]);
        } else {
            tvDate.setText(""); // Или скрыть
        }

        super.refreshContent(e, highlight);
    }

    // Этот метод отвечает за позиционирование маркера (чтобы он был по центру над точкой)
    @Override
    public MPPointF getOffset() {
        // Смещаем маркер: по X - на половину ширины влево (чтобы центр был над точкой)
        // по Y - на всю высоту вверх (чтобы маркер был НАД точкой)
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 20);
    }
}