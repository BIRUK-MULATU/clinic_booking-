package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.VisitRecord;

public record VisitRecordResponse(Long id, Long appointmentId, String diagnosis, String notes,
                                  String prescription, String recordedAt) {

    public static VisitRecordResponse from(VisitRecord record) {
        return new VisitRecordResponse(
                record.getId(),
                record.getAppointment().getId(),
                record.getDiagnosis(),
                record.getNotes(),
                record.getPrescription(),
                record.getRecordedAt().toString());
    }
}
