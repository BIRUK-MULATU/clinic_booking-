package et.aau.clinic.web.api.dto;

/**
 * The JSON shape for a "record this visit" attempt, mirroring
 * BookingResponse: recorded=true carries the saved record; recorded=false
 * carries the VisitRejection reason name and a null record.
 */
public record VisitRecordResult(boolean recorded, String reason, VisitRecordResponse record) {
}
