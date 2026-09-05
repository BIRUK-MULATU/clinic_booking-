package et.aau.clinic.web.api.dto;

public record BookingResponse(boolean approved, String reason, AppointmentResponse appointment) {
}
