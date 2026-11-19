package com.travelvault.strategy;

import com.travelvault.Memory;
import java.util.List;

public interface MemoryFilterStrategy {
    String getDescription();
    List<Memory> filter(List<Memory> memories);
}