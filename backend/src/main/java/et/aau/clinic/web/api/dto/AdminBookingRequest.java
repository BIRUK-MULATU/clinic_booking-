package et.aau.clinic.web.api.dto;

/** Reception booking an appointment directly onto a patient. */
public record AdminBookingRequest(Long patientId, Long slotId) {
}
