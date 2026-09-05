# Hospital expansion — feature branch only

This branch extends the clinic booking system beyond the coursework scope.
CLAUDE.md's "no new features" rule applies to main, NOT to this branch.

Everything else in CLAUDE.md still holds: the Clock injection, the
NotificationService seam, core/ staying free of Spring annotations, and
the testing discipline.

## Non-negotiable constraints

1. The existing five Thymeleaf templates and their element IDs must not
   change. The Selenium Page Objects bind to them. New pages get new IDs.
2. Every new class in et.aau.clinic.core needs unit tests. JaCoCo enforces
   80% branch coverage on that package and `mvn verify` fails below it.
3. New business rules follow the same discipline as the existing ones:
   explicit thresholds, a reason object rather than a boolean, and tests
   derived from equivalence partitioning, boundary value analysis, a
   decision table or a state transition table, with a traceability
   comment naming the derivation.
4. H2 stays. No Postgres, no Docker for the app itself.
5. No Spring Security. Session-based login as before.

## Scope

Build in this order, one phase at a time, stopping after each:

- Phase A: Doctors and departments. A doctor has a specialty and belongs
  to a department. Slots belong to a doctor.
- Phase B: Doctor availability. A weekly recurring schedule that generates
  slots, replacing hand-inserted ones. Handle exceptions (leave, holidays).
- Phase C: Waitlist. When a CONFIRMED appointment is cancelled, the slot
  is offered to the next waitlisted patient. Extends the existing state
  machine rather than replacing it.
- Phase D: Check-in and queue. Arrival time separate from appointment
  time; states for waiting, called, in-consultation.
- Phase E: Visit records. Notes, diagnosis and prescription attached to
  an ATTENDED appointment.

## Working agreement

Same as CLAUDE.md: plan first, wait for approval, stop after each phase,
commit per phase, explain non-obvious decisions.
