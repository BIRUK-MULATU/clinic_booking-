package et.aau.clinic.web.api.dto;

/**
 * The admin's "create a patient account" form. dateOfBirth is an ISO
 * date string (yyyy-MM-dd) straight from an <input type="date">.
 */
public record NewPatientRequest(String name, String dateOfBirth, String phone, String username, String password) {
}
