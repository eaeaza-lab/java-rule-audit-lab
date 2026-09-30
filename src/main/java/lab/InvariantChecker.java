package lab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Checks that a completed allocation has not created impossible stock or money results. */
public final class InvariantChecker {
    private InvariantChecker() {}

    /**
     * Checks remaining stock, per-SKU allocation limits, and the decision tax base.
     * Decisions must correspond one-for-one with the allocation result in its stable order.
     */
    public static List<InvariantViolation> check(
            Scenario scenario, AllocationResult allocationResult, List<Decision> decisions) {
        if (scenario == null || allocationResult == null || decisions == null) {
            throw new IllegalArgumentException("scenario, allocation result, and decisions required");
        }
        List<InvariantViolation> violations = new ArrayList<>();
        Map<String, Integer> stockBySku = stockBySku(scenario.skus());
        Map<String, Integer> allocatedBySku = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : allocationResult.remainingQtyBySku().entrySet()) {
            if (entry.getValue() < 0) {
                violations.add(new InvariantViolation("negative-stock",
                        "sku " + entry.getKey() + " has remaining quantity " + entry.getValue()));
            }
        }
        for (Allocation allocation : allocationResult.allocations()) {
            allocatedBySku.merge(allocation.skuId(), allocation.allocatedQty(), Math::addExact);
        }
        for (Map.Entry<String, Integer> entry : allocatedBySku.entrySet()) {
            int availableQty = stockBySku.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > availableQty) {
                violations.add(new InvariantViolation("over-allocation",
                        "sku " + entry.getKey() + " allocated " + entry.getValue()
                                + " but only " + availableQty + " units exist"));
            }
        }

        long allocatedTaxBase = taxBase(scenario.skus(), allocationResult.allocations());
        long decisionTaxBase = 0;
        if (decisions.size() != allocationResult.allocations().size()) {
            violations.add(new InvariantViolation("tax-base-mismatch",
                    "decision count " + decisions.size() + " does not match allocation count "
                            + allocationResult.allocations().size()));
        }
        int sharedCount = Math.min(decisions.size(), allocationResult.allocations().size());
        for (int index = 0; index < sharedCount; index++) {
            Decision decision = decisions.get(index);
            Allocation allocation = allocationResult.allocations().get(index);
            decisionTaxBase = Math.addExact(decisionTaxBase, decision.netCents());
            if (!decision.orderId().equals(allocation.orderId()) || decision.allocatedQty() != allocation.allocatedQty()) {
                violations.add(new InvariantViolation("tax-base-mismatch",
                        "decision for " + decision.orderId() + " does not match allocation for "
                                + allocation.orderId()));
            }
        }
        for (int index = sharedCount; index < decisions.size(); index++) {
            decisionTaxBase = Math.addExact(decisionTaxBase, decisions.get(index).netCents());
        }
        if (decisionTaxBase != allocatedTaxBase) {
            violations.add(new InvariantViolation("tax-base-mismatch",
                    "allocated tax base " + allocatedTaxBase + " differs from decision tax base " + decisionTaxBase));
        }
        return List.copyOf(violations);
    }

    private static Map<String, Integer> stockBySku(List<Sku> skus) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (Sku sku : skus) {
            quantities.merge(sku.id(), sku.onHand(), Math::addExact);
        }
        return quantities;
    }

    private static long taxBase(List<Sku> skus, List<Allocation> allocations) {
        Map<String, Long> priceBySku = new LinkedHashMap<>();
        for (Sku sku : skus) {
            priceBySku.putIfAbsent(sku.id(), sku.unitPriceCents());
        }
        long total = 0;
        for (Allocation allocation : allocations) {
            long unitPrice = priceBySku.getOrDefault(allocation.skuId(), 0L);
            total = Math.addExact(total, Math.multiplyExact((long) allocation.allocatedQty(), unitPrice));
        }
        return total;
    }
}
