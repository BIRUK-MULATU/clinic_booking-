package et.aau.clinic.service;

import et.aau.clinic.core.AvailabilityExpander;
import et.aau.clinic.core.SlotReconciliation;
import et.aau.clinic.core.SlotReconciler;
import et.aau.clinic.core.WeeklyAvailabilityRule;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.AvailabilityException;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.DoctorAvailability;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.AvailabilityExceptionRepository;
import et.aau.clinic.repository.DoctorAvailabilityRepository;
import et.aau.clinic.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates hospital-expansion Phase B: maps DoctorAvailability/
 * AvailabilityException entities into the plain value types
 * AvailabilityExpander needs, calls the two core/ pure functions, and
 * persists the resulting diff. This is the only class in Phase B that
 * touches JPA - core/ stays exactly as container-free as BookingPolicy.
 *
 * Regeneration is triggered synchronously from addRule/addException,
 * not a @Scheduled job - same minimalism principle that kept Spring
 * Security out of the login flow. No @Transactional here, consistent
 * with AppointmentService's existing style (no explicit transaction
 * demarcation on its own multi-step methods either).
 */
@Service
public class AvailabilityService {

    // Duplicated from AppointmentService rather than shared, deliberately -
    // extracting a shared constant would mean touching AppointmentService,
    // an already-tested file, for a two-line saving.
    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED, AppointmentStatus.ATTENDED);

    private final DoctorAvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public AvailabilityService(DoctorAvailabilityRepository availabilityRepository,
                                AvailabilityExceptionRepository exceptionRepository,
                                SlotRepository slotRepository,
                                AppointmentRepository appointmentRepository,
                                Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.exceptionRepository = exceptionRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    public DoctorAvailability addRule(Doctor doctor, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                       int slotDurationMinutes) {
        DoctorAvailability saved = availabilityRepository.save(
                new DoctorAvailability(doctor, dayOfWeek, startTime, endTime, slotDurationMinutes));
        regenerateSlots(doctor);
        return saved;
    }

    public AvailabilityException addException(Doctor doctor, LocalDate date) {
        AvailabilityException saved = exceptionRepository.save(new AvailabilityException(doctor, date));
        regenerateSlots(doctor);
        return saved;
    }

    public void regenerateSlots(Doctor doctor) {
        List<WeeklyAvailabilityRule> rules = availabilityRepository.findByDoctor(doctor).stream()
                .map(a -> new WeeklyAvailabilityRule(a.getDayOfWeek(), a.getStartTime(), a.getEndTime(),
                        a.getSlotDurationMinutes()))
                .toList();
        Set<LocalDate> exceptionDates = exceptionRepository.findByDoctor(doctor).stream()
                .map(AvailabilityException::getDate)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(clock);
        LocalDateTime windowStart = today.atStartOfDay();
        LocalDateTime windowEnd = today.plusDays(AvailabilityExpander.HORIZON_DAYS).atStartOfDay();

        Set<LocalDateTime> candidates = Set.copyOf(AvailabilityExpander.expand(rules, exceptionDates, today));

        List<Slot> existingSlots = slotRepository.findByDoctorAndStartTimeBetween(doctor, windowStart, windowEnd);
        Map<LocalDateTime, Slot> existingByTime = existingSlots.stream()
                .collect(Collectors.toMap(Slot::getStartTime, slot -> slot));
        Set<LocalDateTime> bookedTimes = existingSlots.stream()
                .filter(slot -> appointmentRepository.existsBySlotAndStatusIn(slot, ACTIVE_STATUSES))
                .map(Slot::getStartTime)
                .collect(Collectors.toSet());

        SlotReconciliation reconciliation = SlotReconciler.reconcile(candidates, existingByTime.keySet(), bookedTimes);

        for (LocalDateTime time : reconciliation.toCreate()) {
            slotRepository.save(new Slot(time, doctor));
        }
        for (LocalDateTime time : reconciliation.toRemove()) {
            slotRepository.delete(existingByTime.get(time));
        }
    }
}
