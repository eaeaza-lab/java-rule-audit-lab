package lab;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 3 && args[0].equals("run") && args[1].equals("--input")) {
            try {
                Scenario scenario = CsvScenarioReader.read(Path.of(args[2]));
                RuleResult<AllocationResult> allocation = AllocationRule.INSTANCE.evaluate(
                        new AllocationInput(scenario.skus(), scenario.orders()));
                List<Decision> decisions = decisions(scenario, allocation.value());
                List<InvariantViolation> violations = InvariantChecker.check(scenario, allocation.value(), decisions);
                System.out.println("Loaded scenario: " + scenario.skus().size() + " stock lines, "
                        + scenario.orders().size() + " orders, tax rate " + scenario.taxRateBps() + " bps");
                if (!violations.isEmpty()) {
                    System.out.println("Invariant violations:");
                    for (InvariantViolation violation : violations) {
                        System.out.println(violation.format());
                    }
                    System.exit(2);
                }
            } catch (CsvInputException exception) {
                System.err.println("Input error: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        System.out.println("java-rule-audit-lab (work in progress), rule " + TaxRule.VERSION);
    }

    private static List<Decision> decisions(Scenario scenario, AllocationResult allocationResult) {
        List<Decision> decisions = new ArrayList<>();
        for (Allocation allocation : allocationResult.allocations()) {
            long unitPrice = unitPrice(scenario.skus(), allocation.skuId());
            long netCents = Math.multiplyExact((long) allocation.allocatedQty(), unitPrice);
            long taxCents = TaxRule.taxCents(netCents, scenario.taxRateBps());
            decisions.add(new Decision(allocation.orderId(), allocation.allocatedQty(), netCents, taxCents));
        }
        return decisions;
    }

    private static long unitPrice(List<Sku> skus, String skuId) {
        for (Sku sku : skus) {
            if (sku.id().equals(skuId)) {
                return sku.unitPriceCents();
            }
        }
        return 0;
    }
}
