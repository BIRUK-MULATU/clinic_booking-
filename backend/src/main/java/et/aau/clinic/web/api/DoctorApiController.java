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
import et.aau.clinic.web.api.dto.DoctorPhotoRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
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

    // A data URL for a lightly-compressed headshot is well under this; the cap only
    // exists to stop someone pasting a multi-megabyte original straight into the DB.
    private static final int MAX_PHOTO_CHARS = 1_400_000;

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
        return ResponseEntity.ok(doctorRepository.findAll().stream()
                .map(doctor -> DoctorResponse.from(doctor, availabilityRepository.findByDoctor(doctor)))
                .toList());
    }

    @PostMapping("/api/doctors")
    public ResponseEntity<?> addDoctor(@RequestBody DoctorRequest request, HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        String photoError = validatePhoto(request.photo());
        if (photoError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(photoError));
        }
        Department department = departmentRepository.findById(request.departmentId()).orElseThrow();
        Doctor doctor = new Doctor(request.name(), request.specialty(), department);
        doctor.setPhoto(emptyToNull(request.photo()));
        Doctor saved = doctorRepository.save(doctor);
        return ResponseEntity.status(201).body(DoctorResponse.from(saved, availabilityRepository.findByDoctor(saved)));
    }

    @PutMapping("/api/doctors/{id}/photo")
    public ResponseEntity<?> setPhoto(@PathVariable Long id, @RequestBody DoctorPhotoRequest request,
                                      HttpSession session) {
        ResponseEntity<ErrorResponse> denied = requireAdmin(session);
        if (denied != null) {
            return denied;
        }
        String photoError = validatePhoto(request.photo());
        if (photoError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(photoError));
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        doctor.setPhoto(emptyToNull(request.photo()));
        Doctor saved = doctorRepository.save(doctor);
        return ResponseEntity.ok(DoctorResponse.from(saved, availabilityRepository.findByDoctor(saved)));
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

    // Returns an error message if the photo is present but unusable, or null if it is
    // absent (fine) or a plausible image data URL within the size cap.
    private static String validatePhoto(String photo) {
        if (photo == null || photo.trim().isEmpty()) {
            return null;
        }
        if (!photo.startsWith("data:image/")) {
            return "Photo must be an image file.";
        }
        if (photo.length() > MAX_PHOTO_CHARS) {
            return "That image is too large - please use a smaller photo.";
        }
        return null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
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
