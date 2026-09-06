package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.QueueEntry;

public record QueueEntryResponse(Long id, Long appointmentId, String patientName, String slotStartTime,
                                  String status, String checkedInAt) {

    public static QueueEntryResponse from(QueueEntry entry) {
        return new QueueEntryResponse(
                entry.getId(),
                entry.getAppointment().getId(),
                entry.getAppointment().getPatient().getName(),
                entry.getAppointment().getSlot().getStartTime().toString(),
                entry.getStatus().name(),
                entry.getCheckedInAt().toString());
    }
}
