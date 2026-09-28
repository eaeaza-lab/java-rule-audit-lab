package lab;

/** Versioned inventory reservation rule that never reserves more than available stock. */
public final class InventoryReservationRule implements Rule<ReservationInput, Reservation> {
    public static final String VERSION = "reservation-v1";
    public static final InventoryReservationRule INSTANCE = new InventoryReservationRule();

    private InventoryReservationRule() {}

    @Override
    public String id() {
        return "inventory-reservation";
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public RuleResult<Reservation> evaluate(ReservationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input required");
        }
        int reservedQty = Math.min(input.availableQty(), input.requestedQty());
        Reservation reservation = new Reservation(reservedQty, input.availableQty() - reservedQty);
        TraceEntry trace = new TraceEntry(id(), version(),
                "skuId=" + input.skuId() + " availableQty=" + input.availableQty()
                        + " requestedQty=" + input.requestedQty(),
                "reservedQty=" + reservation.reservedQty() + " remainingQty=" + reservation.remainingQty());
        return new RuleResult<>(reservation, trace);
    }
}
