package com.travelvault.strategy;

import com.travelvault.Memory;
import java.util.ArrayList;
import java.util.List;

public class AllMemoriesStrategy implements MemoryFilterStrategy {
    @Override
    public String getDescription() {
        return "All Memories";
    }

    @Override
    public List<Memory> filter(List<Memory> memories) {
        return new ArrayList<>(memories);
    }
}