package et.aau.clinic.service;

import et.aau.clinic.core.AppointmentEvent;
import et.aau.clinic.core.AppointmentStateMachine;
import et.aau.clinic.core.BookingDecision;
import et.aau.clinic.core.BookingPolicy;
import et.aau.clinic.core.Fee;
import et.aau.clinic.core.FeeCalculator;
import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates the domain rules against the repositories: turns the
 * booleans BookingPolicy needs into repository lookups, turns
 * AppointmentStateMachine's state changes into saved rows, and is the
 * one place that reads the Clock so core/ never has to.
 */
@Service
public class AppointmentService {

    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED, AppointmentStatus.ATTENDED);

    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;
    private final NotificationService notificationService;

    public AppointmentService(PatientRepository patientRepository, SlotRepository slotRepository,
                               AppointmentRepository appointmentRepository, Clock clock,
                               NotificationService notificationService) {
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
        this.notificationService = notificationService;
    }

    public Optional<Patient> login(String username, String password) {
        return patientRepository.findByUsername(username)
                .filter(patient -> patient.getPassword().equals(password));
    }

    public List<Slot> listAvailableSlots() {
        LocalDateTime now = LocalDateTime.now(clock);
        return slotRepository.findAllByOrderByStartTimeAsc().stream()
                .filter(slot -> slot.getStartTime().isAfter(now))
                .filter(slot -> !appointmentRepository.existsBySlotAndStatusIn(slot, ACTIVE_STATUSES))
                .toList();
    }

    public Slot getSlot(Long slotId) {
        return slotRepository.findById(slotId).orElseThrow();
    }

    public Appointment getAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    public List<Appointment> listAppointmentsForPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        return appointmentRepository.findByPatientOrderByRequestedAtDesc(patient);
    }

    public BookingOutcome requestBooking(Long patientId, Long slotId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        Slot slot = slotRepository.findById(slotId).orElseThrow();

        boolean slotFree = !appointmentRepository.existsBySlotAndStatusIn(slot, ACTIVE_STATUSES);
        boolean noOutstandingBalance = patient.getOutstandingBalance().signum() <= 0;
        LocalDateTime now = LocalDateTime.now(clock);

        BookingDecision decision = BookingPolicy.evaluate(slotFree, noOutstandingBalance, now, slot.getStartTime());
        if (!decision.isApproved()) {
            return new BookingOutcome(decision, null);
        }

        int age = Period.between(patient.getDateOfBirth(), now.toLocalDate()).getYears();
        Fee fee = FeeCalculator.calculate(age);

        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.REQUESTED, fee.category(), fee.amount(), now);
        Appointment saved = appointmentRepository.save(appointment);
        return new BookingOutcome(decision, saved);
    }

    /**
     * Hospital-expansion Phase C: joins the waitlist for a slot that's currently taken.
     * Entered directly in WAITLISTED, the same way requestBooking() creates fresh
     * appointments directly in REQUESTED - neither is a transition, both are the initial
     * state of a new row. No eligibility check here beyond the slot/patient existing:
     * EXPANSION.md doesn't specify one, and BookingPolicy's C1/C2/C3 govern booking an
     * available slot, not queuing for one that's already gone.
     */
    public Appointment joinWaitlist(Long patientId, Long slotId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        Slot slot = slotRepository.findById(slotId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);

        int age = Period.between(patient.getDateOfBirth(), now.toLocalDate()).getYears();
        Fee fee = FeeCalculator.calculate(age);

        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.WAITLISTED, fee.category(), fee.amount(), now);
        return appointmentRepository.save(appointment);
    }

    public Appointment confirm(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        appointment.setStatus(AppointmentStateMachine.transition(appointment.getStatus(), AppointmentEvent.CONFIRM));
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.sendConfirmation(saved.getPatient(), saved);
        return saved;
    }

    public Appointment markAttended(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        appointment.setStatus(AppointmentStateMachine.transition(appointment.getStatus(), AppointmentEvent.ATTEND));
        return appointmentRepository.save(appointment);
    }

    public Appointment markNoShow(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        appointment.setStatus(
                AppointmentStateMachine.transition(appointment.getStatus(), AppointmentEvent.MARK_NO_SHOW));
        return appointmentRepository.save(appointment);
    }

    public Appointment cancel(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        AppointmentStatus previousStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStateMachine.transition(previousStatus, AppointmentEvent.CANCEL));

        if (previousStatus == AppointmentStatus.CONFIRMED) {
            LocalDateTime now = LocalDateTime.now(clock);
            BigDecimal fee = AppointmentStateMachine.lateCancellationFee(
                    now, appointment.getSlot().getStartTime(), appointment.getFeeAmount());
            appointment.setCancellationFee(fee);

            promoteNextWaitlisted(appointment.getSlot());
        }

        return appointmentRepository.save(appointment);
    }

    /**
     * Hospital-expansion Phase C, now wired: when a CONFIRMED appointment is cancelled,
     * the slot it held frees up, so the longest-waiting WAITLISTED appointment for that
     * same slot (if any) is promoted to REQUESTED via the transition that already exists
     * for exactly this purpose.
     */
    private void promoteNextWaitlisted(Slot slot) {
        appointmentRepository.findFirstBySlotAndStatusOrderByRequestedAtAsc(slot, AppointmentStatus.WAITLISTED)
                .ifPresent(waitlisted -> {
                    waitlisted.setStatus(
                            AppointmentStateMachine.transition(AppointmentStatus.WAITLISTED, AppointmentEvent.PROMOTE));
                    appointmentRepository.save(waitlisted);
                });
    }
}
