package lab;

/** A stock line: units on hand for a SKU at a warehouse, with a unit price in cents. */
public record Sku(String id, String warehouse, int onHand, long unitPriceCents) {
    public Sku {
        if (id == null || id.isEmpty() || warehouse == null || warehouse.isEmpty()) {
            throw new IllegalArgumentException("id and warehouse required");
        }
        if (onHand < 0 || unitPriceCents < 0) {
            throw new IllegalArgumentException("negative value");
        }
    }
}
