package lab;

import java.util.List;

/** Stock and order lines supplied to the deterministic allocation rule. */
public record AllocationInput(List<Sku> skus, List<Order> orders) {
    public AllocationInput {
        if (skus == null || orders == null) {
            throw new IllegalArgumentException("skus and orders required");
        }
        skus = List.copyOf(skus);
        orders = List.copyOf(orders);
    }
}
