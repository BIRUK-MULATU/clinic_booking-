package et.aau.clinic.web.api;

import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import et.aau.clinic.web.api.dto.AdminAppointmentResponse;
import et.aau.clinic.web.api.dto.AdminBookingRequest;
import et.aau.clinic.web.api.dto.AppointmentResponse;
import et.aau.clinic.web.api.dto.BookingResponse;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.ReminderResponse;
import et.aau.clinic.web.api.dto.ReminderSendResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Reception's views onto - and actions on - everyone's appointments
 * (hospital-expansion), all reception-only:
 *
 *   GET  /api/admin/appointments/pending  - every REQUESTED appointment,
 *        any date, soonest slot first: the "confirm queue".
 *   GET  /api/admin/appointments?date=... - the day roster: every
 *        appointment whose slot falls on one date. Defaults to today.
 *   POST /api/admin/appointments          - reception books an
 *        appointment directly onto a patient (straight to CONFIRMED).
 *   GET  /api/admin/appointments/reminders       - CONFIRMED
 *        appointments in the next 24h: the "reminders due" list.
 *   POST /api/admin/appointments/{id}/reminder   - send that one
 *        appointment's 24-hour reminder now (Rule H).
 *   POST /api/admin/appointments/reminders/send-all - run the whole
 *        due-reminder sweep on demand.
 *
 * All separate from QueueApiController's live queue, which only covers
 * patients who have physically checked in.
 */
@RestController
public class AdminApiController {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final Clock clock;

    public AdminApiController(AppointmentRepository appointmentRepository, AppointmentService appointmentService,
                              Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
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

    @PostMapping("/api/admin/appointments")
    public ResponseEntity<?> bookForPatient(@RequestBody AdminBookingRequest request, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        if (request.patientId() == null || request.slotId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Pick both a patient and a slot."));
        }

        BookingOutcome outcome = appointmentService.bookForPatient(request.patientId(), request.slotId());
        if (!outcome.decision().isApproved()) {
            return ResponseEntity.ok(new BookingResponse(false, outcome.decision().getReason().name(), null));
        }
        return ResponseEntity.status(201).body(
                new BookingResponse(true, null, AppointmentResponse.from(outcome.appointment())));
    }

    @GetMapping("/api/admin/appointments/reminders")
    public ResponseEntity<?> remindersDue(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(appointmentService.listUpcomingReminders().stream()
                .map(ReminderResponse::from)
                .toList());
    }

    @PostMapping("/api/admin/appointments/{id}/reminder")
    public ResponseEntity<?> sendReminder(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(ReminderSendResult.from(appointmentService.remind(id)));
    }

    @PostMapping("/api/admin/appointments/reminders/send-all")
    public ResponseEntity<?> sendAllDueReminders(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(Map.of("sent", appointmentService.sendDueReminders()));
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
