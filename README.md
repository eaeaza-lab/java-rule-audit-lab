# Java Business Rule Audit Lab

An offline Java CLI that runs synthetic inventory, allocation, and tax scenarios through explicit, versioned business rules. It emits an explainable decision trace, invariant violations, and a compact HTML audit report. Golden edge-case tests pin intentional legacy behavior so it cannot be silently "fixed".

Status: **work in progress** (skeleton only: one tax rule and a test runner).

## Run

Requires JDK 21 and Node 22 (Node is only used to drive the check script).

```
node scripts/check.js          # compile + run all tests
java -cp build/classes lab.Main
```

Java standard library only. Synthetic data only. No network.

See [SPEC.md](SPEC.md) and [PLANS.md](PLANS.md).

Built by a supervised autonomous agent pipeline (nightshift).
