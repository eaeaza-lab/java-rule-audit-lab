package lab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Minimal dependency-free test runner; exits non-zero on any failure. */
public final class TestRunner {
    private static int failures = 0;
    private static int total = 0;

    private TestRunner() {}

    static void check(String name, boolean ok) {
        total++;
        if (!ok) {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws IOException {
        check("tax rounds half up", TaxRule.taxCents(1_050, 1_000) == 105);
        check("tax half cent rounds up", TaxRule.taxCents(5, 1_000) == 1);
        check("tax zero", TaxRule.taxCents(0, 2_000) == 0);
        boolean threw = false;
        try {
            TaxRule.taxCents(-1, 100);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("tax rejects negatives", threw);
        RuleResult<Long> taxResult = TaxRule.INSTANCE.evaluate(new TaxInput(5, 1_000));
        check("tax rule returns tax result and trace", taxResult.value() == 1
                && taxResult.trace().format().equals(
                        "rule=tax@tax-v1 inputs=[netCents=5 rateBps=1000] result=taxCents=1"));
        RuleRegistry registry = RuleRegistry.standard();
        check("registry finds tax rule by version", registry.find("tax", TaxRule.VERSION).orElseThrow() == TaxRule.INSTANCE
                && registry.find("inventory-reservation", InventoryReservationRule.VERSION).orElseThrow()
                        == InventoryReservationRule.INSTANCE
                && registry.find("allocation", AllocationRule.VERSION).orElseThrow() == AllocationRule.INSTANCE
                && registry.all().size() == 3);
        boolean duplicateRule = false;
        try {
            registry.register(TaxRule.INSTANCE);
        } catch (IllegalArgumentException e) {
            duplicateRule = true;
        }
        check("registry rejects duplicate version", duplicateRule);
        TraceEntry t = new TraceEntry("tax", TaxRule.VERSION, "net=1050 bps=1000", "105");
        check("trace format", t.format().equals("rule=tax@tax-v1 inputs=[net=1050 bps=1000] result=105"));
        Scenario sc = new Scenario(
                java.util.List.of(new Sku("SKU-0001", "Warehouse-A", 5, 200)),
                java.util.List.of(new Order("O-1", "SKU-0001", 2, 1)), 1_000);
        check("scenario holds data", sc.skus().size() == 1 && sc.orders().get(0).quantity() == 2);
        check("decision fields", new Decision("O-1", 2, 400, 40).taxCents() == 40);
        boolean bad = false;
        try {
            new Order("O-1", "SKU-0001", 0, 1);
        } catch (IllegalArgumentException e) {
            bad = true;
        }
        check("order rejects zero quantity", bad);
        Scenario fromCsv = CsvScenarioReader.read(Path.of("samples", "basic"));
        check("csv reader loads basic scenario", fromCsv.skus().size() == 2
                && fromCsv.orders().size() == 2 && fromCsv.taxRateBps() == 750);
        Path invalid = Path.of("build", "csv-reader-invalid");
        Files.createDirectories(invalid);
        Files.writeString(invalid.resolve("stock.csv"),
                "sku_id,warehouse,on_hand,unit_price_cents\nSKU-0001,Warehouse-A,nope,250\n");
        Files.writeString(invalid.resolve("orders.csv"), "order_id,sku_id,quantity,priority\n");
        Files.writeString(invalid.resolve("tax-rate.csv"), "tax_rate_bps\n750\n");
        boolean lineNumbered = false;
        try {
            CsvScenarioReader.read(invalid);
        } catch (CsvInputException exception) {
            lineNumbered = exception.getMessage().equals("stock.csv:2: invalid integer for on_hand");
        }
        check("csv reader reports file and line", lineNumbered);
        Path generatedFirst = Path.of("build", "generated-first");
        Path generatedSecond = Path.of("build", "generated-second");
        SyntheticScenarioGenerator.generate(1L, generatedFirst);
        SyntheticScenarioGenerator.generate(1L, generatedSecond);
        check("generator creates a readable synthetic scenario", CsvScenarioReader.read(generatedFirst).skus().size() == 3
                && CsvScenarioReader.read(generatedFirst).orders().size() == 6);
        check("generator is byte-identical for the same seed", Files.mismatch(
                generatedFirst.resolve("stock.csv"), generatedSecond.resolve("stock.csv")) == -1L
                && Files.mismatch(generatedFirst.resolve("orders.csv"), generatedSecond.resolve("orders.csv")) == -1L
                && Files.mismatch(generatedFirst.resolve("tax-rate.csv"), generatedSecond.resolve("tax-rate.csv")) == -1L);
        check("golden cases pin legacy rule behavior", GoldenCases.verify(RuleRegistry.standard()) == 3);
        RuleRegistry missingLegacyTax = new RuleRegistry();
        missingLegacyTax.register(AllocationRule.INSTANCE);
        boolean goldenPinsVersion = false;
        try {
            GoldenCases.verify(missingLegacyTax);
        } catch (IllegalStateException e) {
            goldenPinsVersion = e.getMessage().equals("golden case requires tax@tax-v1");
        }
        check("golden cases require pinned rule version", goldenPinsVersion);
        RuleRegistry changedPinnedTax = new RuleRegistry();
        changedPinnedTax.register(new Rule<TaxInput, Long>() {
            @Override
            public String id() {
                return "tax";
            }

            @Override
            public String version() {
                return "tax-v1";
            }

            @Override
            public RuleResult<Long> evaluate(TaxInput input) {
                return new RuleResult<>(0L, new TraceEntry(id(), version(), "changed", "taxCents=0"));
            }
        });
        changedPinnedTax.register(AllocationRule.INSTANCE);
        boolean goldenPinsBehavior = false;
        try {
            GoldenCases.verify(changedPinnedTax);
        } catch (IllegalStateException e) {
            goldenPinsBehavior = e.getMessage().contains("half-cent amounts must round up");
        }
        check("golden cases reject changed behavior at a pinned version", goldenPinsBehavior);
        RuleResult<Reservation> partialReservation = InventoryReservationRule.INSTANCE.evaluate(
                new ReservationInput("SKU-0001", 3, 5));
        check("reservation limits request to available stock", partialReservation.value().equals(new Reservation(3, 0))
                && partialReservation.trace().format().equals("rule=inventory-reservation@reservation-v1 "
                        + "inputs=[skuId=SKU-0001 availableQty=3 requestedQty=5] "
                        + "result=reservedQty=3 remainingQty=0"));
        check("reservation preserves surplus stock", InventoryReservationRule.INSTANCE.evaluate(
                new ReservationInput("SKU-0001", 5, 2)).value().equals(new Reservation(2, 3)));
        check("reservation accepts empty request", InventoryReservationRule.INSTANCE.evaluate(
                new ReservationInput("SKU-0001", 5, 0)).value().equals(new Reservation(0, 5)));
        boolean invalidReservation = false;
        try {
            new ReservationInput("SKU-0001", -1, 1);
        } catch (IllegalArgumentException e) {
            invalidReservation = true;
        }
        check("reservation rejects negative quantities", invalidReservation);
        RuleResult<AllocationResult> allocationResult = AllocationRule.INSTANCE.evaluate(new AllocationInput(
                java.util.List.of(new Sku("SKU-0001", "Warehouse-A", 5, 200)),
                java.util.List.of(new Order("ORD-0002", "SKU-0001", 3, 1),
                        new Order("ORD-0001", "SKU-0001", 4, 1),
                        new Order("ORD-0003", "SKU-0001", 2, 2))));
        check("allocation prioritizes then breaks ties by order id", allocationResult.value().allocations().equals(
                java.util.List.of(new Allocation("ORD-0001", "SKU-0001", 4, 4),
                        new Allocation("ORD-0002", "SKU-0001", 3, 1),
                        new Allocation("ORD-0003", "SKU-0001", 2, 0)))
                && allocationResult.value().remainingQtyBySku().get("SKU-0001") == 0);
        check("allocation trace records ordered outcome", allocationResult.trace().format().equals(
                "rule=allocation@allocation-v1 inputs=[stockQtyBySku=SKU-0001:5 "
                        + "orderIds=ORD-0001,ORD-0002,ORD-0003] "
                        + "result=allocations=ORD-0001:4,ORD-0002:1,ORD-0003:0 remainingQtyBySku=SKU-0001:0"));
        RuleResult<AllocationResult> multiWarehouse = AllocationRule.INSTANCE.evaluate(new AllocationInput(
                java.util.List.of(new Sku("SKU-0001", "Warehouse-A", 2, 200),
                        new Sku("SKU-0001", "Warehouse-B", 3, 200)),
                java.util.List.of(new Order("ORD-0001", "SKU-0001", 5, 1))));
        check("allocation combines stock lines for a sku", multiWarehouse.value().allocations().get(0).allocatedQty() == 5);
        Scenario invariantScenario = new Scenario(
                java.util.List.of(new Sku("SKU-0001", "Warehouse-A", 3, 200)),
                java.util.List.of(new Order("ORD-0001", "SKU-0001", 3, 1)), 1_000);
        AllocationResult validAllocation = new AllocationResult(
                java.util.List.of(new Allocation("ORD-0001", "SKU-0001", 3, 3)),
                java.util.Map.of("SKU-0001", 0));
        check("invariants accept reconciled allocation and tax base", InvariantChecker.check(invariantScenario,
                validAllocation, java.util.List.of(new Decision("ORD-0001", 3, 600, 60))).isEmpty());
        AllocationResult invalidAllocation = new AllocationResult(
                java.util.List.of(new Allocation("ORD-0001", "SKU-0001", 4, 4)),
                java.util.Map.of("SKU-0001", -1));
        java.util.List<InvariantViolation> invariantViolations = InvariantChecker.check(invariantScenario,
                invalidAllocation, java.util.List.of(new Decision("ORD-0001", 4, 700, 70)));
        check("invariants report negative stock over-allocation and tax-base mismatch",
                invariantViolations.stream().map(InvariantViolation::code).toList().equals(
                        java.util.List.of("negative-stock", "over-allocation", "tax-base-mismatch")));
        System.out.println((total - failures) + "/" + total + " tests passed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
