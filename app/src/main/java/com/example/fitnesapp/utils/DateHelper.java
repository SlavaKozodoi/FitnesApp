package com.example.fitnesapp.utils;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesapp.Adapters.CalendarPagerAdapter;
import com.example.fitnesapp.models.CalendarDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;// DateHelper.java
import java.util.Locale;

    public class DateHelper {

        // weeksHistoryCount - сколько недель НАЗАД мы хотим видеть (например, 15)
        public static List<List<CalendarDate>> getWeeksHistory(int weeksHistoryCount) {
            List<List<CalendarDate>> weeks = new ArrayList<>();

            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);

            // 1. Ставим календарь на Понедельник ТЕКУЩЕЙ недели
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

            // 2. Отматываем назад на нужное количество недель (это будет наша точка старта)
            calendar.add(Calendar.WEEK_OF_YEAR, -weeksHistoryCount);

            // 3. Генерируем недели от прошлого до "сегодня"
            // Цикл идет от 0 до weeksHistoryCount включительно (итого weeksHistoryCount + 1 недель)
            for (int w = 0; w <= weeksHistoryCount; w++) {
                List<CalendarDate> daysOfWeek = new ArrayList<>();

                for (int d = 0; d < 7; d++) {
                    Date date = calendar.getTime();
                    String dayNum = new SimpleDateFormat("d", Locale.getDefault()).format(date);
                    boolean isToday = android.text.format.DateUtils.isToday(date.getTime());

                    daysOfWeek.add(new CalendarDate(date, dayNum, isToday));

                    // Двигаемся на 1 день вперед
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                weeks.add(daysOfWeek);
            }
            return weeks;
        }

        public static void setupHistoryCalendar(Context context, RecyclerView recyclerView, CalendarPagerAdapter.OnDateClickListener listener) {
            // 1. Генерируем данные (20 недель истории + текущая)
            // Можно вынести число 20 в параметры метода, если нужно разное количество для разных экранов
            List<List<CalendarDate>> weeksData = DateHelper.getWeeksHistory(13);

            // 2. Создаем адаптер
            CalendarPagerAdapter adapter = new CalendarPagerAdapter(context, weeksData, listener);

            // 3. Ищем "Сегодня" и выделяем его
            for (List<CalendarDate> week : weeksData) {
                for (CalendarDate day : week) {
                    if (android.text.format.DateUtils.isToday(day.getDate().getTime())) {
                        adapter.setSelectedDate(day);
                        break;
                    }
                }
            }

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            // 4. Настраиваем "прилипание" страниц (SnapHelper)
            if (recyclerView.getOnFlingListener() == null) {
                PagerSnapHelper snapHelper = new PagerSnapHelper();
                snapHelper.attachToRecyclerView(recyclerView);
            }

            // 5. Скроллим в конец (к текущей неделе)
            recyclerView.post(() -> {
                if (weeksData.size() > 0) {
                    recyclerView.scrollToPosition(weeksData.size() - 1);
                }
            });
        }
    }
