package et.aau.clinic.config;

import et.aau.clinic.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hospital-expansion Rule F: drives AppointmentService.sendDueReminders()
 * on a fixed schedule. All the logic lives in the service and in
 * ReminderPolicy (which the unit tests cover); this class is just the
 * timer, so it is excluded from coverage along with the rest of config/.
 *
 * @Profile("!test") so the scheduler never fires during the *IT tests -
 * they call sendDueReminders() directly against a fixed Clock instead.
 * @EnableScheduling sits here rather than on ClinicApplication for the
 * same reason: activating it is part of what this non-test bean does.
 */
@Component
@Profile("!test")
@EnableScheduling
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final AppointmentService appointmentService;

    public ReminderScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // Every 15 minutes. The reminder window is 24 hours, so this cadence is far finer
    // than it needs to be - a missed run just means the next one picks the appointment up.
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void sendDueReminders() {
        int sent = appointmentService.sendDueReminders();
        if (sent > 0) {
            log.info("Reminder sweep: sent {} appointment reminder(s)", sent);
        }
    }
}
