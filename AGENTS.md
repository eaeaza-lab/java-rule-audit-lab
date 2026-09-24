# AGENTS.md

## Build / test
- Full check: `node scripts/check.js` (javac with `-Xlint:all -Werror`, then runs `lab.TestRunner`). This is the only command in `.nightshift.json`.
- Run CLI after a check: `java -cp build/classes lab.Main <args>`.
- Layout: `src/main/java/lab/`, `src/test/java/lab/`, output in `build/` (git-ignored).

## Rules
- Java 21, standard library only. No Maven/Gradle/JUnit; add tests as `check(...)` calls (or classes invoked from) `lab.TestRunner`.
- Deterministic: no wall-clock, no unseeded randomness, no locale-dependent formatting; use integer cents and basis points, never floating point for money.
- Synthetic data only; no real company, person, marketplace or account names; no secrets; no network at runtime.
- Rules are versioned (`VERSION` constants). Changing legacy behavior requires a new rule version, a golden test for the old one, and a Decision log entry in PLANS.md.
- Never hand-write lockfiles or checksums. Do not commit unless asked.
- Each session: do one milestone from PLANS.md, tick it, append to the Progress log, keep the check green.
- `.nightshift.json` commands must start with an allowed runner prefix; put multi-step logic in scripts.
