# Defect Log

Real defects found while building and testing this project. Each entry
follows: ID, description, steps to reproduce, expected vs actual, severity,
priority, status (New -> In Progress -> Fixed -> Verified -> Closed).

Severity: how bad the impact is if it ships. Priority: how urgent it is to
fix relative to other work.

---

## DEF-001: Integration tests fail intermittently with a unique-constraint violation

**Found:** Phase 6, while first running `AppointmentServiceIT`.
**Component:** `backend/src/test/java/et/aau/clinic/integration/AppointmentServiceIT.java`
**Severity:** Medium (test suite reliability, not a production defect)
**Priority:** High (blocked `mvn verify` from passing at all)
**Status:** Closed

### Steps to reproduce

1. Write `AppointmentServiceIT` as a plain `@SpringBootTest` (no
   `@Transactional`) with a `@BeforeEach` that inserts a `Patient` row with a
   fixed username (`"tigist"`).
2. Run `mvn clean verify`.

### Expected

All test methods in the class pass independently.

### Actual

The first test method passes. Every subsequent test method in the same
class fails with:

```
org.springframework.dao.DataIntegrityViolationException: ... Unique index
or primary key violation: "PUBLIC.CONSTRAINT_INDEX_F ON
PUBLIC.PATIENT(USERNAME NULLS FIRST) VALUES ( /* 1 */ 'tigist' )"
```

### Root cause

Spring's `@SpringBootTest` reuses one application context (and therefore one
H2 database instance, via `DB_CLOSE_DELAY=-1`) across every test method in
the class. Without `@Transactional`, each test method's writes are committed
immediately and never rolled back, so the second `@BeforeEach` tries to
insert a `Patient` with a username that the first test already committed,
tripping the `UNIQUE` constraint on `Patient.username`.

### Fix

Added `@Transactional` at the class level on `AppointmentServiceIT` and
`AppointmentJourneyIT` (Phase 6). Each test method now runs inside a
transaction that is rolled back when the method ends, so writes from one
test are never visible to the next.

Deliberately **not** applied to `BookingJourneyIT` (Phase 7, Selenium):
Selenium drives the app over a real HTTP socket from a separate thread, so
an open, uncommitted test-thread transaction would simply be invisible to
the server thread handling the browser's requests. That class avoids the
collision instead by using a distinct seeded username per test method.

### Verification

`mvn clean verify` passes with all `AppointmentServiceIT` and
`AppointmentJourneyIT` methods green, run repeatedly.

---

## DEF-002: CI coverage-summary step fails with a confusing secondary error whenever the build already failed

**Found:** Phase 8/E, while running the deliberate regression demo (see
`docs/regression-demo.md`) - the boundary-value test correctly caught the
regression, but a second, unrelated CI failure appeared alongside it.
**Component:** `.github/workflows/ci.yml`, "Publish coverage summary" step.
**Severity:** Low (cosmetic/noise in CI output, does not affect the app or
the test results themselves)
**Priority:** Medium (worth fixing - a real failure's log gets buried under
an unrelated one, making the CI output harder to read)
**Status:** Closed

### Steps to reproduce

1. Break a test in `et.aau.clinic.core` (e.g. shift a fee boundary) so
   `mvn clean verify` fails during the unit-test phase.
2. Push to a branch and let the GitHub Actions `CI` workflow run.

### Expected

Only the actual test failure is reported; the coverage-summary step is
skipped or clearly reports "no coverage available", since there is nothing
to summarise.

### Actual

The "Publish coverage summary" step (which runs unconditionally via
`if: always()`) also fails:

```
awk: fatal: cannot open file `target/site/jacoco/jacoco.csv' for reading: No such file or directory
Error: Process completed with exit code 2.
```

### Root cause

`jacoco.csv` is produced by the `jacoco-maven-plugin:report` goal, which is
bound to Maven's `verify` phase. Maven's default `fail-fast` behaviour stops
the build at the first failing phase, so when unit tests fail in the earlier
`test` phase, `verify` never runs and the CSV is never written - but the
summary step assumed it always would be.

### Fix

Added a file-existence check at the top of the step: if `jacoco.csv` is
missing, write a one-line "no coverage report was generated" note to the job
summary and exit successfully instead of trying to parse a file that was
never created.

### Verification

Reproduced on the `regression-demo` branch
([failing run](https://github.com/BIRUK-MULATU/clinic_booking-/actions/runs/33305001970)),
fixed in the same commit that reverted the regression
([passing run](https://github.com/BIRUK-MULATU/clinic_booking-/actions/runs/33305212152)).

---

## DEF-003: Selenium system test is flaky under constrained CPU (Jenkins-in-Docker) - NoSuchElementException right after a form submit

**Found:** verifying the Jenkins pipeline actually runs end to end (Part E /
Jenkins requirement), first real build against `docker/jenkins/`.
**Component:** `backend/src/test/java/et/aau/clinic/system/pages/*.java` (Page
Objects).
**Severity:** Medium (a real, reproducible test-suite defect - not the
application, but it would fail a real CI run unpredictably)
**Priority:** High (a flaky system test is worse than a slow one - fails
intermittently, hard to trust)
**Status:** Closed

### Steps to reproduce

1. Run the full journey (`BookingJourneyIT`) in an environment with limited
   CPU relative to a GitHub-hosted runner, e.g. the Jenkins Docker container
   used for `docker/jenkins/` on a shared host.
2. `ConfirmationPage.getStatus()` (and other page-object accessors) called
   `driver.findElement(...)` directly, immediately after a `.click()` that
   triggers a server-side redirect.

### Expected

The system test passes reliably regardless of how fast the server happens
to respond.

### Actual

```
org.openqa.selenium.NoSuchElementException: no such element: Unable to
locate element: {"method":"css selector","selector":"#confirmation-status"}
```

The same test passes reliably in GitHub Actions (hosted runner, more CPU)
but failed the first time it ran inside the Jenkins container.

### Root cause

Selenium's WebDriver spec only guarantees that `click()` blocks until a
triggered navigation *starts*, not until the resulting page has finished
rendering. The page objects called `driver.findElement()` immediately after
a click, implicitly assuming the next page was already fully rendered. That
assumption silently held on a fast, lightly-loaded GitHub-hosted runner and
broke under the Docker container's tighter CPU budget - a race condition,
not a fluke.

### Fix

Added `AbstractPage`, a common base class for all five page objects, with a
`find(...)` helper backed by `WebDriverWait` + `ExpectedConditions.presenceOfElementLocated`
(10s timeout). Every locator call in every page object now waits for its
target element instead of assuming it is already there. No behaviour change
on the fast path - the wait returns immediately once the element exists.

### Verification

Reproduced in the Jenkins container (build #3 of the `clinic-booking-pipeline`
job), fixed and verified with `mvn clean verify` locally, then re-run in the
same Jenkins container (build #4) to confirm the fix under the same
constrained conditions that first exposed the bug.

The fix reduces the failure rate - it does not prove the underlying race is
eliminated, since an explicit wait shortens the window rather than removing
it. As a data point: the same failure family (`BookingJourneyIT`, status
assertion mismatch right after a redirect) recurred once on 2026-09-05, on a
plain local `mvn clean verify` rerun on a machine under heavier load than
usual (backend, frontend dev server, and browser sessions running
concurrently), and was gone on immediate retry. Treat this defect as
mitigated, not closed against recurrence under sufficiently adverse timing.
