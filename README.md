# Java Business Rule Audit Lab

An offline Java CLI that runs synthetic inventory, allocation, and tax scenarios through explicit, versioned business rules. It emits an explainable decision trace, invariant violations, and a compact HTML audit report. Golden edge-case tests pin intentional legacy behavior so it cannot be silently "fixed".

Status: **MVP in progress**. Scenario CSV input, a versioned tax-rule registry, auditable
inventory reservation, and deterministic allocation are available; invariant checks, reports, and
generated scenarios are upcoming milestones.

## Run

Requires JDK 21 and Node 22 (Node is only used to drive the check script).

```
node scripts/check.js          # compile + run all tests
java -cp build/classes lab.Main
java -cp build/classes lab.Main run --input samples/basic
```

Java standard library only. Synthetic data only. No network.

## Rule trace foundation

Rules are registered by a stable id and behavior version. The tax rule is registered as
`tax@tax-v1`; evaluating it returns both the integer-cent result and a stable trace line such as:

```
rule=tax@tax-v1 inputs=[netCents=5 rateBps=1000] result=taxCents=1
```

## Inventory reservation

`inventory-reservation@reservation-v1` makes one explicit per-SKU stock transition: it reserves
the lesser of the requested and available quantities, then records the units left. For example,
an availability of 3 against a request for 5 produces:

```
rule=inventory-reservation@reservation-v1 inputs=[skuId=SKU-0001 availableQty=3 requestedQty=5] result=reservedQty=3 remainingQty=0
```

## Allocation

`allocation@allocation-v1` applies all orders for each SKU in ascending numeric priority, breaking
equal-priority ties by lexical order ID. It records each requested and allocated quantity plus the
stock left for each SKU. Stock lines for the same SKU are combined before allocation, so inventory
held in synthetic warehouses is allocated consistently.

For example, with five units and equal-priority requests `ORD-0002` for three units and
`ORD-0001` for four, `ORD-0001` receives four and `ORD-0002` receives one:

```
rule=allocation@allocation-v1 inputs=[stockQtyBySku=SKU-0001:5 orderIds=ORD-0001,ORD-0002] result=allocations=ORD-0001:4,ORD-0002:1 remainingQtyBySku=SKU-0001:0
```

## Scenario CSV files

`run --input <directory>` reads UTF-8 files named `stock.csv`, `orders.csv`, and
`tax-rate.csv`. Headers are required and rows are validated with file-and-line error messages.
The included [`samples/basic`](samples/basic) scenario uses these columns:

```
stock.csv:    sku_id,warehouse,on_hand,unit_price_cents
orders.csv:   order_id,sku_id,quantity,priority
tax-rate.csv: tax_rate_bps
```

See [SPEC.md](SPEC.md) and [PLANS.md](PLANS.md).

Built by a supervised autonomous agent pipeline (nightshift).
