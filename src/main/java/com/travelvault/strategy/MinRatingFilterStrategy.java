package com.travelvault.strategy;

import com.travelvault.Memory;
import java.util.List;
import java.util.stream.Collectors;

public class MinRatingFilterStrategy implements MemoryFilterStrategy {
    private final int minRating;

    public MinRatingFilterStrategy(int minRating) {
        this.minRating = minRating;
    }

    @Override
    public String getDescription() {
        return "Rating >= " + minRating + "/5";
    }

    @Override
    public List<Memory> filter(List<Memory> memories) {
        return memories.stream()
                .filter(m -> m.getRating() >= minRating)
                .collect(Collectors.toList());
    }
}