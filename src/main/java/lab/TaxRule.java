package lab;

/** Versioned tax rule: integer cents, basis points, half-up rounding (intentional legacy behavior). */
public final class TaxRule implements Rule<TaxInput, Long> {
    public static final String VERSION = "tax-v1";
    public static final TaxRule INSTANCE = new TaxRule();

    private TaxRule() {}

    @Override
    public String id() {
        return "tax";
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public RuleResult<Long> evaluate(TaxInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input required");
        }
        long taxCents = taxCents(input.netCents(), input.rateBps());
        TraceEntry trace = new TraceEntry(id(), version(),
                "netCents=" + input.netCents() + " rateBps=" + input.rateBps(),
                "taxCents=" + taxCents);
        return new RuleResult<>(taxCents, trace);
    }

    /** Returns tax in cents for a net amount at a rate given in basis points (1 bp = 0.01%). */
    public static long taxCents(long netCents, int rateBps) {
        if (netCents < 0 || rateBps < 0) {
            throw new IllegalArgumentException("negative input");
        }
        return (netCents * rateBps + 5_000) / 10_000;
    }
}
