# Defect Log

Real defects found while building and testing this project. Each entry
follows: ID, description, steps to reproduce, expected vs actual, severity,
priority, status (New -> In Progress -> Fixed -> Verified -> Closed).

Severity: how bad the impact is if it ships. Priority: how urgent it is to
fix relative to other work.

---

## DEF-001: Integration tests fail intermittently with a unique-constraint violation

**Found:** Phase 6, while first running `AppointmentServiceIT`.
**Component:** `src/test/java/et/aau/clinic/integration/AppointmentServiceIT.java`
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
