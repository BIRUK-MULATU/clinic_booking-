package et.aau.clinic.repository;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    // Backs C2 in VisitRecordPolicy - one visit record per appointment.
    boolean existsByAppointment(Appointment appointment);

    Optional<VisitRecord> findByAppointment(Appointment appointment);
}
