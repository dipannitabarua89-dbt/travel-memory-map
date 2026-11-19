package com.travelvault;

import java.time.LocalDate;

public class CulinaryMemory extends Memory {
    private final String dishName;
    private final double priceEur;

    public CulinaryMemory(String title, LocalDate dateVisited, int rating, String notes,
                          String dishName, double priceEur) {
        super(title, dateVisited, rating, notes);
        if (priceEur < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        this.dishName = (dishName == null || dishName.isBlank())
                ? "Local Specialty" : dishName.trim();
        this.priceEur = priceEur;
    }

    public String getDishName() {
        return dishName;
    }

    public double getPriceEur() {
        return priceEur;
    }

    @Override
    public String getCategory() {
        return "CULINARY";
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Dish: %s, Price: €%.2f", dishName, priceEur);
    }
}