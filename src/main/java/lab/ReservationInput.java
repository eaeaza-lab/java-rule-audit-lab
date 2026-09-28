package lab;

/** Input to inventory reservation for one SKU: available and requested units. */
public record ReservationInput(String skuId, int availableQty, int requestedQty) {
    public ReservationInput {
        if (skuId == null || skuId.isEmpty()) {
            throw new IllegalArgumentException("skuId required");
        }
        if (availableQty < 0 || requestedQty < 0) {
            throw new IllegalArgumentException("negative quantity");
        }
    }
}
