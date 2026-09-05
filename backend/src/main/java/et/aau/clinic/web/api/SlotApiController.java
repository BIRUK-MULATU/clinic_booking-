package et.aau.clinic.web.api;

import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.DoctorRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.NewSlotRequest;
import et.aau.clinic.web.api.dto.SlotResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
public class SlotApiController {

    private final AppointmentService appointmentService;
    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final Clock clock;

    public SlotApiController(AppointmentService appointmentService, SlotRepository slotRepository,
                             DoctorRepository doctorRepository, Clock clock) {
        this.appointmentService = appointmentService;
        this.slotRepository = slotRepository;
        this.doctorRepository = doctorRepository;
        this.clock = clock;
    }

    // What a patient sees: future slots that are still free to book.
    @GetMapping("/api/slots")
    public ResponseEntity<?> listSlots(HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(appointmentService.listAvailableSlots().stream()
                .map(SlotResponse::from)
                .toList());
    }

    @GetMapping("/api/slots/{id}")
    public ResponseEntity<?> getSlot(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(SlotResponse.from(appointmentService.getSlot(id)));
    }

    // What reception sees: every future slot, taken or not, for slot management.
    @GetMapping("/api/admin/slots")
    public ResponseEntity<?> listUpcomingSlots(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(appointmentService.listUpcomingSlots().stream()
                .map(slot -> SlotResponse.from(slot, appointmentService.slotIsTaken(slot)))
                .toList());
    }

    // Reception adds a one-off slot: pick a doctor (optional) and a future date/time.
    @PostMapping("/api/slots")
    public ResponseEntity<?> addSlot(@RequestBody NewSlotRequest request, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }

        LocalDateTime startTime;
        try {
            startTime = LocalDateTime.parse(request.startTime());
        } catch (DateTimeParseException | NullPointerException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Start time must be a valid date and time."));
        }
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("A slot must be in the future."));
        }

        Doctor doctor = null;
        if (request.doctorId() != null) {
            doctor = doctorRepository.findById(request.doctorId()).orElse(null);
            if (doctor == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Unknown doctor."));
            }
            boolean clash = slotRepository.findByDoctorAndStartTimeBetween(doctor, startTime, startTime.plusSeconds(1))
                    .stream().anyMatch(s -> s.getStartTime().equals(startTime));
            if (clash) {
                return ResponseEntity.badRequest().body(
                        new ErrorResponse("That doctor already has a slot at that time."));
            }
        }

        Slot saved = slotRepository.save(new Slot(startTime, doctor));
        return ResponseEntity.status(201).body(SlotResponse.from(saved));
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
