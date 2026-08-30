# Demonstrated regression (assignment Part E)

Required evidence: introduce a change that breaks a test, show the CI
pipeline catching it, then fix it and show the pipeline pass again.

## The regression

Branch: `regression-demo` (never merged into `main` - `main` was never broken).

**Commit `9b6c9fe`** - "Regression demo: shift CHILD upper age boundary from
17 to 16". Changed one line in `FeeCalculator.calculate`:

```diff
- if (age <= 17) {
+ if (age <= 16) {
```

This is a deliberate off-by-one on Rule 1's CHILD/ADULT boundary: a
17-year-old patient would now be charged the ADULT fee (250 ETB) instead of
the CHILD fee (100 ETB).

**GitHub Actions run (failing):**
<https://github.com/BIRUK-MULATU/clinic_booking-/actions/runs/33305001970>
Status: **Failure**, 22s. The `FeeCalculatorTest.fee_atUpperBoundaryOfChildBand_age17_is100`
boundary-value test - written in Phase 3, before this regression ever
existed - caught it immediately:

```
[ERROR] et.aau.clinic.unit.FeeCalculatorTest.fee_atUpperBoundaryOfChildBand_age17_is100 -- Time elapsed: 0.024 s <<< FAILURE!
org.opentest4j.AssertionFailedError:
expected: CHILD
 but was: ADULT
```

This is exactly what boundary value analysis is for: the test exists at
age 17 specifically because that is the boundary, and a one-line shift of
the boundary was caught by the one test written to sit exactly on it.

### A second, real defect this run surfaced (DEF-002)

The run's "Publish coverage summary" step *also* failed, with an unrelated
and confusing error:

```
awk: fatal: cannot open file `target/site/jacoco/jacoco.csv' for reading: No such file or directory
```

Root cause: `jacoco.csv` is written by a plugin goal bound to Maven's
`verify` phase. Maven stops at the first failing phase, so when the unit
tests fail during the `test` phase, `verify` never runs and the CSV is
never produced - but the summary step still tried to read it. Logged and
fixed alongside the regression fix; see `docs/defect-log.md` DEF-002.

## The fix

**Commit `4a98ab9`** - "Fix regression: restore CHILD upper age boundary to
17". Reverts the boundary to `age <= 17`, and fixes DEF-002 by having the
coverage-summary step check the file exists first and skip cleanly if not.

**GitHub Actions run (passing):**
<https://github.com/BIRUK-MULATU/clinic_booking-/actions/runs/33305212152>
Status: **Success**, 52s. Full suite (58 unit + 9 integration + 2 Selenium
system tests) green, coverage gate passed.

## Summary for the report

| | Commit | CI run | Result |
|---|---|---|---|
| Before | `9b6c9fe` | [run 33305001970](https://github.com/BIRUK-MULATU/clinic_booking-/actions/runs/33305001970) | Failure (22s) |
| After | `4a98ab9` | [run 33305212152](https://github.com/BIRUK-MULATU/clinic_booking-/actions/runs/33305212152) | Success (52s) |

Both runs remain visible under the `regression-demo` branch in the repo's
Actions tab for as long as the branch/workflow history is retained.
