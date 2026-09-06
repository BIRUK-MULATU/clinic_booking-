# Mutation testing — PIT

Technique: **mutation testing**, used to check that the unit tests for
`et.aau.clinic.core` actually *detect* faults rather than merely execute
the lines (which line/branch coverage alone proves).

## Setup

`org.pitest:pitest-maven` is configured in `backend/pom.xml`:

- **Target classes:** `et.aau.clinic.core.*` — the same business logic
  JaCoCo's 80% branch gate covers.
- **Target tests:** `et.aau.clinic.unit.*` — the fast unit tests, which
  are what exercise `core/` directly.
- **Thresholds:** mutation score ≥ 90%, line coverage ≥ 80% — the build
  fails below either.
- Not bound to a phase, so `mvn verify` is unaffected.

## Running it

```bash
cd backend
mvn test-compile org.pitest:pitest-maven:mutationCoverage
# or, alongside the normal build:
mvn -Pmutation verify
```

Report: `backend/target/pit-reports/index.html`.

## Result

| Mutations generated | Killed | Mutation score |
|---|---|---|
| 165 | 165 | **100%** |

Every mutant PIT generated across the eight `core/` rule classes
(`FeeCalculator`, `BookingPolicy`, `AppointmentStateMachine`,
`VisitRecordPolicy`, `ReminderPolicy`, `CoverageCalculator`,
`SuspensionPolicy`, `ReschedulePolicy`, `WaitlistOfferPolicy`, plus the
small decision/result records) is detected by at least one unit test —
no surviving or no-coverage mutants. This is stronger evidence than the
100% branch coverage figure: it shows the boundary and decision-table
cases were derived tightly enough that flipping a `<` to `<=`, a
threshold constant, or a returned value causes a test to fail.

## CI

Both pipelines run PIT as a separate step after the main build:

- **GitHub Actions** (`.github/workflows/ci.yml`) — a "Mutation testing"
  step, a job-summary table, and the HTML report uploaded as the
  `pit-report` artifact.
- **Jenkins** (`Jenkinsfile`) — a "Mutation Testing" stage;
  `target/pit-reports/**` archived.
