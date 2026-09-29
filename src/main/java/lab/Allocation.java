package lab;

/** Allocation of one requested order line after applying the stock priority policy. */
public record Allocation(String orderId, String skuId, int requestedQty, int allocatedQty) {
    public Allocation {
        if (orderId == null || orderId.isEmpty() || skuId == null || skuId.isEmpty()) {
            throw new IllegalArgumentException("orderId and skuId required");
        }
        if (requestedQty < 0 || allocatedQty < 0 || allocatedQty > requestedQty) {
            throw new IllegalArgumentException("invalid allocation quantity");
        }
    }
}
