package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Slot;

public record SlotResponse(Long id, String startTime, DoctorResponse doctor) {

    public static SlotResponse from(Slot slot) {
        DoctorResponse doctorResponse = slot.getDoctor() == null ? null : DoctorResponse.from(slot.getDoctor());
        return new SlotResponse(slot.getId(), slot.getStartTime().toString(), doctorResponse);
    }
}
