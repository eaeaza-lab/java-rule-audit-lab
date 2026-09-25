package lab;

/** A customer order line; lower priority number is served first. */
public record Order(String id, String skuId, int quantity, int priority) {
    public Order {
        if (id == null || id.isEmpty() || skuId == null || skuId.isEmpty()) {
            throw new IllegalArgumentException("id and skuId required");
        }
        if (quantity <= 0 || priority < 0) {
            throw new IllegalArgumentException("invalid quantity or priority");
        }
    }
}
