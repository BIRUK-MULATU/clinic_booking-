package et.aau.clinic.repository;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsBySlotAndStatusIn(Slot slot, Collection<AppointmentStatus> statuses);

    List<Appointment> findByPatientOrderByRequestedAtDesc(Patient patient);
}
