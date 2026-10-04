package lab;

/** Immutable rule selections and the tax rate supplied by a JSON-like configuration file. */
public record RuleConfig(String taxVersion, String reservationVersion, String allocationVersion, int taxRateBps) {
    public RuleConfig {
        if (taxVersion == null || taxVersion.isBlank() || reservationVersion == null || reservationVersion.isBlank()
                || allocationVersion == null || allocationVersion.isBlank() || taxRateBps < 0) {
            throw new IllegalArgumentException("rule configuration has invalid values");
        }
    }
}
