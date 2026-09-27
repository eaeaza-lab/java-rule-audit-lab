# Java Business Rule Audit Lab

An offline Java CLI that runs synthetic inventory, allocation, and tax scenarios through explicit, versioned business rules. It emits an explainable decision trace, invariant violations, and a compact HTML audit report. Golden edge-case tests pin intentional legacy behavior so it cannot be silently "fixed".

Status: **MVP in progress**. Scenario CSV input is available; allocation, invariant checks, reports,
and generated scenarios are upcoming milestones.

## Run

Requires JDK 21 and Node 22 (Node is only used to drive the check script).

```
node scripts/check.js          # compile + run all tests
java -cp build/classes lab.Main
java -cp build/classes lab.Main run --input samples/basic
```

Java standard library only. Synthetic data only. No network.

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
