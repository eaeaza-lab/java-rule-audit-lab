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
        System.out.println((total - failures) + "/" + total + " tests passed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
