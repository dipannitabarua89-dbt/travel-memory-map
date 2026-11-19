package com.travelvault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class City {
    private final String name;
    private final List<Memory> memories;

    public City(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("City name cannot be empty.");
        }
        this.name = name.trim();
        this.memories = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addMemory(Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Cannot add a null memory.");
        }
        memories.add(memory);
    }

    public boolean removeMemoryByTitle(String title) {
        return memories.removeIf(m -> m.getTitle().equalsIgnoreCase(title.trim()));
    }

    public List<Memory> getMemories() {
        return Collections.unmodifiableList(memories);
    }

    public double getAverageRating() {
        if (memories.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (Memory m : memories) {
            sum += m.getRating();
        }
        return (double) sum / memories.size();
    }
}