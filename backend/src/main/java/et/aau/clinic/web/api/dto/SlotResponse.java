package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Slot;

public record SlotResponse(Long id, String startTime) {

    public static SlotResponse from(Slot slot) {
        return new SlotResponse(slot.getId(), slot.getStartTime().toString());
    }
}
