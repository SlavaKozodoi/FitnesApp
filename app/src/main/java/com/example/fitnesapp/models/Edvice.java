package com.example.fitnesapp.models;

public class Edvice {
    private int imageEdvice;
    private String title;
    private String time;

    public Edvice(int imageEdvice, String title, String time) {
        this.imageEdvice = imageEdvice;
        this.title = title;
        this.time = time;
    }

    public int getImageEdvice() {
        return imageEdvice;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }
}
