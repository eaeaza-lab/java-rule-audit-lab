package lab;

import java.util.List;

/** Complete rule-engine input: stock, orders and the tax rate in basis points. */
public record Scenario(List<Sku> skus, List<Order> orders, int taxRateBps) {
    public Scenario {
        if (taxRateBps < 0) {
            throw new IllegalArgumentException("negative tax rate");
        }
        skus = List.copyOf(skus);
        orders = List.copyOf(orders);
    }
}
