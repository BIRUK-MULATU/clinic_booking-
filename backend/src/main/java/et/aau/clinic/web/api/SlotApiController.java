package et.aau.clinic.web.api;

import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.DoctorRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.DirectoryService;
import et.aau.clinic.service.DoctorLoadService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.NewSlotRequest;
import et.aau.clinic.web.api.dto.NewSlotResult;
import et.aau.clinic.web.api.dto.SlotResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SlotApiController {

    private final AppointmentService appointmentService;
    private final DoctorLoadService doctorLoadService;
    private final DirectoryService directoryService;
    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final Clock clock;

    public SlotApiController(AppointmentService appointmentService, DoctorLoadService doctorLoadService,
                             DirectoryService directoryService, SlotRepository slotRepository,
                             DoctorRepository doctorRepository, Clock clock) {
        this.appointmentService = appointmentService;
        this.doctorLoadService = doctorLoadService;
        this.directoryService = directoryService;
        this.slotRepository = slotRepository;
        this.doctorRepository = doctorRepository;
        this.clock = clock;
    }

    // What a patient sees: future slots that are still free to book, each tagged with how
    // full the doctor's day is (so a patient sees a doctor is nearly/fully booked upfront).
    @GetMapping("/api/slots")
    public ResponseEntity<?> listSlots(HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(withDoctorDayStatus(appointmentService.listAvailableSlots(), false));
    }

    @GetMapping("/api/slots/{id}")
    public ResponseEntity<?> getSlot(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(SlotResponse.from(appointmentService.getSlot(id)));
    }

    // What reception sees: every future slot, taken or not, with the doctor's day status.
    @GetMapping("/api/admin/slots")
    public ResponseEntity<?> listUpcomingSlots(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(withDoctorDayStatus(appointmentService.listUpcomingSlots(), true));
    }

    // Reception adds a one-off slot: pick a doctor (optional) and a future date/time. The
    // slot is always created; if the doctor is now over their daily limit for that day,
    // reception is notified via the warning field (not blocked).
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
        String warning = doctor == null ? null
                : doctorLoadService.slotOverLimitWarning(doctor, startTime.toLocalDate()).orElse(null);
        return ResponseEntity.status(201).body(new NewSlotResult(SlotResponse.from(saved), warning));
    }

    @PutMapping("/api/slots/{id}")
    public ResponseEntity<?> updateSlot(@PathVariable Long id, @RequestBody NewSlotRequest request,
                                        HttpSession session) {
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
        Slot updated = directoryService.updateSlot(id, request.doctorId(), startTime);
        String status = updated.getDoctor() == null ? null
                : doctorLoadService.loadOn(updated.getDoctor(), startTime.toLocalDate()).status().name();
        return ResponseEntity.ok(SlotResponse.from(updated, appointmentService.slotIsTaken(updated), status));
    }

    @DeleteMapping("/api/slots/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        directoryService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // Computes each slot's doctorDayStatus, memoising the load per (doctorId, date) so a
    // day with many slots for one doctor is only counted once.
    private List<SlotResponse> withDoctorDayStatus(List<Slot> slots, boolean includeBooked) {
        Map<String, String> statusCache = new HashMap<>();
        return slots.stream().map(slot -> {
            boolean booked = includeBooked && appointmentService.slotIsTaken(slot);
            String status = null;
            if (slot.getDoctor() != null) {
                LocalDate date = slot.getStartTime().toLocalDate();
                status = statusCache.computeIfAbsent(
                        slot.getDoctor().getId() + "@" + date,
                        key -> doctorLoadService.loadOn(slot.getDoctor(), date).status().name());
            }
            return SlotResponse.from(slot, booked, status);
        }).toList();
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
