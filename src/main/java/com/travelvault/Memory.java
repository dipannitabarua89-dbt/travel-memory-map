package com.travelvault;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

public abstract class Memory implements Comparable<Memory> {
    private final String title;
    private final LocalDate dateVisited;
    private final int rating;
    private final String notes;

    protected Memory(String title, LocalDate dateVisited, int rating, String notes) {
        if (title == null || title.isBlank()) {
            throw new InvalidMemoryException("Memory title cannot be blank.");
        }
        if (dateVisited == null) {
            throw new InvalidMemoryException("Date visited cannot be null.");
        }
        if (rating < 1 || rating > 5) {
            throw new InvalidMemoryException("Rating must be between 1 and 5, got: " + rating);
        }
        this.title = title.trim();
        this.dateVisited = dateVisited;
        this.rating = rating;
        this.notes = notes == null ? "" : notes.trim();
    }

    public String getTitle() { return title; }
    public LocalDate getDateVisited() { return dateVisited; }
    public int getRating() { return rating; }
    public String getNotes() { return notes; }

    public abstract String getCategory();
    public abstract String getSpecificDetails();

    public String getSummary() {
        return String.format("[%s] %s (%s) | Rating: %d/5 | %s",
                getCategory(), title, dateVisited, rating, getSpecificDetails());
    }

    @Override
    public int compareTo(Memory other) {
        return Comparator.comparingInt(Memory::getRating).reversed()
                .thenComparing(Memory::getDateVisited, Comparator.reverseOrder())
                .thenComparing(Memory::getTitle, String.CASE_INSENSITIVE_ORDER)
                .compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Memory memory)) return false;
        return rating == memory.rating
                && Objects.equals(title, memory.title)
                && Objects.equals(dateVisited, memory.dateVisited);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, dateVisited, rating);
    }
}