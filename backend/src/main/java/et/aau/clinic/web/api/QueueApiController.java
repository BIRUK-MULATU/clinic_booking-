package et.aau.clinic.web.api;

import et.aau.clinic.domain.QueueEntryStatus;
import et.aau.clinic.repository.QueueEntryRepository;
import et.aau.clinic.service.QueueService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.QueueEntryResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;

/** Hospital-expansion Phase D, surfaced through the JSON API. */
@RestController
public class QueueApiController {

    private static final EnumSet<QueueEntryStatus> ACTIVE_QUEUE_STATUSES =
            EnumSet.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.IN_CONSULTATION);

    private final QueueService queueService;
    private final QueueEntryRepository queueEntryRepository;

    public QueueApiController(QueueService queueService, QueueEntryRepository queueEntryRepository) {
        this.queueService = queueService;
        this.queueEntryRepository = queueEntryRepository;
    }

    @PostMapping("/api/appointments/{id}/check-in")
    public ResponseEntity<?> checkIn(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        var entry = queueService.checkIn(id);
        return ResponseEntity.status(201).body(QueueEntryResponse.from(entry));
    }

    // The front-desk view: every entry not yet DONE, oldest check-in first - the same
    // "who's next" ordering QueueEntryRepository already provides. Reception-only: running
    // the queue is a front-desk job, not something a patient does for themselves.
    @GetMapping("/api/queue")
    public ResponseEntity<?> listQueue(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(queueEntryRepository.findByStatusInOrderByCheckedInAtAsc(ACTIVE_QUEUE_STATUSES)
                .stream()
                .map(QueueEntryResponse::from)
                .toList());
    }

    @PostMapping("/api/queue/{id}/call")
    public ResponseEntity<?> call(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(QueueEntryResponse.from(queueService.call(id)));
    }

    @PostMapping("/api/queue/{id}/start-consultation")
    public ResponseEntity<?> startConsultation(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(QueueEntryResponse.from(queueService.startConsultation(id)));
    }

    @PostMapping("/api/queue/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(QueueEntryResponse.from(queueService.completeConsultation(id)));
    }

    private ResponseEntity<ErrorResponse> requireAdmin(HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(403).body(new ErrorResponse("Reception only."));
        }
        return null;
    }
}
