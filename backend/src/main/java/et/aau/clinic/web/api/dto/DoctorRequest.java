package et.aau.clinic.web.api.dto;

/**
 * photo is an optional data URL uploaded from the admin's device.
 * dailyPatientLimit is optional too - null falls back to the Doctor
 * entity's default.
 */
public record DoctorRequest(String name, String specialty, Long departmentId, String photo,
                            Integer dailyPatientLimit) {
}
