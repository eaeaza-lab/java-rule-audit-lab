# PLANS

Each milestone fits one ~30-minute session. Acceptance command for all: `node scripts/check.js` (plus the extra command where listed).

## Milestones
- [x] M0 setup (spec): docs, skeleton, tax rule, test runner. Stage: mvp
- [x] M1 Core model: `Scenario`, `Sku`, `Order`, `Decision`, `TraceEntry` records; trace formatting test. Stage: mvp
- [x] M2 CSV reader: parse stock/orders/tax-rate files with line-numbered errors. Accept: check + `java -cp build/classes lab.Main run --input samples/basic`. Stage: mvp
- [x] M3 Rule interface + registry with versions; port tax rule into it with trace output. Stage: mvp
- [x] M4 Inventory reservation rule with trace and unit tests. Stage: mvp
- [x] M5 Allocation rule (priority, then order id tie-break) with trace and tests. Stage: mvp
- [x] M6 Invariant checker (non-negative stock, no over-allocation, totals match); exit code 2 on violations. Accept: check + criterion 4 command. Stage: mvp
- [x] M7 Seeded synthetic generator + `verify-determinism`. Accept: criterion 2. Stage: mvp
- [x] M8 Golden legacy-behavior cases and `golden` command with version-pinning. Accept: criterion 5. Stage: mvp
- [ ] M9 HTML audit report (self-contained, escaped). Accept: criteria 4 and 6. Stage: mvp
- [ ] M10 JSON-like rule-config reader (rule versions, rates). Stage: polish
- [ ] M11 Report polish: collapsible traces, summary table, print CSS. Stage: polish
- [ ] M12 README walkthrough with sample output; CLI `--help`. Stage: polish

## Progress log
- 2026-09-25 M0: wrote SPEC/PLANS/README/AGENTS, `.nightshift.json`, `scripts/check.js`, `TaxRule`, `TestRunner` with 4 passing tests.
- 2026-09-26 M1: added `Sku`, `Order`, `Scenario`, `Decision`, `TraceEntry` records with validation and `TraceEntry.format()`; 4 new tests (not run locally, verified by reading).
- 2026-09-27 M2: added strict UTF-8 CSV scenario input for stock, orders, and tax rates, with file-and-line errors; the `run --input` CLI now confirms the parsed scenario and includes a synthetic basic sample.
- 2026-09-28 M3: added a versioned rule interface and deterministic registry; `tax@tax-v1` now evaluates to both its cents result and an explainable trace entry, with registry and trace tests.
- 2026-09-29 M4: added `inventory-reservation@reservation-v1`, which caps each SKU request at available units and traces both reserved and remaining stock; added full, partial, and empty-request tests.
- 2026-09-30 M5: added `allocation@allocation-v1`, which allocates orders per SKU by ascending priority and then lexical order ID, traces the ordered allocation outcome, and has tie-break, depletion, and multi-stock-line tests.
- 2026-10-01 M6: added post-allocation reconciliation for remaining stock, per-SKU allocation ceilings, and tax-base decisions; `run` prints actionable violations and exits 2 when reconciliation fails.
- 2026-10-02 M7: added a seeded fictional CSV generator and byte-level `verify-determinism` command; regression tests ensure generated scenarios are readable and identical for an equal seed.
- 2026-10-03 M8: added three pinned legacy edge cases and the `golden` CLI command, which reports its passed-case count and fails if a historical rule version or behavior changes.

## Decision log
- 2026-09-25: Check command is `node scripts/check.js` because the runner allowlist has no Java/Maven entry; the Node script only shells out to `javac`/`java`.
- 2026-09-25: No JUnit; a tiny custom `TestRunner` keeps the project standard-library-only.
- 2026-09-25: Money is integer cents; tax rate in basis points; half-up rounding pinned as legacy behavior (`tax-v1`).
- 2026-09-26: Records validate in compact constructors (fail fast on negatives); `Scenario` holds one flat tax rate in bps; trace line format is `rule=<id>@<version> inputs=[..] result=..` so every line names rule id and version (criterion 3).
- 2026-09-27: M2 fixes the three required filenames and exact headers so scenario inputs are unambiguous; it supports standard quoted fields but rejects blank rows and malformed column counts to preserve precise line-numbered diagnostics.
- 2026-09-28: M3 looks rules up by both id and version, allowing a later rule version to coexist with legacy behavior instead of silently replacing it.
- 2026-09-29: M4 represents reservation as a single-SKU request with explicit remaining quantity, so later allocation can compose it deterministically while retaining an auditable stock transition.
- 2026-09-30: M5 aggregates multiple stock lines with the same SKU before allocation because orders reference SKUs rather than warehouses; the allocation result preserves per-SKU remaining quantities for the forthcoming invariant checker.
- 2026-10-01: M6 derives a decision's tax base from the first stock-line price for its SKU, matching the existing aggregated allocation model; the checker deliberately reports distinct stable codes for negative stock, over-allocation, and tax-base reconciliation failures.
- 2026-10-02: M7 uses Java's explicitly seeded `Random` and fixed row counts, IDs, UTF-8 encoding, and LF line endings so generated scenario bytes are stable across runs and suitable for direct comparison.
- 2026-10-03: M8 stores golden rule versions and expected outcomes as literals, then evaluates the version selected from the registry; this lets a replacement implementation preserve an old version while preventing either a silent version drift or an in-place behavior change.
