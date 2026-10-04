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
                RunOptions options = runOptions(args);
                RuleConfig config = options.configPath() == null ? null : RuleConfigReader.read(options.configPath());
                if (config != null) {
                    scenario = new Scenario(scenario.skus(), scenario.orders(), config.taxRateBps());
                }
                RuleRegistry registry = RuleRegistry.standard();
                Rule<AllocationInput, AllocationResult> allocationRule = requireRule(registry, "allocation",
                        config == null ? AllocationRule.VERSION : config.allocationVersion());
                Rule<ReservationInput, Reservation> reservationRule = requireRule(registry, "inventory-reservation",
                        config == null ? InventoryReservationRule.VERSION : config.reservationVersion());
                Rule<TaxInput, Long> taxRule = requireRule(registry, "tax",
                        config == null ? TaxRule.VERSION : config.taxVersion());
                int taxRateBps = scenario.taxRateBps();
                RuleResult<AllocationResult> allocation = allocationRule.evaluate(
                        new AllocationInput(scenario.skus(), scenario.orders()));
                List<Decision> decisions = decisions(scenario, allocation.value(), taxRule, taxRateBps);
                List<InvariantViolation> violations = InvariantChecker.check(scenario, allocation.value(), decisions);
                List<TraceEntry> traces = traces(scenario, allocation, decisions, reservationRule, taxRule, taxRateBps);
                if (options.tracePath() != null) {
                    writeTrace(options.tracePath(), traces);
                }
                if (options.reportPath() != null) {
                    HtmlAuditReport.write(options.reportPath(), scenario, traces, decisions, violations);
                }
                System.out.println("Loaded scenario: " + scenario.skus().size() + " stock lines, "
                        + scenario.orders().size() + " orders, tax rate " + taxRateBps + " bps");
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

    private static List<Decision> decisions(Scenario scenario, AllocationResult allocationResult,
            Rule<TaxInput, Long> taxRule, int taxRateBps) {
        List<Decision> decisions = new ArrayList<>();
        for (Allocation allocation : allocationResult.allocations()) {
            long unitPrice = unitPrice(scenario.skus(), allocation.skuId());
            long netCents = Math.multiplyExact((long) allocation.allocatedQty(), unitPrice);
            long taxCents = taxRule.evaluate(new TaxInput(netCents, taxRateBps)).value();
            decisions.add(new Decision(allocation.orderId(), allocation.allocatedQty(), netCents, taxCents));
        }
        return decisions;
    }

    private static List<TraceEntry> traces(Scenario scenario, RuleResult<AllocationResult> allocation,
            List<Decision> decisions, Rule<ReservationInput, Reservation> reservationRule,
            Rule<TaxInput, Long> taxRule, int taxRateBps) {
        List<TraceEntry> traces = new ArrayList<>();
        Map<String, Integer> remainingBySku = new LinkedHashMap<>();
        for (Sku sku : scenario.skus()) {
            remainingBySku.merge(sku.id(), sku.onHand(), Math::addExact);
        }
        for (Allocation item : allocation.value().allocations()) {
            int availableQty = remainingBySku.getOrDefault(item.skuId(), 0);
            RuleResult<Reservation> reservation = reservationRule.evaluate(
                    new ReservationInput(item.skuId(), availableQty, item.requestedQty()));
            traces.add(reservation.trace());
            remainingBySku.put(item.skuId(), reservation.value().remainingQty());
        }
        traces.add(allocation.trace());
        for (Decision decision : decisions) {
            traces.add(taxRule.evaluate(new TaxInput(decision.netCents(), taxRateBps)).trace());
        }
        return List.copyOf(traces);
    }

    private static RunOptions runOptions(String[] args) {
        Path tracePath = null;
        Path reportPath = null;
        Path configPath = null;
        for (int index = 3; index < args.length; index += 2) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("missing value for " + args[index]);
            }
            if (args[index].equals("--trace") && tracePath == null) {
                tracePath = Path.of(args[index + 1]);
            } else if (args[index].equals("--report") && reportPath == null) {
                reportPath = Path.of(args[index + 1]);
            } else if (args[index].equals("--config") && configPath == null) {
                configPath = Path.of(args[index + 1]);
            } else {
                throw new IllegalArgumentException("expected optional --trace, --report, or --config");
            }
        }
        return new RunOptions(tracePath, reportPath, configPath);
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

    private record RunOptions(Path tracePath, Path reportPath, Path configPath) {}

    @SuppressWarnings("unchecked")
    private static <I, O> Rule<I, O> requireRule(RuleRegistry registry, String id, String version) {
        Rule<?, ?> rule = registry.find(id, version)
                .orElseThrow(() -> new IllegalArgumentException("rule config selects unregistered rule " + id + "@" + version));
        return (Rule<I, O>) rule;
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
