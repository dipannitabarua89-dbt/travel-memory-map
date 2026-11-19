package com.travelvault;

import java.time.LocalDate;

public class LandmarkMemory extends Memory {
    private final String architecturalStyle;
    private final double entryFeeEur;

    public LandmarkMemory(String title, LocalDate dateVisited, int rating, String notes,
                          String architecturalStyle, double entryFeeEur) {
        super(title, dateVisited, rating, notes);
        if (entryFeeEur < 0) {
            throw new IllegalArgumentException("Entry fee cannot be negative.");
        }
        this.architecturalStyle = (architecturalStyle == null || architecturalStyle.isBlank())
                ? "General" : architecturalStyle.trim();
        this.entryFeeEur = entryFeeEur;
    }

    public String getArchitecturalStyle() {
        return architecturalStyle;
    }

    public double getEntryFeeEur() {
        return entryFeeEur;
    }

    @Override
    public String getCategory() {
        return "LANDMARK";
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Style: %s, Entry: €%.2f", architecturalStyle, entryFeeEur);
    }
}