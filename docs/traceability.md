# Traceability table — core/ test suite

Maps every formal test-design technique to the exact test method that implements it. Each
test method carries a one-line comment with the same case ID used here, so the mapping can be
verified from either direction: table → code, or code → table.

This table covers the **four course rules** (1, 2, 3, 3b). The seven extension rules (F–L) are
traced the same way — a class-level comment naming the technique, `TC-` comments on each method
— in their own `*Test` classes, and are summarised in `docs/deliverables/02-test-design.html`
§5; pairwise testing of the booking outcome is in §6 of the same document. Rule 3's state
machine grew from 5×4 to **7×7** (10 valid / 39 invalid) as the extensions added the
`WAITLISTED` / `OFFER_EXPIRED` states and the `PROMOTE` / `RESCHEDULE` / `EXPIRE_OFFER` events;
the section below reflects the final grid.

## Rule 1 — Fee by age (Equivalence Partitioning + Boundary Value Analysis)

`FeeCalculatorTest.java`

| Case ID | Technique element | Test method |
|---|---|---|
| TC-F01 | EP/BVA — partition P1 (invalid, age < 0), boundary just below CHILD | `fee_belowLowerBoundary_ageMinus1_throws` |
| TC-F02 | EP — partition P1 interior | `fee_wellBelowRange_ageMinus50_throws` |
| TC-F03 | EP/BVA — partition P2 (CHILD), lower boundary (0) | `fee_atLowerBoundaryOfChildBand_age0_is100` |
| TC-F04 | EP — partition P2 (CHILD) interior | `fee_withinChildBand_age9_is100` |
| TC-F05 | EP/BVA — partition P2 (CHILD), upper boundary (17) | `fee_atUpperBoundaryOfChildBand_age17_is100` |
| TC-F06 | EP/BVA — partition P3 (ADULT), lower boundary (18) | `fee_atLowerBoundaryOfAdultBand_age18_is250` |
| TC-F07 | EP — partition P3 (ADULT) interior | `fee_withinAdultBand_age40_is250` |
| TC-F08 | EP/BVA — partition P3 (ADULT), upper boundary (64) | `fee_atUpperBoundaryOfAdultBand_age64_is250` |
| TC-F09 | EP/BVA — partition P4 (SENIOR), lower boundary (65) | `fee_atLowerBoundaryOfSeniorBand_age65_is150` |
| TC-F10 | EP — partition P4 (SENIOR) interior | `fee_withinSeniorBand_age90_is150` |
| TC-F11 | EP/BVA — partition P4 (SENIOR), upper boundary (120) | `fee_atUpperBoundaryOfSeniorBand_age120_is150` |
| TC-F12 | EP/BVA — partition P5 (invalid, age > 120), boundary just above SENIOR | `fee_aboveUpperBoundary_age121_throws` |
| TC-F13 | EP — partition P5 interior | `fee_wellAboveRange_age200_throws` |

13 cases: 3 valid partitions × 3 (low boundary, interior, high boundary) + 2 invalid
partitions × 2 (near edge, far interior) = 9 + 4 = 13.

## Rule 2 — Booking eligibility (Decision Table + BVA)

`BookingPolicyTest.java`. Conditions: C1 (slot free), C2 (no outstanding balance), C3 (≥2h notice).

| Case ID | Technique element | Test method |
|---|---|---|
| TC-B01 | Decision table Rule R1 (C1=F; C2/C3 don't-care, shown true) | `decision_slotUnavailable_withGoodBalanceAndNotice_rejectsWithSlotUnavailable` |
| TC-B02 | Decision table Rule R1 (C1=F; C2/C3 don't-care, shown false) — proves C1 outranks C2, C3 | `decision_slotUnavailable_withBadBalanceAndInsufficientNotice_stillRejectsWithSlotUnavailable` |
| TC-B03 | Decision table Rule R2 (C1=T, C2=F; C3 don't-care, shown true) | `decision_outstandingBalance_withSufficientNotice_rejectsWithOutstandingBalance` |
| TC-B04 | Decision table Rule R2 (C1=T, C2=F; C3 don't-care, shown false) — proves C2 outranks C3 | `decision_outstandingBalance_withInsufficientNotice_stillRejectsWithOutstandingBalance` |
| TC-B05 | Decision table Rule R3 (C1=T, C2=T, C3=F) | `decision_insufficientNotice_rejectsWithInsufficientNotice` |
| TC-B06 | Decision table Rule R4 (C1=T, C2=T, C3=T) — the approve row | `decision_allConditionsTrue_approves` |
| TC-B07 | BVA on C3 — lower boundary of "sufficient" (exactly 2h00m) | `notice_exactlyTwoHoursBeforeSlot_isSufficient_approves` |
| TC-B08 | BVA on C3 — just below the boundary (1h59m) | `notice_oneMinuteLessThanTwoHours_isInsufficient_rejects` |
| TC-B09 | BVA on C3 — interior, well above the boundary (10h) | `notice_wellOverTwoHours_isSufficient_approves` |

9 cases: 4 decision-table rules (2 of which need a don't-care-true/false pair to prove
priority ordering = 6 cases) + 3 BVA cases on the one numeric condition.

## Rule 3 — Appointment lifecycle (State Transition Testing)

`AppointmentStateMachineTest.java`. States × events = 7 × 7 = 49 pairs (10 valid, 39 invalid).

| Case ID | Technique element | Test method |
|---|---|---|
| TC-S01 | Valid: REQUESTED --confirm--> CONFIRMED | `transition_requestedConfirm_movesToConfirmed` |
| TC-S02 | Valid: REQUESTED --cancel--> CANCELLED | `transition_requestedCancel_movesToCancelled` |
| TC-S03 | Valid: CONFIRMED --attend--> ATTENDED | `transition_confirmedAttend_movesToAttended` |
| TC-S04 | Valid: CONFIRMED --cancel--> CANCELLED | `transition_confirmedCancel_movesToCancelled` |
| TC-S05 | Valid: CONFIRMED --markNoShow--> NO_SHOW | `transition_confirmedMarkNoShow_movesToNoShow` |
| TC-S09 | Valid: WAITLISTED --promote--> REQUESTED | `transition_waitlistedPromote_movesToRequested` |
| TC-S10 | Valid: WAITLISTED --cancel--> CANCELLED | `transition_waitlistedCancel_movesToCancelled` |
| TC-S11 | Valid: REQUESTED --reschedule--> REQUESTED (self-loop, Rule K) | `transition_requestedReschedule_staysRequested` |
| TC-S12 | Valid: CONFIRMED --reschedule--> CONFIRMED (self-loop, Rule K) | `transition_confirmedReschedule_staysConfirmed` |
| TC-S13 | Valid: REQUESTED --expireOffer--> OFFER_EXPIRED (Rule L) | `transition_requestedExpireOffer_movesToOfferExpired` |
| (generated, 39×) | All 39 invalid (state, event) pairs — the full 49-pair grid minus the 10 valid | `transition_invalidPair_throwsIllegalStateException` (parameterised, data from `invalidStateEventPairs()`) |
| (meta-check) | Asserts the generated invalid set has exactly 39 members, so the grid can't silently lose or duplicate a pair; the test file also carries the row-by-row hand-derivation of the 10/39 split | `invalidStateEventPairs_containsExactlyThirtyNinePairs` |

## Rule 3b — Late cancellation fee (BVA, guard condition on the CANCEL transition)

`AppointmentStateMachineTest.java`

| Case ID | Technique element | Test method |
|---|---|---|
| TC-S06 | BVA — just inside the late window (23h59m), charges 50% | `lateCancellationFee_atTwentyThreeHours59Minutes_chargesHalfFee` |
| TC-S07 | BVA — the boundary itself (exactly 24h00m), free | `lateCancellationFee_atExactlyTwentyFourHours_isFree` |
| TC-S08 | BVA — just outside the late window (24h01m), free | `lateCancellationFee_atTwentyFourHoursOneMinute_isFree` |

## Coverage summary

| Technique | Test class | Case count |
|---|---|---|
| Equivalence partitioning + BVA | `FeeCalculatorTest` | 13 |
| Decision table + BVA | `BookingPolicyTest` | 9 |
| State transition testing | `AppointmentStateMachineTest` | 10 valid + 39 invalid (1 parameterised test) + 1 meta-check = 12 methods (53 executions) |
| BVA (guard condition) | `AppointmentStateMachineTest` | 3 |

The extension rules F–L add `PatientRegistrationPolicyTest`, `DoctorLoadTest`,
`ReminderPolicyTest`, `CoverageCalculatorTest`, `SuspensionPolicyTest`, `ReschedulePolicyTest`
and `WaitlistOfferPolicyTest`, each with the same `TC-` traceability comments (case IDs
`TC-R*`, `TC-G*`, `TC-H*`, `TC-I*`, `TC-J*`, `TC-K*`, `TC-L*`). Pairwise cases `TC-P01–P11`
are in `BookingPairwiseIT`.

Every method above is directly reachable from `et.aau.clinic.core.{FeeCalculator,
BookingPolicy, AppointmentStateMachine}` — no test in this table touches Spring, the
database, or the web layer, consistent with `core/` being pure-function and
container-free per CLAUDE.md.
