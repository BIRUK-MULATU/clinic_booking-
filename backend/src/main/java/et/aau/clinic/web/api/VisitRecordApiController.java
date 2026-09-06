package et.aau.clinic.web.api;

import et.aau.clinic.service.VisitRecordOutcome;
import et.aau.clinic.service.VisitRecordService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.VisitRecordRequest;
import et.aau.clinic.web.api.dto.VisitRecordResponse;
import et.aau.clinic.web.api.dto.VisitRecordResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hospital-expansion Phase E, surfaced through the JSON API. Writing a
 * visit record is reception/clinician work, so POST is ADMIN-only - the
 * same guard AppointmentApiController.confirm() uses. Reading one back
 * only needs a logged-in session.
 *
 * A policy rejection (not ATTENDED, already recorded, bad diagnosis)
 * comes back as 200 with recorded=false and the reason name, exactly
 * like BookingApiController returns approved=false - it is an expected
 * business outcome, not an error, so the frontend renders it inline
 * without a catch.
 */
@RestController
public class VisitRecordApiController {

    private final VisitRecordService visitRecordService;

    public VisitRecordApiController(VisitRecordService visitRecordService) {
        this.visitRecordService = visitRecordService;
    }

    @PostMapping("/api/appointments/{id}/visit-record")
    public ResponseEntity<?> record(@PathVariable Long id, @RequestBody VisitRecordRequest request,
                                    HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(403).body(new ErrorResponse("Only reception can record a visit."));
        }

        VisitRecordOutcome outcome = visitRecordService.record(
                id, request.diagnosis(), request.notes(), request.prescription());
        if (!outcome.decision().isApproved()) {
            return ResponseEntity.ok(new VisitRecordResult(false, outcome.decision().getReason().name(), null));
        }
        return ResponseEntity.status(201).body(new VisitRecordResult(
                true, null, VisitRecordResponse.from(outcome.record())));
    }

    @GetMapping("/api/appointments/{id}/visit-record")
    public ResponseEntity<?> get(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return visitRecordService.findForAppointment(id)
                .map(record -> ResponseEntity.ok((Object) VisitRecordResponse.from(record)))
                .orElseGet(() -> ResponseEntity.status(404).body(new ErrorResponse("No visit record yet.")));
    }
}
