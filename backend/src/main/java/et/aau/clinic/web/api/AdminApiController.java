package et.aau.clinic.web.api;

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
 * Reception's day roster (hospital-expansion): every appointment for a
 * given date, earliest slot first, regardless of whether the patient
 * has checked in yet. Separate from QueueApiController's live queue,
 * which only covers patients who have physically checked in - this
 * shows who is expected today at all, checked in or not.
 */
@RestController
public class AdminApiController {

    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public AdminApiController(AppointmentRepository appointmentRepository, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    @GetMapping("/api/admin/appointments")
    public ResponseEntity<?> appointmentsForDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(403).body(new ErrorResponse("Reception only."));
        }

        LocalDate day = date != null ? date : LocalDate.now(clock);
        return ResponseEntity.ok(appointmentRepository
                .findBySlot_StartTimeBetweenOrderBySlot_StartTimeAsc(day.atStartOfDay(), day.plusDays(1).atStartOfDay())
                .stream()
                .map(AdminAppointmentResponse::from)
                .toList());
    }
}
