package com.example.fitnesapp.models;

public class Edvice {
    private int imageEdvice;
    private String title;
    private String time;
    private String description;

    public Edvice(int imageEdvice, String title,String description, String time) {
        this.imageEdvice = imageEdvice;
        this.title = title;
        this.description = description;
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

    public String getDescription() {
        return description;
    }
}
