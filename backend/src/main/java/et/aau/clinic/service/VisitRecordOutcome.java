package et.aau.clinic.service;

import et.aau.clinic.core.VisitRecordDecision;
import et.aau.clinic.domain.VisitRecord;

/**
 * What VisitRecordService.record() returns: the policy decision, plus
 * the saved VisitRecord when (and only when) that decision approved.
 * Mirrors BookingOutcome exactly - the caller checks decision.isApproved()
 * and reads whichever field applies.
 */
public record VisitRecordOutcome(VisitRecordDecision decision, VisitRecord record) {
}
