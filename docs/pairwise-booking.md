# Pairwise (all-pairs) test design — self-service booking

Technique: **combinatorial / pairwise testing**, applied to
`AppointmentService.requestBooking`. Implemented by
`backend/src/test/java/et/aau/clinic/integration/BookingPairwiseIT.java`.

## Factors and values

| Factor | Values | Rule it exercises |
|---|---|---|
| age band | CHILD, ADULT, SENIOR | Rule 1 — consultation fee |
| slot free | FREE, TAKEN | Rule 2 C1 |
| balance | CLEAR, OWING | Rule 2 C2 |
| notice | ENOUGH (≥ 2h), SHORT | Rule 2 C3 |
| suspension | CLEAR, SUSPENDED (3 no-shows in 90 days) | Rule J — checked before C1 |
| coverage | 0%, 50%, 100% | Rule I — net payable |

Full Cartesian product = 3 × 2 × 2 × 2 × 2 × 3 = **144** combinations.

## Covering array

A greedy all-pairs generator (`docs/pairwise-booking-gen.py`) produces a
**10-row** array in which every pair of factor values co-occurs in at
least one row. It is seeded with the three approve-path rows
`(CHILD, 0%) (ADULT, 50%) (SENIOR, 100%)` so that each age × coverage fee
combination is also checked on a *successful* booking, not only on a
rejection.

One supplementary row (**TC-P11**) is added beyond the covering array so
that `OUTSTANDING_BALANCE` shows up as an actually-reported reason — in
the covering array it is always masked by a higher-priority condition
(suspension or C1).

Total: **11 test cases**, TC-P01 … TC-P11.

## Expected-outcome derivation

Priority order: **Rule J (suspension) → C1 (slot free) → C2 (balance) →
C3 (notice) → approve**. On approval the fee is Rule 1's for the age band
and the net payable is Rule I's: `fee − fee × coverage% ÷ 100`, half-up
to 2 dp.

| Case | age | slot | balance | notice | suspension | coverage | Expected |
|---|---|---|---|---|---|---|---|
| TC-P01 | CHILD | FREE | CLEAR | ENOUGH | CLEAR | 0% | approve · CHILD · 100.00 |
| TC-P02 | ADULT | FREE | CLEAR | ENOUGH | CLEAR | 50% | approve · ADULT · 125.00 |
| TC-P03 | SENIOR | FREE | CLEAR | ENOUGH | CLEAR | 100% | approve · SENIOR · 0.00 |
| TC-P04 | CHILD | TAKEN | OWING | SHORT | SUSPENDED | 50% | SUSPENDED_NO_SHOWS |
| TC-P05 | ADULT | FREE | OWING | SHORT | SUSPENDED | 0% | SUSPENDED_NO_SHOWS |
| TC-P06 | SENIOR | TAKEN | CLEAR | SHORT | SUSPENDED | 100% | SUSPENDED_NO_SHOWS |
| TC-P07 | ADULT | TAKEN | OWING | ENOUGH | CLEAR | 100% | SLOT_UNAVAILABLE |
| TC-P08 | SENIOR | TAKEN | OWING | ENOUGH | SUSPENDED | 0% | SUSPENDED_NO_SHOWS |
| TC-P09 | CHILD | FREE | CLEAR | SHORT | CLEAR | 100% | INSUFFICIENT_NOTICE |
| TC-P10 | SENIOR | FREE | CLEAR | ENOUGH | CLEAR | 50% | approve · SENIOR · 75.00 |
| TC-P11 | ADULT | FREE | OWING | ENOUGH | CLEAR | 0% | OUTSTANDING_BALANCE |

## Observation for the report

Because suspension is the highest-priority gate and appears in 4 of the
10 covering-array rows, `SUSPENDED_NO_SHOWS` dominates the rejection
outcomes. This is expected for pairwise coverage over a short-circuiting
decision and is why the approve-path rows are seeded and TC-P11 is added
— pairwise coverage of the *inputs* does not by itself guarantee
coverage of the *outcomes*.
