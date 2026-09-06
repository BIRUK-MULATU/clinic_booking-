package et.aau.clinic.service;

import et.aau.clinic.core.QueueEvent;
import et.aau.clinic.core.QueueStateMachine;
import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.QueueEntry;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.QueueEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Orchestrates hospital-expansion Phase D's day-of-visit queue. Only
 * checkIn() and completeConsultation() touch anything outside
 * QueueEntry's own lifecycle: checkIn() reads the Appointment being
 * checked in against, and completeConsultation() is the seam that
 * synchronises QueueEntry's completion with AppointmentService's
 * existing markAttended() - see that method's own comment for why it
 * is @Transactional when nothing else in this codebase's service layer
 * is.
 */
@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final Clock clock;

    public QueueService(QueueEntryRepository queueEntryRepository, AppointmentRepository appointmentRepository,
                         AppointmentService appointmentService, Clock clock) {
        this.queueEntryRepository = queueEntryRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
        this.clock = clock;
    }

    public QueueEntry checkIn(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only a CONFIRMED appointment can check in, was " + appointment.getStatus());
        }
        return queueEntryRepository.save(new QueueEntry(appointment, LocalDateTime.now(clock)));
    }

    public QueueEntry call(Long queueEntryId) {
        return applyEvent(queueEntryId, QueueEvent.CALL);
    }

    public QueueEntry startConsultation(Long queueEntryId) {
        return applyEvent(queueEntryId, QueueEvent.START_CONSULTATION);
    }

    /**
     * The synchronisation seam: drives QueueEntry to DONE and, in the
     * same transaction, drives the linked Appointment from CONFIRMED to
     * ATTENDED via the AppointmentService method that already owns that
     * transition. @Transactional here (the first in this codebase's
     * service layer) because this seam's failure mode is not
     * self-correcting the way AvailabilityService's is: if the queue
     * entry reached DONE but markAttended() failed (e.g. the
     * appointment was concurrently cancelled), nothing would ever
     * revisit either row, leaving a permanent, silent mismatch. Wrapping
     * both saves in one transaction means either both happen or
     * neither does - a thrown exception here rolls back the QueueEntry
     * save too, leaving the database exactly as it was before the call.
     */
    @Transactional
    public QueueEntry completeConsultation(Long queueEntryId) {
        QueueEntry entry = applyEvent(queueEntryId, QueueEvent.COMPLETE);
        appointmentService.markAttended(entry.getAppointment().getId());
        return entry;
    }

    private QueueEntry applyEvent(Long queueEntryId, QueueEvent event) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId).orElseThrow();
        entry.setStatus(QueueStateMachine.transition(entry.getStatus(), event));
        return queueEntryRepository.save(entry);
    }
}
