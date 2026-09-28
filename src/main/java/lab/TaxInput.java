package lab;

/** Input to the tax rule: net amount in cents and rate in basis points. */
public record TaxInput(long netCents, int rateBps) {
    public TaxInput {
        if (netCents < 0 || rateBps < 0) {
            throw new IllegalArgumentException("negative input");
        }
    }
}
