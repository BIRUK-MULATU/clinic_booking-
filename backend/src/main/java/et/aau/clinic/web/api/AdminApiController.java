package et.aau.clinic.web.api;

import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.web.api.dto.AdminAppointmentResponse;
import et.aau.clinic.web.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Reception's views onto everyone's appointments (hospital-expansion),
 * both reception-only:
 *
 *   /api/admin/appointments/pending - every REQUESTED appointment, any
 *   date, soonest slot first. This is the "confirm queue": a patient
 *   requests a booking and it sits here until reception confirms it.
 *
 *   /api/admin/appointments?date=... - the day roster: every appointment
 *   whose slot falls on one date, checked in or not. Defaults to today.
 *
 * Both are separate from QueueApiController's live queue, which only
 * covers patients who have physically checked in.
 */
@RestController
public class AdminApiController {

    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public AdminApiController(AppointmentRepository appointmentRepository, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    @GetMapping("/api/admin/appointments/pending")
    public ResponseEntity<?> pendingConfirmation(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(appointmentRepository
                .findByStatusOrderBySlot_StartTimeAsc(AppointmentStatus.REQUESTED)
                .stream()
                .map(AdminAppointmentResponse::from)
                .toList());
    }

    @GetMapping("/api/admin/appointments")
    public ResponseEntity<?> appointmentsForDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }

        LocalDate day = date != null ? date : LocalDate.now(clock);
        return ResponseEntity.ok(appointmentRepository
                .findBySlot_StartTimeBetweenOrderBySlot_StartTimeAsc(day.atStartOfDay(), day.plusDays(1).atStartOfDay())
                .stream()
                .map(AdminAppointmentResponse::from)
                .toList());
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
