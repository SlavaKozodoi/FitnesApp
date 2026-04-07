package com.example.fitnesapp.models;

import java.util.Date;

public class CalendarDate {
    private Date date;
    private String dayNumber; // "27"
    private boolean isSelected;

    public CalendarDate(Date date, String dayNumber, boolean isSelected) {
        this.date = date;
        this.dayNumber = dayNumber;
        this.isSelected = isSelected;
    }

    public String getDayNumber() { return dayNumber; }
    public Date getDate() { return date; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
