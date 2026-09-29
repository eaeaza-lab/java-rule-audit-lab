package lab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Versioned allocation rule: lower priority is served first, then lexical order id. */
public final class AllocationRule implements Rule<AllocationInput, AllocationResult> {
    public static final String VERSION = "allocation-v1";
    public static final AllocationRule INSTANCE = new AllocationRule();

    private AllocationRule() {}

    @Override
    public String id() {
        return "allocation";
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public RuleResult<AllocationResult> evaluate(AllocationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input required");
        }
        Map<String, Integer> availableBySku = availableBySku(input.skus());
        List<Order> ordered = new ArrayList<>(input.orders());
        ordered.sort(Comparator.comparingInt(Order::priority).thenComparing(Order::id));

        List<Allocation> allocations = new ArrayList<>();
        for (Order order : ordered) {
            int availableQty = availableBySku.getOrDefault(order.skuId(), 0);
            Reservation reservation = InventoryReservationRule.INSTANCE.evaluate(
                    new ReservationInput(order.skuId(), availableQty, order.quantity())).value();
            availableBySku.put(order.skuId(), reservation.remainingQty());
            allocations.add(new Allocation(order.id(), order.skuId(), order.quantity(), reservation.reservedQty()));
        }
        AllocationResult result = new AllocationResult(allocations, availableBySku);
        TraceEntry trace = new TraceEntry(id(), version(),
                "stockQtyBySku=" + quantities(input.skus()) + " orderIds=" + orderIds(ordered),
                "allocations=" + allocationQuantities(allocations) + " remainingQtyBySku=" + quantities(availableBySku));
        return new RuleResult<>(result, trace);
    }

    private static Map<String, Integer> availableBySku(List<Sku> skus) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (Sku sku : skus) {
            quantities.merge(sku.id(), sku.onHand(), Math::addExact);
        }
        return quantities;
    }

    private static String orderIds(List<Order> orders) {
        List<String> ids = new ArrayList<>();
        for (Order order : orders) {
            ids.add(order.id());
        }
        return String.join(",", ids);
    }

    private static String allocationQuantities(List<Allocation> allocations) {
        List<String> quantities = new ArrayList<>();
        for (Allocation allocation : allocations) {
            quantities.add(allocation.orderId() + ":" + allocation.allocatedQty());
        }
        return String.join(",", quantities);
    }

    private static String quantities(List<Sku> skus) {
        return quantities(availableBySku(skus));
    }

    private static String quantities(Map<String, Integer> quantities) {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            values.add(entry.getKey() + ":" + entry.getValue());
        }
        return String.join(",", values);
    }
}
