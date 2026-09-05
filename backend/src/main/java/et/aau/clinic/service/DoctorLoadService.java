package et.aau.clinic.service;

import et.aau.clinic.core.DoctorLoad;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Hospital-expansion: doctor capacity. Turns "how many patients are
 * with this doctor on this day" into a repository count and hands it to
 * Rule G (DoctorLoad) - the same core/ vs service split as everywhere
 * else. "Scheduled" means an active appointment (REQUESTED, CONFIRMED or
 * ATTENDED); cancelled and no-show appointments free the place back up.
 */
@Service
public class DoctorLoadService {

    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED, AppointmentStatus.ATTENDED);

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;

    public DoctorLoadService(AppointmentRepository appointmentRepository, SlotRepository slotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
    }

    /** The doctor's load for one day: booked patients vs their daily limit. */
    public DoctorLoad loadOn(Doctor doctor, LocalDate date) {
        long scheduled = appointmentRepository.countBySlot_DoctorAndStatusInAndSlot_StartTimeBetween(
                doctor, ACTIVE_STATUSES, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        return DoctorLoad.evaluate(doctor.getDailyPatientLimit(), (int) scheduled);
    }

    /** How many slots this doctor has on one day - capacity provisioned, booked or not. */
    public int slotCountOn(Doctor doctor, LocalDate date) {
        return slotRepository.findByDoctorAndStartTimeBetween(
                doctor, date.atStartOfDay(), date.plusDays(1).atStartOfDay()).size();
    }

    /**
     * A warning message when the doctor now has more slots on this day than their daily
     * patient limit allows - the "you've over-provisioned this doctor" notice reception
     * sees after adding a slot. Empty when the day is within the limit.
     */
    public Optional<String> slotOverLimitWarning(Doctor doctor, LocalDate date) {
        int slots = slotCountOn(doctor, date);
        int limit = doctor.getDailyPatientLimit();
        if (slots > limit) {
            return Optional.of(String.format(
                    "%s now has %d slots on %s, over their daily patient limit of %d.",
                    doctor.getName(), slots, date, limit));
        }
        return Optional.empty();
    }
}
