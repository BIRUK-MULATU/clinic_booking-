package et.aau.clinic.unit;

import et.aau.clinic.core.VisitRecordDecision;
import et.aau.clinic.core.VisitRecordPolicy;
import et.aau.clinic.domain.VisitRejection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule E (hospital-expansion Phase E) - decision table over C1
 * (appointment ATTENDED), C2 (no existing visit record), C3 (diagnosis
 * trimmed length 1..500), evaluated in that priority order, plus
 * boundary value analysis on the 500-character diagnosis limit.
 *
 * The collapsed table has 5 outcome rows. For the two rules that sit
 * above a lower-priority condition (R1 above C2/C3, R2 above C3) there
 * are two tests each: one with the don't-care condition(s) valid and
 * one with them invalid, to prove the reported reason really does not
 * depend on them - i.e. that the priority ordering holds. BVA then
 * pins the four values around the length boundary: 0, 1, 500, 501.
 */
class VisitRecordPolicyTest {

    private static final int MAX_DIAGNOSIS_LENGTH = 500; // mirrors VisitRecordPolicy.MAX_DIAGNOSIS_LENGTH

    // TC-E01 - Decision table Rule R1 (C1=F; C2/C3 don't-care, shown valid here).
    @Test
    void visit_appointmentNotAttended_withNoRecordAndGoodDiagnosis_rejectsWithNotAttended() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(false, true, "Sprained ankle");

        assertRejected(decision, VisitRejection.APPOINTMENT_NOT_ATTENDED);
    }

    // TC-E02 - Decision table Rule R1 (C1=F) with C2/C3 don't-care shown invalid this time.
    @Test
    void visit_appointmentNotAttended_withExistingRecordAndBlankDiagnosis_stillRejectsWithNotAttended() {
        // Proves C1 outranks C2 and C3: both of those fail too, but
        // APPOINTMENT_NOT_ATTENDED must still be the reported reason.
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(false, false, "   ");

        assertRejected(decision, VisitRejection.APPOINTMENT_NOT_ATTENDED);
    }

    // TC-E03 - Decision table Rule R2 (C1=T, C2=F; C3 don't-care, shown valid here).
    @Test
    void visit_recordAlreadyExists_withGoodDiagnosis_rejectsWithAlreadyRecorded() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, false, "Sprained ankle");

        assertRejected(decision, VisitRejection.VISIT_ALREADY_RECORDED);
    }

    // TC-E04 - Decision table Rule R2 (C1=T, C2=F) with C3 don't-care shown invalid this time.
    @Test
    void visit_recordAlreadyExists_withBlankDiagnosis_stillRejectsWithAlreadyRecorded() {
        // Proves C2 outranks C3.
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, false, "");

        assertRejected(decision, VisitRejection.VISIT_ALREADY_RECORDED);
    }

    // TC-E05 - Decision table Rule R3 (C1=T, C2=T, C3=F because diagnosis is empty).
    @Test
    void visit_attendedNoRecord_emptyDiagnosis_rejectsWithDiagnosisRequired() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, true, "");

        assertRejected(decision, VisitRejection.DIAGNOSIS_REQUIRED);
    }

    // TC-E06 - EP: C3 invalid interior - diagnosis is whitespace only, which trims to empty.
    @Test
    void visit_attendedNoRecord_whitespaceOnlyDiagnosis_rejectsWithDiagnosisRequired() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, true, "     ");

        assertRejected(decision, VisitRejection.DIAGNOSIS_REQUIRED);
    }

    // TC-E07 - Defensive: a null diagnosis is treated as blank, not a NullPointerException.
    @Test
    void visit_attendedNoRecord_nullDiagnosis_rejectsWithDiagnosisRequired() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, true, null);

        assertRejected(decision, VisitRejection.DIAGNOSIS_REQUIRED);
    }

    // TC-E08 - BVA: length 0 (the empty string) - the failing side of the lower edge.
    @Test
    void diagnosisLength_zero_isRejectedAsRequired() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, true, diagnosisOfLength(0));

        assertRejected(decision, VisitRejection.DIAGNOSIS_REQUIRED);
    }

    // TC-E09 - BVA: length 1 - the passing side of the lower edge.
    @Test
    void diagnosisLength_one_isAccepted() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, true, diagnosisOfLength(1));

        assertThat(decision.isApproved()).isTrue();
    }

    // TC-E10 - BVA: length exactly 500 - the boundary itself, inclusive, still accepted.
    @Test
    void diagnosisLength_exactlyMaximum_isAccepted() {
        VisitRecordDecision decision =
                VisitRecordPolicy.evaluate(true, true, diagnosisOfLength(MAX_DIAGNOSIS_LENGTH));

        assertThat(decision.isApproved()).isTrue();
    }

    // TC-E11 - BVA: length 501 - one past the boundary, the failing side of the upper edge.
    @Test
    void diagnosisLength_oneOverMaximum_isRejectedAsTooLong() {
        VisitRecordDecision decision =
                VisitRecordPolicy.evaluate(true, true, diagnosisOfLength(MAX_DIAGNOSIS_LENGTH + 1));

        assertRejected(decision, VisitRejection.DIAGNOSIS_TOO_LONG);
    }

    // TC-E12 - Decision table Rule R4 (C1=T, C2=T, C3=T) - the sole approve row.
    @Test
    void visit_attendedNoRecord_goodDiagnosis_approves() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(true, true, "Sprained ankle, no fracture");

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).isNull();
    }

    // TC-E13 - BVA supporting case: surrounding whitespace does not count toward the length,
    // so a 500-char diagnosis padded with spaces is still accepted (trim happens first).
    @Test
    void diagnosisLength_maximumWithSurroundingWhitespace_isAccepted() {
        VisitRecordDecision decision = VisitRecordPolicy.evaluate(
                true, true, "  " + diagnosisOfLength(MAX_DIAGNOSIS_LENGTH) + "  ");

        assertThat(decision.isApproved()).isTrue();
    }

    private static String diagnosisOfLength(int length) {
        return "d".repeat(length);
    }

    private void assertRejected(VisitRecordDecision decision, VisitRejection expectedReason) {
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).isEqualTo(expectedReason);
    }
}
