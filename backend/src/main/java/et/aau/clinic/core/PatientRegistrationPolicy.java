package et.aau.clinic.core;

import et.aau.clinic.domain.RegistrationRejection;

import java.time.LocalDate;

/**
 * Rule F (hospital-expansion: admin-created patient accounts). The clinic
 * creates every patient login itself - there is no self sign-up - so this
 * is the validation the admin's "create account" form runs through,
 * evaluated as a priority-ordered decision table over five conditions,
 * the same shape as BookingPolicy and VisitRecordPolicy.
 *
 *   C1 - name is present
 *   C2 - username is present
 *   C3 - username is not already taken
 *   C4 - password is at least 4 characters   (boundary value target: 3 vs 4)
 *   C5 - date of birth is in the past and implies an age of 120 or less
 *        (boundary value targets: today vs tomorrow; exactly 120y vs 120y+1d)
 *
 * Pure: the caller passes in "today" and whether the username is taken
 * (a repository lookup), so there is no Clock or database dependency here.
 * The 0-120 age range is the same one FeeCalculator enforces (Rule 1).
 */
public final class PatientRegistrationPolicy {

    static final int MIN_PASSWORD_LENGTH = 4;
    static final int MAX_AGE_YEARS = 120;

    private PatientRegistrationPolicy() {
    }

    public static RegistrationDecision evaluate(String name, String username, String password,
                                                LocalDate dateOfBirth, LocalDate today, boolean usernameTaken) {
        if (isBlank(name)) {
            return RegistrationDecision.reject(RegistrationRejection.NAME_REQUIRED);
        }
        if (isBlank(username)) {
            return RegistrationDecision.reject(RegistrationRejection.USERNAME_REQUIRED);
        }
        if (usernameTaken) {
            return RegistrationDecision.reject(RegistrationRejection.USERNAME_TAKEN);
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return RegistrationDecision.reject(RegistrationRejection.PASSWORD_TOO_SHORT);
        }
        if (dateOfBirth == null
                || dateOfBirth.isAfter(today)
                || dateOfBirth.isBefore(today.minusYears(MAX_AGE_YEARS))) {
            return RegistrationDecision.reject(RegistrationRejection.INVALID_DATE_OF_BIRTH);
        }
        return RegistrationDecision.approve();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
