package lab;

/** Versioned tax rule: integer cents, basis points, half-up rounding (intentional legacy behavior). */
public final class TaxRule {
    public static final String VERSION = "tax-v1";

    private TaxRule() {}

    /** Returns tax in cents for a net amount at a rate given in basis points (1 bp = 0.01%). */
    public static long taxCents(long netCents, int rateBps) {
        if (netCents < 0 || rateBps < 0) {
            throw new IllegalArgumentException("negative input");
        }
        return (netCents * rateBps + 5_000) / 10_000;
    }
}
