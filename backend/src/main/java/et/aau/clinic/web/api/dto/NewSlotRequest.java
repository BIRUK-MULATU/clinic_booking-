package et.aau.clinic.web.api.dto;

/**
 * The admin's "add one slot" form. startTime is a local date-time string
 * (yyyy-MM-ddTHH:mm) from an <input type="datetime-local">. doctorId is
 * optional - a Slot can exist without a doctor, as it always could.
 */
public record NewSlotRequest(Long doctorId, String startTime) {
}
