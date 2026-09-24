# SPEC: Java Business Rule Audit Lab

## Problem
Business rules for stock reservation, allocation, and tax often live as undocumented code. Refactors "fix" quirky legacy behavior (rounding, tie-breaks) that downstream systems depend on, and nobody can explain why a given decision was made.

## Target user
A backend developer or reviewer who wants to learn or demonstrate how to make rule behavior explicit, explainable, and regression-proof, without a framework or network.

## MVP scope
- Offline CLI, Java 21, standard library only.
- Input: CSV scenario files (stock, orders, tax rates) plus a simple JSON-like rule-config file.
- Deterministic rule engine with versioned rules: tax (half-up rounding in basis points), inventory reservation, allocation (priority, then order id tie-break).
- Output: decision trace (one line per rule firing with rule id/version, inputs, result), invariant violations (e.g. negative stock, over-allocation, allocated total != tax base), and a compact self-contained HTML audit report.
- Seeded synthetic scenario generator (same seed gives byte-identical output).
- Golden edge-case tests that pin intentional legacy behavior.

## Non-goals
- No network, database, GUI, or web server. No real data or real business names.
- No third-party dependencies or build tools (Maven/Gradle/JUnit).
- No general rule-language/DSL, no floating-point money, no multi-currency.
- Not a production tax or inventory system.

## Acceptance criteria
1. `node scripts/check.js` exits 0 (compiles with `-Werror`, all tests pass).
2. `java -cp build/classes lab.Main generate --seed 1 --out build/s1` twice into different dirs yields identical files: `java -cp build/classes lab.Main verify-determinism --seed 1` exits 0.
3. `java -cp build/classes lab.Main run --input build/s1 --trace build/trace.txt` exits 0 and writes a non-empty trace whose lines name a rule id and version.
4. `java -cp build/classes lab.Main run --input samples/violations --report build/report.html` exits 2 (violations found) and the HTML contains an "Invariant violations" section.
5. `java -cp build/classes lab.Main golden` exits 0 and prints the count of legacy-behavior golden cases passed; changing a pinned rule without bumping its version makes it exit non-zero.
6. HTML report is self-contained: `grep -c "http" build/report.html` prints 0 (no external references).
7. No file in the repo contains real-world names; generator uses only fictional names like `SKU-0001`, `Warehouse-A`.
