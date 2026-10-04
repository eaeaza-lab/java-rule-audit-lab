package lab;

import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 5 && args[0].equals("generate") && args[1].equals("--seed")
                && args[3].equals("--out")) {
            try {
                SyntheticScenarioGenerator.generate(Long.parseLong(args[2]), Path.of(args[4]));
                System.out.println("Generated synthetic scenario in " + args[4]);
            } catch (IllegalArgumentException exception) {
                System.err.println("Generation error: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length == 3 && args[0].equals("verify-determinism") && args[1].equals("--seed")) {
            try {
                verifyDeterminism(Long.parseLong(args[2]));
                System.out.println("Determinism verified for seed " + args[2]);
            } catch (IllegalArgumentException exception) {
                System.err.println("Determinism error: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length == 1 && args[0].equals("golden")) {
            try {
                int passed = GoldenCases.verify(RuleRegistry.standard());
                System.out.println(passed + " legacy-behavior golden cases passed");
            } catch (IllegalStateException exception) {
                System.err.println("Golden verification failed: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length >= 3 && args[0].equals("run") && args[1].equals("--input")) {
            try {
                Scenario scenario = CsvScenarioReader.read(Path.of(args[2]));
                RuleResult<AllocationResult> allocation = AllocationRule.INSTANCE.evaluate(
                        new AllocationInput(scenario.skus(), scenario.orders()));
                List<Decision> decisions = decisions(scenario, allocation.value());
                List<InvariantViolation> violations = InvariantChecker.check(scenario, allocation.value(), decisions);
                List<TraceEntry> traces = traces(scenario, allocation, decisions);
                RunOptions options = runOptions(args);
                if (options.tracePath() != null) {
                    writeTrace(options.tracePath(), traces);
                }
                if (options.reportPath() != null) {
                    HtmlAuditReport.write(options.reportPath(), scenario, traces, decisions, violations);
                }
                System.out.println("Loaded scenario: " + scenario.skus().size() + " stock lines, "
                        + scenario.orders().size() + " orders, tax rate " + scenario.taxRateBps() + " bps");
                if (!violations.isEmpty()) {
                    System.out.println("Invariant violations:");
                    for (InvariantViolation violation : violations) {
                        System.out.println(violation.format());
                    }
                    System.exit(2);
                }
            } catch (IOException | IllegalArgumentException exception) {
                System.err.println("Input error: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        System.out.println("java-rule-audit-lab (work in progress), rule " + TaxRule.VERSION);
    }

    private static void verifyDeterminism(long seed) {
        Path first = Path.of("build", "determinism-first");
        Path second = Path.of("build", "determinism-second");
        SyntheticScenarioGenerator.generate(seed, first);
        SyntheticScenarioGenerator.generate(seed, second);
        for (String name : List.of("stock.csv", "orders.csv", "tax-rate.csv")) {
            try {
                if (!java.util.Arrays.equals(Files.readAllBytes(first.resolve(name)),
                        Files.readAllBytes(second.resolve(name)))) {
                    throw new IllegalArgumentException("generated " + name + " differs for the same seed");
                }
            } catch (IOException exception) {
                throw new IllegalArgumentException("cannot verify generated " + name, exception);
            }
        }
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

    private static List<TraceEntry> traces(Scenario scenario, RuleResult<AllocationResult> allocation,
            List<Decision> decisions) {
        List<TraceEntry> traces = new ArrayList<>();
        Map<String, Integer> remainingBySku = new LinkedHashMap<>();
        for (Sku sku : scenario.skus()) {
            remainingBySku.merge(sku.id(), sku.onHand(), Math::addExact);
        }
        for (Allocation item : allocation.value().allocations()) {
            int availableQty = remainingBySku.getOrDefault(item.skuId(), 0);
            RuleResult<Reservation> reservation = InventoryReservationRule.INSTANCE.evaluate(
                    new ReservationInput(item.skuId(), availableQty, item.requestedQty()));
            traces.add(reservation.trace());
            remainingBySku.put(item.skuId(), reservation.value().remainingQty());
        }
        traces.add(allocation.trace());
        for (Decision decision : decisions) {
            traces.add(TaxRule.INSTANCE.evaluate(new TaxInput(decision.netCents(), scenario.taxRateBps())).trace());
        }
        return List.copyOf(traces);
    }

    private static RunOptions runOptions(String[] args) {
        Path tracePath = null;
        Path reportPath = null;
        for (int index = 3; index < args.length; index += 2) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("missing value for " + args[index]);
            }
            if (args[index].equals("--trace") && tracePath == null) {
                tracePath = Path.of(args[index + 1]);
            } else if (args[index].equals("--report") && reportPath == null) {
                reportPath = Path.of(args[index + 1]);
            } else {
                throw new IllegalArgumentException("expected optional --trace or --report");
            }
        }
        return new RunOptions(tracePath, reportPath);
    }

    private static void writeTrace(Path path, List<TraceEntry> traces) throws IOException {
        List<String> lines = new ArrayList<>();
        for (TraceEntry trace : traces) {
            lines.add(trace.format());
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private record RunOptions(Path tracePath, Path reportPath) {}

    private static long unitPrice(List<Sku> skus, String skuId) {
        for (Sku sku : skus) {
            if (sku.id().equals(skuId)) {
                return sku.unitPriceCents();
            }
        }
        return 0;
    }
}
