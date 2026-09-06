package et.aau.clinic.repository;

import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctor(Doctor doctor);
}
