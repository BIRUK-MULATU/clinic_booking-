package et.aau.clinic.repository;

import et.aau.clinic.domain.Department;
import et.aau.clinic.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByDepartment(Department department);
}
