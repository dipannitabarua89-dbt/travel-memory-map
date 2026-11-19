package com.travelvault.strategy;

import com.travelvault.Memory;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryFilterStrategy implements MemoryFilterStrategy {
    private final String category;

    public CategoryFilterStrategy(String category) {
        this.category = category.trim().toUpperCase();
    }

    @Override
    public String getDescription() {
        return "Category: " + category;
    }

    @Override
    public List<Memory> filter(List<Memory> memories) {
        return memories.stream()
                .filter(m -> m.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}