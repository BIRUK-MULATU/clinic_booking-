package et.aau.clinic.repository;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsBySlotAndStatusIn(Slot slot, Collection<AppointmentStatus> statuses);

    List<Appointment> findByPatientOrderByRequestedAtDesc(Patient patient);

    // "Next" waitlisted patient for a slot, FIFO by requestedAt - used to promote on cancellation.
    Optional<Appointment> findFirstBySlotAndStatusOrderByRequestedAtAsc(Slot slot, AppointmentStatus status);

    // The reception day-roster: every appointment whose slot falls within a day, earliest first.
    List<Appointment> findBySlot_StartTimeBetweenOrderBySlot_StartTimeAsc(LocalDateTime from, LocalDateTime to);
}
