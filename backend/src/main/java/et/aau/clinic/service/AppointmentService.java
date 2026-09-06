package et.aau.clinic.service;

import et.aau.clinic.core.AppointmentEvent;
import et.aau.clinic.core.AppointmentStateMachine;
import et.aau.clinic.core.BookingDecision;
import et.aau.clinic.core.BookingPolicy;
import et.aau.clinic.core.CoverageCalculator;
import et.aau.clinic.core.Fee;
import et.aau.clinic.core.FeeCalculator;
import et.aau.clinic.core.ReminderDecision;
import et.aau.clinic.core.ReminderPolicy;
import et.aau.clinic.core.SuspensionDecision;
import et.aau.clinic.core.SuspensionPolicy;
import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.RejectionReason;
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

    /**
     * Every future slot, taken or not - the admin's slot-management view, as opposed to
     * listAvailableSlots() which is what a patient sees when choosing one to book.
     */
    public List<Slot> listUpcomingSlots() {
        LocalDateTime now = LocalDateTime.now(clock);
        return slotRepository.findAllByOrderByStartTimeAsc().stream()
                .filter(slot -> slot.getStartTime().isAfter(now))
                .toList();
    }

    public boolean slotIsTaken(Slot slot) {
        return appointmentRepository.existsBySlotAndStatusIn(slot, ACTIVE_STATUSES);
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
        LocalDateTime now = LocalDateTime.now(clock);

        // Rule H (highest-priority gate): a patient with 3+ recent no-shows cannot self-book,
        // regardless of the slot, their balance or the notice given. Reception's bookForPatient()
        // deliberately skips this, exactly as it skips C2/C3.
        if (suspensionFor(patient, now).isSuspended()) {
            return new BookingOutcome(BookingDecision.reject(RejectionReason.SUSPENDED_NO_SHOWS), null);
        }

        boolean slotFree = !appointmentRepository.existsBySlotAndStatusIn(slot, ACTIVE_STATUSES);
        boolean noOutstandingBalance = patient.getOutstandingBalance().signum() <= 0;

        BookingDecision decision = BookingPolicy.evaluate(slotFree, noOutstandingBalance, now, slot.getStartTime());
        if (!decision.isApproved()) {
            return new BookingOutcome(decision, null);
        }

        Appointment appointment = newAppointment(patient, slot, AppointmentStatus.REQUESTED, now);
        Appointment saved = appointmentRepository.save(appointment);
        return new BookingOutcome(decision, saved);
    }

    /**
     * Hospital-expansion: reception books an appointment directly onto a patient, rather
     * than the patient requesting it themselves. Only C1 (the slot is still free) is
     * enforced - reception is deliberately allowed to override C2 (outstanding balance)
     * and C3 (2-hour notice), which exist to gate self-service booking, not a front-desk
     * booking made in person. The appointment is created straight in CONFIRMED (reception
     * has no reason to confirm its own booking) and the confirmation SMS is sent, exactly
     * as confirm() would.
     */
    public BookingOutcome bookForPatient(Long patientId, Long slotId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        Slot slot = slotRepository.findById(slotId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);

        if (slotIsTaken(slot)) {
            return new BookingOutcome(BookingDecision.reject(RejectionReason.SLOT_UNAVAILABLE), null);
        }

        Appointment appointment = newAppointment(patient, slot, AppointmentStatus.CONFIRMED, now);
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.sendConfirmation(saved.getPatient(), saved);
        return new BookingOutcome(BookingDecision.approve(), saved);
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

        Appointment appointment = newAppointment(patient, slot, AppointmentStatus.WAITLISTED, now);
        return appointmentRepository.save(appointment);
    }

    /**
     * Rule H: the patient's current no-show suspension status. Exposed so the UI can warn
     * a patient ("2 of 3 no-shows") before they even pick a slot.
     */
    public SuspensionDecision suspensionFor(Long patientId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        return suspensionFor(patient, LocalDateTime.now(clock));
    }

    private SuspensionDecision suspensionFor(Patient patient, LocalDateTime now) {
        List<LocalDateTime> noShowTimes = appointmentRepository
                .findByPatientAndStatus(patient, AppointmentStatus.NO_SHOW).stream()
                .map(appointment -> appointment.getSlot().getStartTime())
                .toList();
        return SuspensionPolicy.evaluate(noShowTimes, now);
    }

    /**
     * Builds a fresh appointment with its fee (Rule 1) and net payable after insurance
     * (Rule G) both captured now, so neither a later birthday nor a later change to the
     * patient's coverage rewrites this row. Shared by every creation path.
     */
    private Appointment newAppointment(Patient patient, Slot slot, AppointmentStatus status, LocalDateTime now) {
        int age = Period.between(patient.getDateOfBirth(), now.toLocalDate()).getYears();
        Fee fee = FeeCalculator.calculate(age);
        Appointment appointment = new Appointment(patient, slot, status, fee.category(), fee.amount(), now);
        appointment.setNetPayable(CoverageCalculator.netPayable(fee.amount(), patient.getCoveragePercent()));
        return appointment;
    }

    /**
     * Hospital-expansion Rule F: the scheduled job's entry point. Pulls every
     * CONFIRMED appointment that has not been reminded yet, asks ReminderPolicy
     * (against the injected Clock) which ones are now within the 24-hour window,
     * and for each of those sends one reminder SMS and stamps reminderSentAt so
     * the next run skips it. Returns how many were sent - handy for the job's
     * log line and for asserting in tests.
     */
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (Appointment appointment :
                appointmentRepository.findByStatusAndReminderSentAtIsNull(AppointmentStatus.CONFIRMED)) {
            if (attemptReminder(appointment, now).isDue()) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * Hospital-expansion Rule F: reception presses "send reminder" next to one
     * appointment. Runs exactly the same ReminderPolicy check as the scheduled
     * sweep - so a slot still more than 24h away, or one already reminded, comes
     * back with that skip reason rather than sending - and returns the decision so
     * the admin UI can show either "sent" or why not.
     */
    public ReminderDecision remind(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        return attemptReminder(appointment, LocalDateTime.now(clock));
    }

    /**
     * The CONFIRMED appointments whose slot is within the next 24 hours - reception's
     * "reminders due" list. Ordered soonest-first; already-reminded ones stay in the
     * list (their reminderSentAt is non-null) so the admin can see they are done.
     */
    public List<Appointment> listUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        return appointmentRepository.findByStatusAndSlot_StartTimeBetweenOrderBySlot_StartTimeAsc(
                AppointmentStatus.CONFIRMED, now, now.plusHours(24));
    }

    private ReminderDecision attemptReminder(Appointment appointment, LocalDateTime now) {
        ReminderDecision decision = ReminderPolicy.decide(appointment.getStatus(), now,
                appointment.getSlot().getStartTime(), appointment.getReminderSentAt() != null);
        if (decision.isDue()) {
            notificationService.sendReminder(appointment.getPatient(), appointment);
            appointment.setReminderSentAt(now);
            appointmentRepository.save(appointment);
        }
        return decision;
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
