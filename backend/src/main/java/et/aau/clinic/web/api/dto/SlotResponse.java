package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Slot;

/**
 * A slot for the frontend.
 *
 *   booked          - meaningful only in the admin's every-slot list
 *                     (/api/admin/slots); the patient's /api/slots only
 *                     returns free slots, so there it is always false.
 *   doctorDayStatus - the doctor's load band (AVAILABLE / NEARLY_FULL /
 *                     FULL) for this slot's date, or null when the slot
 *                     has no doctor or the caller did not compute it.
 */
public record SlotResponse(Long id, String startTime, DoctorResponse doctor, boolean booked, String doctorDayStatus) {

    public static SlotResponse from(Slot slot) {
        return from(slot, false, null);
    }

    public static SlotResponse from(Slot slot, boolean booked) {
        return from(slot, booked, null);
    }

    public static SlotResponse from(Slot slot, boolean booked, String doctorDayStatus) {
        DoctorResponse doctorResponse = slot.getDoctor() == null ? null : DoctorResponse.from(slot.getDoctor());
        return new SlotResponse(slot.getId(), slot.getStartTime().toString(), doctorResponse, booked, doctorDayStatus);
    }
}
