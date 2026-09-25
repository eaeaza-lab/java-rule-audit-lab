package lab;

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

    public static void main(String[] args) {
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
        System.out.println((total - failures) + "/" + total + " tests passed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
