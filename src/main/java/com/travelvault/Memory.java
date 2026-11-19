package com.travelvault;

import java.time.LocalDate;

public abstract class Memory {
    private final String title;
    private final LocalDate dateVisited;
    private final int rating; // 1 to 5
    private final String notes;

    public Memory(String title, LocalDate dateVisited, int rating, String notes) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Memory title cannot be empty.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        this.title = title.trim();
        this.dateVisited = dateVisited != null ? dateVisited : LocalDate.now();
        this.rating = rating;
        this.notes = notes != null ? notes.trim() : "";
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDateVisited() {
        return dateVisited;
    }

    public int getRating() {
        return rating;
    }

    public String getNotes() {
        return notes;
    }

    // Polymorphic methods implemented by subclasses
    public abstract String getCategory();

    public abstract String getSpecificDetails();

    public String getSummary() {
        return String.format("[%s] %s (%s) - Rating: %d/5 | %s | Notes: %s",
                getCategory(), title, dateVisited, rating, getSpecificDetails(),
                notes.isEmpty() ? "None" : notes);
    }
}