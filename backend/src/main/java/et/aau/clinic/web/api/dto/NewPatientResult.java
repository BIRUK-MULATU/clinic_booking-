package et.aau.clinic.web.api.dto;

/**
 * The JSON shape for a "create patient account" attempt, mirroring
 * BookingResponse: created=true carries the new account; created=false
 * carries the RegistrationRejection reason name.
 */
public record NewPatientResult(boolean created, String reason, PatientAccountResponse patient) {
}
