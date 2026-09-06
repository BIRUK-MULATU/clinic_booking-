package et.aau.clinic.domain;

/**
 * Why PatientRegistrationPolicy refused to create a patient account
 * (hospital-expansion: admin-created accounts). Carried on the result
 * object rather than thrown, matching RejectionReason and VisitRejection.
 */
public enum RegistrationRejection {
    NAME_REQUIRED,
    USERNAME_REQUIRED,
    USERNAME_TAKEN,
    PASSWORD_TOO_SHORT,
    INVALID_DATE_OF_BIRTH
}
