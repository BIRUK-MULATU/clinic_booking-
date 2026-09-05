package et.aau.clinic.repository;

import et.aau.clinic.domain.AvailabilityException;
import et.aau.clinic.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {

    List<AvailabilityException> findByDoctor(Doctor doctor);
}
