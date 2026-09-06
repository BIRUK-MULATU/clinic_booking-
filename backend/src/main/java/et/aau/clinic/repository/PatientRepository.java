package et.aau.clinic.repository;

import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUsername(String username);

    // The admin's patient roster: everyone in one role, by name.
    List<Patient> findByRoleOrderByNameAsc(Role role);
}
