package lab;

/** Outcome for one order: units allocated and the tax due on the allocated net amount, in cents. */
public record Decision(String orderId, int allocatedQty, long netCents, long taxCents) {
    public Decision {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId required");
        }
        if (allocatedQty < 0 || netCents < 0 || taxCents < 0) {
            throw new IllegalArgumentException("negative value");
        }
    }
}
