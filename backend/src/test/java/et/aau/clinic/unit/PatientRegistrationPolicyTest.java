package et.aau.clinic.unit;

import et.aau.clinic.core.PatientRegistrationPolicy;
import et.aau.clinic.core.RegistrationDecision;
import et.aau.clinic.domain.RegistrationRejection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule F (hospital-expansion: admin-created patient accounts) - decision
 * table over C1 (name present), C2 (username present), C3 (username free),
 * C4 (password >= 4 chars), C5 (date of birth in the past, age <= 120),
 * evaluated in that priority order, plus boundary value analysis on the
 * password length and the two date-of-birth edges.
 *
 * For each rule that sits above a lower-priority condition there is a
 * pair of tests - the lower conditions shown once valid and once invalid -
 * to prove the reported reason does not depend on them (priority holds).
 */
class PatientRegistrationPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);
    private static final LocalDate VALID_DOB = LocalDate.of(1990, 5, 1);

    // TC-R01 - Rule R1 (C1=F); lower conditions shown valid.
    @Test
    void registration_blankName_rejectsWithNameRequired() {
        assertRejected(evaluate("  ", "tigist", "secret", VALID_DOB, false), RegistrationRejection.NAME_REQUIRED);
    }

    // TC-R02 - Rule R1 (C1=F) with every lower condition also invalid - proves C1 outranks C2..C5.
    @Test
    void registration_blankName_withEveryOtherProblem_stillRejectsWithNameRequired() {
        assertRejected(evaluate(null, "", "x", TODAY.plusDays(1), true), RegistrationRejection.NAME_REQUIRED);
    }

    // TC-R03 - Rule R2 (C1=T, C2=F); lower conditions shown valid.
    @Test
    void registration_blankUsername_rejectsWithUsernameRequired() {
        assertRejected(evaluate("Tigist", " ", "secret", VALID_DOB, false), RegistrationRejection.USERNAME_REQUIRED);
    }

    // TC-R04 - Rule R2 (C2=F) with lower conditions invalid - proves C2 outranks C3..C5.
    @Test
    void registration_blankUsername_withOtherProblems_stillRejectsWithUsernameRequired() {
        assertRejected(evaluate("Tigist", null, "x", TODAY.plusDays(1), true),
                RegistrationRejection.USERNAME_REQUIRED);
    }

    // TC-R05 - Rule R3 (C1,C2=T, C3=F: username taken); lower conditions shown valid.
    @Test
    void registration_usernameTaken_rejectsWithUsernameTaken() {
        assertRejected(evaluate("Tigist", "tigist", "secret", VALID_DOB, true), RegistrationRejection.USERNAME_TAKEN);
    }

    // TC-R06 - Rule R3 (C3=F) with lower conditions invalid - proves C3 outranks C4, C5.
    @Test
    void registration_usernameTaken_withShortPasswordAndBadDob_stillRejectsWithUsernameTaken() {
        assertRejected(evaluate("Tigist", "tigist", "x", TODAY.plusDays(1), true),
                RegistrationRejection.USERNAME_TAKEN);
    }

    // TC-R07 - Rule R4 (C4=F: password too short); C5 shown valid.
    @Test
    void registration_shortPassword_rejectsWithPasswordTooShort() {
        assertRejected(evaluate("Tigist", "tigist", "abc", VALID_DOB, false),
                RegistrationRejection.PASSWORD_TOO_SHORT);
    }

    // TC-R08 - Rule R4 (C4=F) with C5 also invalid - proves C4 outranks C5.
    @Test
    void registration_shortPassword_withBadDob_stillRejectsWithPasswordTooShort() {
        assertRejected(evaluate("Tigist", "tigist", "abc", TODAY.plusDays(1), false),
                RegistrationRejection.PASSWORD_TOO_SHORT);
    }

    // TC-R09 - BVA on C4: length 3 is the failing side of the password-length edge.
    @Test
    void passwordLength_three_isRejected() {
        assertRejected(evaluate("Tigist", "tigist", "abc", VALID_DOB, false),
                RegistrationRejection.PASSWORD_TOO_SHORT);
    }

    // TC-R10 - BVA on C4: length 4 is the passing side of the same edge.
    @Test
    void passwordLength_four_isAccepted() {
        assertThat(evaluate("Tigist", "tigist", "abcd", VALID_DOB, false).isApproved()).isTrue();
    }

    // TC-R10b - C4: a null password is treated as too short, not a NullPointerException
    // (covers the password == null branch of C4, the mirror of TC-R15 for the date of birth).
    @Test
    void password_null_isRejectedAsTooShort() {
        assertRejected(evaluate("Tigist", "tigist", null, VALID_DOB, false),
                RegistrationRejection.PASSWORD_TOO_SHORT);
    }

    // TC-R11 - Rule R5 (C5=F): a date of birth in the future.
    @Test
    void registration_futureDateOfBirth_rejectsWithInvalidDateOfBirth() {
        assertRejected(evaluate("Tigist", "tigist", "secret", TODAY.plusDays(1), false),
                RegistrationRejection.INVALID_DATE_OF_BIRTH);
    }

    // TC-R12 - BVA on C5 lower edge: born exactly today (age 0) is accepted.
    @Test
    void dateOfBirth_today_isAccepted() {
        assertThat(evaluate("Tigist", "tigist", "secret", TODAY, false).isApproved()).isTrue();
    }

    // TC-R13 - BVA on C5 upper edge: exactly 120 years old is accepted...
    @Test
    void dateOfBirth_exactly120YearsAgo_isAccepted() {
        assertThat(evaluate("Tigist", "tigist", "secret", TODAY.minusYears(120), false).isApproved()).isTrue();
    }

    // TC-R14 - ...but one day older than 120 is rejected.
    @Test
    void dateOfBirth_oneDayOver120Years_isRejected() {
        assertRejected(evaluate("Tigist", "tigist", "secret", TODAY.minusYears(120).minusDays(1), false),
                RegistrationRejection.INVALID_DATE_OF_BIRTH);
    }

    // TC-R15 - null date of birth is treated as invalid, not a NullPointerException.
    @Test
    void dateOfBirth_null_isRejected() {
        assertRejected(evaluate("Tigist", "tigist", "secret", null, false),
                RegistrationRejection.INVALID_DATE_OF_BIRTH);
    }

    // TC-R16 - the sole approve row: every condition satisfied.
    @Test
    void registration_allConditionsMet_approves() {
        RegistrationDecision decision = evaluate("Tigist Alemu", "tigist", "secret", VALID_DOB, false);
        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).isNull();
    }

    private static RegistrationDecision evaluate(String name, String username, String password,
                                                LocalDate dateOfBirth, boolean usernameTaken) {
        return PatientRegistrationPolicy.evaluate(name, username, password, dateOfBirth, TODAY, usernameTaken);
    }

    private void assertRejected(RegistrationDecision decision, RegistrationRejection expected) {
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).isEqualTo(expected);
    }
}
