package et.aau.clinic.service;

import et.aau.clinic.domain.Department;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.AvailabilityExceptionRepository;
import et.aau.clinic.repository.DepartmentRepository;
import et.aau.clinic.repository.DoctorAvailabilityRepository;
import et.aau.clinic.repository.DoctorRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hospital-expansion: the update and delete half of the admin's CRUD
 * over doctors, departments and patients - the create/read half already
 * lives in the API controllers. Deletes are guarded, not cascading into
 * appointments: a row that a patient's history depends on is kept, and
 * the caller is told why. Deleting a doctor does tidy up that doctor's
 * own scaffolding (availability rules, exceptions and unbooked slots),
 * since none of that is anyone's history.
 */
@Service
public class DirectoryService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final Clock clock;

    public DirectoryService(DoctorRepository doctorRepository, DepartmentRepository departmentRepository,
                            PatientRepository patientRepository, SlotRepository slotRepository,
                            AppointmentRepository appointmentRepository,
                            DoctorAvailabilityRepository availabilityRepository,
                            AvailabilityExceptionRepository exceptionRepository, Clock clock) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.exceptionRepository = exceptionRepository;
        this.clock = clock;
    }

    // --- doctors ---

    public Doctor updateDoctor(Long id, String name, String specialty, Long departmentId) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        if (isBlank(name) || isBlank(specialty)) {
            throw new IllegalArgumentException("A doctor needs a name and a specialty.");
        }
        doctor.setName(name.trim());
        doctor.setSpecialty(specialty.trim());
        if (departmentId != null) {
            doctor.setDepartment(departmentRepository.findById(departmentId).orElseThrow());
        }
        return doctorRepository.save(doctor);
    }

    @Transactional
    public void deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        if (appointmentRepository.existsBySlot_Doctor(doctor)) {
            throw new IllegalStateException(
                    "This doctor has appointments booked against their slots - cancel those first.");
        }
        availabilityRepository.findByDoctor(doctor).forEach(availabilityRepository::delete);
        exceptionRepository.findByDoctor(doctor).forEach(exceptionRepository::delete);
        slotRepository.findByDoctor(doctor).forEach(slotRepository::delete);
        doctorRepository.delete(doctor);
    }

    // --- departments ---

    public Department updateDepartment(Long id, String name) {
        Department department = departmentRepository.findById(id).orElseThrow();
        if (isBlank(name)) {
            throw new IllegalArgumentException("A department needs a name.");
        }
        department.setName(name.trim());
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id).orElseThrow();
        if (!doctorRepository.findByDepartment(department).isEmpty()) {
            throw new IllegalStateException("This department still has doctors - move or remove them first.");
        }
        departmentRepository.delete(department);
    }

    // --- patients ---

    public Patient updatePatient(Long id, String name, LocalDate dateOfBirth, String phone, String newPassword) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        if (isBlank(name)) {
            throw new IllegalArgumentException("A patient needs a name.");
        }
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now(clock))
                || dateOfBirth.isBefore(LocalDate.now(clock).minusYears(120))) {
            throw new IllegalArgumentException("Enter a valid date of birth (in the past, age 120 or under).");
        }
        // Same 4-char minimum as Rule F (PatientRegistrationPolicy) enforces on new accounts.
        if (newPassword != null && !newPassword.isEmpty() && newPassword.length() < 4) {
            throw new IllegalArgumentException("The password must be at least 4 characters.");
        }
        patient.setName(name.trim());
        patient.setDateOfBirth(dateOfBirth);
        patient.setPhone(phone == null ? "" : phone.trim());
        if (newPassword != null && !newPassword.isEmpty()) {
            patient.setPassword(newPassword);
        }
        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        if (appointmentRepository.existsByPatient(patient)) {
            throw new IllegalStateException(
                    "This patient has appointments on record - their history can't be deleted.");
        }
        patientRepository.delete(patient);
    }

    // --- slots ---

    public Slot updateSlot(Long id, Long doctorId, LocalDateTime startTime) {
        Slot slot = slotRepository.findById(id).orElseThrow();
        if (appointmentRepository.existsBySlot(slot)) {
            throw new IllegalStateException("This slot has an appointment against it - it can't be changed.");
        }
        Doctor doctor = doctorId == null ? null : doctorRepository.findById(doctorId).orElseThrow();
        if (doctor != null) {
            boolean clash = slotRepository.findByDoctorAndStartTimeBetween(doctor, startTime, startTime.plusSeconds(1))
                    .stream().anyMatch(s -> !s.getId().equals(id) && s.getStartTime().equals(startTime));
            if (clash) {
                throw new IllegalStateException("That doctor already has a slot at that time.");
            }
        }
        slot.setStartTime(startTime);
        slot.setDoctor(doctor);
        return slotRepository.save(slot);
    }

    public void deleteSlot(Long id) {
        Slot slot = slotRepository.findById(id).orElseThrow();
        if (appointmentRepository.existsBySlot(slot)) {
            throw new IllegalStateException("This slot has an appointment against it - cancel the appointment first.");
        }
        slotRepository.delete(slot);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
