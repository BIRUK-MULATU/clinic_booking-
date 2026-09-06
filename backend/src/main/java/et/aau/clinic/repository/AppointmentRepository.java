package et.aau.clinic.repository;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Doctor;
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

    // Rule H: a patient's past no-shows, used to count the recent ones for the suspension check.
    List<Appointment> findByPatientAndStatus(Patient patient, AppointmentStatus status);

    // "Next" waitlisted patient for a slot, FIFO by requestedAt - used to promote on cancellation.
    Optional<Appointment> findFirstBySlotAndStatusOrderByRequestedAtAsc(Slot slot, AppointmentStatus status);

    // The reception day-roster: every appointment whose slot falls within a day, earliest first.
    List<Appointment> findBySlot_StartTimeBetweenOrderBySlot_StartTimeAsc(LocalDateTime from, LocalDateTime to);

    // Reminder candidates (Rule F): CONFIRMED and not yet reminded. The 24-hour-window and
    // slot-not-started conditions are ReminderPolicy's job, evaluated per row against the Clock.
    List<Appointment> findByStatusAndReminderSentAtIsNull(AppointmentStatus status);

    // Reception's "reminders due" list: CONFIRMED appointments whose slot falls in a time
    // window (now .. now+24h), soonest first. Already-reminded rows are kept in - their
    // reminderSentAt tells the admin they are done.
    List<Appointment> findByStatusAndSlot_StartTimeBetweenOrderBySlot_StartTimeAsc(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    // Reception's "needs my attention" list: every appointment in one status (REQUESTED for the
    // confirm queue), across all dates, soonest slot first.
    List<Appointment> findByStatusOrderBySlot_StartTimeAsc(AppointmentStatus status);

    // How many patients are scheduled with one doctor on one day - feeds Rule G / DoctorLoad.
    long countBySlot_DoctorAndStatusInAndSlot_StartTimeBetween(
            Doctor doctor, Collection<AppointmentStatus> statuses, LocalDateTime from, LocalDateTime to);

    // Guards for the admin CRUD deletes: is anything still pointing at this row?
    boolean existsBySlot(Slot slot);

    boolean existsByPatient(Patient patient);

    boolean existsBySlot_Doctor(Doctor doctor);
}
