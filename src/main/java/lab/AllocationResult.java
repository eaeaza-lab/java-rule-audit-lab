package lab;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All order allocations and the quantity left for each SKU. */
public record AllocationResult(List<Allocation> allocations, Map<String, Integer> remainingQtyBySku) {
    public AllocationResult {
        if (allocations == null || remainingQtyBySku == null) {
            throw new IllegalArgumentException("allocations and remaining quantities required");
        }
        allocations = List.copyOf(allocations);
        remainingQtyBySku = Collections.unmodifiableMap(new LinkedHashMap<>(remainingQtyBySku));
    }
}
