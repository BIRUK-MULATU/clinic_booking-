package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Slot;

/**
 * A slot for the frontend. booked is meaningful only in the admin's
 * every-slot list (/api/admin/slots); the patient's /api/slots only
 * ever returns free slots, so there it is always false.
 */
public record SlotResponse(Long id, String startTime, DoctorResponse doctor, boolean booked) {

    public static SlotResponse from(Slot slot) {
        return from(slot, false);
    }

    public static SlotResponse from(Slot slot, boolean booked) {
        DoctorResponse doctorResponse = slot.getDoctor() == null ? null : DoctorResponse.from(slot.getDoctor());
        return new SlotResponse(slot.getId(), slot.getStartTime().toString(), doctorResponse, booked);
    }
}
