package et.aau.clinic.service;

import et.aau.clinic.core.VisitRecordDecision;
import et.aau.clinic.core.VisitRecordPolicy;
import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.VisitRecord;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.VisitRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Hospital-expansion Phase E: orchestrates Rule E against the
 * repositories. It turns the two booleans VisitRecordPolicy needs into
 * repository lookups (is the appointment ATTENDED, does a record already
 * exist), and is the one place that reads the Clock so core/ never has
 * to - the same division of labour as AppointmentService.
 */
@Service
public class VisitRecordService {

    private final AppointmentRepository appointmentRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final Clock clock;

    public VisitRecordService(AppointmentRepository appointmentRepository,
                              VisitRecordRepository visitRecordRepository, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.clock = clock;
    }

    public VisitRecordOutcome record(Long appointmentId, String diagnosis, String notes, String prescription) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();

        boolean attended = appointment.getStatus() == AppointmentStatus.ATTENDED;
        boolean noExistingRecord = !visitRecordRepository.existsByAppointment(appointment);

        VisitRecordDecision decision = VisitRecordPolicy.evaluate(attended, noExistingRecord, diagnosis);
        if (!decision.isApproved()) {
            return new VisitRecordOutcome(decision, null);
        }

        VisitRecord saved = visitRecordRepository.save(new VisitRecord(
                appointment, diagnosis.trim(), blankToNull(notes), blankToNull(prescription),
                LocalDateTime.now(clock)));
        return new VisitRecordOutcome(decision, saved);
    }

    public Optional<VisitRecord> findForAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        return visitRecordRepository.findByAppointment(appointment);
    }

    // Notes and prescription are optional: an empty or whitespace-only field is stored as
    // null rather than "", so "no prescription given" reads the same everywhere.
    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
