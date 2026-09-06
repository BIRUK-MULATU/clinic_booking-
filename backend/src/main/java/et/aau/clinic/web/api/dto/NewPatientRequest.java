package et.aau.clinic.web.api.dto;

/**
 * The admin's "create / edit a patient account" form. dateOfBirth is an
 * ISO date string (yyyy-MM-dd) straight from an <input type="date">.
 * coveragePercent is the insurance percentage (Rule I); null means
 * "not supplied" - treated as 0 on create and "leave unchanged" on edit.
 */
public record NewPatientRequest(String name, String dateOfBirth, String phone, String username, String password,
                                Integer coveragePercent) {
}
