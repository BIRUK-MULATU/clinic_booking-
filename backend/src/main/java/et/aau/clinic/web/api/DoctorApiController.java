package et.aau.clinic.web.api;

import et.aau.clinic.domain.Department;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.repository.AvailabilityExceptionRepository;
import et.aau.clinic.repository.DepartmentRepository;
import et.aau.clinic.repository.DoctorAvailabilityRepository;
import et.aau.clinic.repository.DoctorRepository;
import et.aau.clinic.service.AvailabilityService;
import et.aau.clinic.web.api.dto.AvailabilityRuleRequest;
import et.aau.clinic.web.api.dto.AvailabilityRuleResponse;
import et.aau.clinic.web.api.dto.DepartmentRequest;
import et.aau.clinic.web.api.dto.DepartmentResponse;
import et.aau.clinic.web.api.dto.DoctorRequest;
import et.aau.clinic.web.api.dto.DoctorResponse;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.ExceptionDateRequest;
import et.aau.clinic.web.api.dto.ExceptionDateResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hospital-expansion Phases A/B, surfaced through the JSON API for the
 * decorated frontend. Every endpoint here is reception-only (Role.ADMIN)
 * now that the app has a role split - managing doctors, departments and
 * availability is a clinic-administration job, not a patient one.
 */
@RestController
public class DoctorApiController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final AvailabilityService availabilityService;

    public DoctorApiController(DoctorRepository doctorRepository,
                                DepartmentRepository departmentRepository,
                                DoctorAvailabilityRepository availabilityRepository,
                                AvailabilityExceptionRepository exceptionRepository,
                                AvailabilityService availabilityService) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.exceptionRepository = exceptionRepository;
        this.availabilityService = availabilityService;
    }

    @GetMapping("/api/doctors")
    public ResponseEntity<?> listDoctors(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(doctorRepository.findAll().stream().map(DoctorResponse::from).toList());
    }

    @PostMapping("/api/doctors")
    public ResponseEntity<?> addDoctor(@RequestBody DoctorRequest request, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        Department department = departmentRepository.findById(request.departmentId()).orElseThrow();
        Doctor saved = doctorRepository.save(new Doctor(request.name(), request.specialty(), department));
        return ResponseEntity.status(201).body(DoctorResponse.from(saved));
    }

    @GetMapping("/api/departments")
    public ResponseEntity<?> listDepartments(HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(departmentRepository.findAll().stream().map(DepartmentResponse::from).toList());
    }

    @PostMapping("/api/departments")
    public ResponseEntity<?> addDepartment(@RequestBody DepartmentRequest request, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        Department saved = departmentRepository.save(new Department(request.name()));
        return ResponseEntity.status(201).body(DepartmentResponse.from(saved));
    }

    @GetMapping("/api/doctors/{id}/availability")
    public ResponseEntity<?> listAvailability(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(availabilityRepository.findByDoctor(doctor).stream()
                .map(AvailabilityRuleResponse::from)
                .toList());
    }

    @PostMapping("/api/doctors/{id}/availability")
    public ResponseEntity<?> addAvailability(@PathVariable Long id, @RequestBody AvailabilityRuleRequest request,
                                              HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        var saved = availabilityService.addRule(doctor, request.dayOfWeek(), request.startTime(),
                request.endTime(), request.slotDurationMinutes());
        return ResponseEntity.status(201).body(AvailabilityRuleResponse.from(saved));
    }

    @GetMapping("/api/doctors/{id}/exceptions")
    public ResponseEntity<?> listExceptions(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(exceptionRepository.findByDoctor(doctor).stream()
                .map(ExceptionDateResponse::from)
                .toList());
    }

    @PostMapping("/api/doctors/{id}/exceptions")
    public ResponseEntity<?> addException(@PathVariable Long id, @RequestBody ExceptionDateRequest request,
                                           HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        var saved = availabilityService.addException(doctor, request.date());
        return ResponseEntity.status(201).body(ExceptionDateResponse.from(saved));
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
