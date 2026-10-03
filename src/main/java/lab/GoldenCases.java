package lab;

import java.util.List;

/**
 * Pinned examples of intentional legacy behavior.
 *
 * <p>The version strings here are deliberately literals rather than references to rule constants.
 * A rule behavior change must therefore add a new version and preserve the old implementation for
 * these cases instead of silently moving the golden expectation.</p>
 */
public final class GoldenCases {
    private static final String TAX_V1 = "tax-v1";
    private static final String ALLOCATION_V1 = "allocation-v1";

    private GoldenCases() {}

    /** Runs every pinned case and returns the number that passed. */
    public static int verify(RuleRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry required");
        }

        Rule<?, ?> tax = requireRegistered(registry, "tax", TAX_V1);
        RuleResult<Long> halfCentTax = evaluate(tax, new TaxInput(5, 1_000));
        require(halfCentTax.value() == 1L
                        && halfCentTax.trace().format().equals(
                                "rule=tax@tax-v1 inputs=[netCents=5 rateBps=1000] result=taxCents=1"),
                "tax@tax-v1 half-cent amounts must round up");

        Rule<?, ?> allocation = requireRegistered(registry, "allocation", ALLOCATION_V1);
        RuleResult<AllocationResult> tieBreak = evaluate(allocation, new AllocationInput(
                List.of(new Sku("SKU-0001", "Warehouse-A", 5, 200)),
                List.of(new Order("ORD-0002", "SKU-0001", 3, 1),
                        new Order("ORD-0001", "SKU-0001", 4, 1))));
        require(tieBreak.value().allocations().equals(List.of(
                        new Allocation("ORD-0001", "SKU-0001", 4, 4),
                        new Allocation("ORD-0002", "SKU-0001", 3, 1))),
                "allocation@allocation-v1 must break equal priorities by lexical order id");

        RuleResult<AllocationResult> priority = evaluate(allocation, new AllocationInput(
                List.of(new Sku("SKU-0001", "Warehouse-A", 2, 200)),
                List.of(new Order("ORD-0001", "SKU-0001", 2, 9),
                        new Order("ORD-9999", "SKU-0001", 2, 1))));
        require(priority.value().allocations().equals(List.of(
                        new Allocation("ORD-9999", "SKU-0001", 2, 2),
                        new Allocation("ORD-0001", "SKU-0001", 2, 0))),
                "allocation@allocation-v1 must serve lower numeric priority first");

        return 3;
    }

    private static Rule<?, ?> requireRegistered(RuleRegistry registry, String id, String version) {
        return registry.find(id, version).orElseThrow(
                () -> new IllegalStateException("golden case requires " + id + "@" + version));
    }

    @SuppressWarnings("unchecked")
    private static <I, O> RuleResult<O> evaluate(Rule<?, ?> rule, I input) {
        return ((Rule<I, O>) rule).evaluate(input);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("golden case failed: " + message);
        }
    }
}
