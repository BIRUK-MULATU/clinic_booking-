package et.aau.clinic.web.api;

import et.aau.clinic.core.PatientRegistrationPolicy;
import et.aau.clinic.core.RegistrationDecision;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Role;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.service.DirectoryService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.NewPatientRequest;
import et.aau.clinic.web.api.dto.NewPatientResult;
import et.aau.clinic.web.api.dto.PatientAccountResponse;
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
import java.time.format.DateTimeParseException;

/**
 * Hospital-expansion: the clinic creates every patient login itself
 * (there is no self sign-up), so this is reception-only. POST runs the
 * new account through PatientRegistrationPolicy (Rule F); a rejection
 * comes back as 200 created=false with the reason, the same pattern
 * BookingApiController uses for a RejectionReason.
 */
@RestController
public class AdminPatientController {

    private final PatientRepository patientRepository;
    private final DirectoryService directoryService;
    private final Clock clock;

    public AdminPatientController(PatientRepository patientRepository, DirectoryService directoryService, Clock clock) {
        this.patientRepository = patientRepository;
        this.directoryService = directoryService;
        this.clock = clock;
    }

    @GetMapping("/api/admin/patients")
    public ResponseEntity<?> listPatients(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(patientRepository.findByRoleOrderByNameAsc(Role.PATIENT).stream()
                .map(PatientAccountResponse::from)
                .toList());
    }

    @PostMapping("/api/admin/patients")
    public ResponseEntity<?> createPatient(@RequestBody NewPatientRequest request, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = request.dateOfBirth() == null ? null : LocalDate.parse(request.dateOfBirth());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Date of birth must be a valid date."));
        }

        boolean usernameTaken = request.username() != null
                && patientRepository.findByUsername(request.username()).isPresent();
        RegistrationDecision decision = PatientRegistrationPolicy.evaluate(
                request.name(), request.username(), request.password(), dateOfBirth,
                LocalDate.now(clock), usernameTaken);
        if (!decision.isApproved()) {
            return ResponseEntity.ok(new NewPatientResult(false, decision.getReason().name(), null));
        }

        int coverage = request.coveragePercent() == null ? 0 : request.coveragePercent();
        if (coverage < 0 || coverage > 100) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Coverage must be between 0 and 100."));
        }

        Patient patient = new Patient(
                request.name().trim(), dateOfBirth, request.username().trim(), request.password(),
                request.phone() == null ? "" : request.phone().trim());
        patient.setCoveragePercent(coverage);
        Patient saved = patientRepository.save(patient);
        return ResponseEntity.status(201).body(
                new NewPatientResult(true, null, PatientAccountResponse.from(saved)));
    }

    @PutMapping("/api/admin/patients/{id}")
    public ResponseEntity<?> updatePatient(@PathVariable Long id, @RequestBody NewPatientRequest request,
                                           HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        LocalDate dateOfBirth;
        try {
            dateOfBirth = request.dateOfBirth() == null ? null : LocalDate.parse(request.dateOfBirth());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Date of birth must be a valid date."));
        }
        // username is the account's identity and is not changed here; password only if a new one is given.
        Patient updated = directoryService.updatePatient(
                id, request.name(), dateOfBirth, request.phone(), request.password(), request.coveragePercent());
        return ResponseEntity.ok(PatientAccountResponse.from(updated));
    }

    @DeleteMapping("/api/admin/patients/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        directoryService.deletePatient(id);
        return ResponseEntity.noContent().build();
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
