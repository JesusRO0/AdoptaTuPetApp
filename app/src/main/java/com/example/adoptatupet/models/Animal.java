package com.example.adoptatupet.models;

public class Animal {
    private String name;
    private String description;
    private int imageResId; // ID de recurso de imagen (R.drawable.xxx)

    public Animal(String name, String description, int imageResId) {
        this.name = name;
        this.description = description;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }
}