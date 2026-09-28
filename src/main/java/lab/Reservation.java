package lab;

/** Reservation outcome for one SKU request, including stock left after the reservation. */
public record Reservation(int reservedQty, int remainingQty) {
    public Reservation {
        if (reservedQty < 0 || remainingQty < 0) {
            throw new IllegalArgumentException("negative quantity");
        }
    }
}
