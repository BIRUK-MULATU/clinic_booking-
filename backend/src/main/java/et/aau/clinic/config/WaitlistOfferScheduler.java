package et.aau.clinic.config;

import et.aau.clinic.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hospital-expansion Rule L: drives AppointmentService.expireStaleWaitlistOffers()
 * on a fixed schedule. The logic lives in the service and in
 * WaitlistOfferPolicy (both unit-tested); this class is just the timer,
 * excluded from coverage with the rest of config/.
 *
 * @Profile("!test") so it never fires during the *IT tests, which call
 * expireStaleWaitlistOffers() directly against a fixed Clock.
 * @EnableScheduling already lives on ReminderScheduler - one is enough
 * for the whole application.
 */
@Component
@Profile("!test")
public class WaitlistOfferScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaitlistOfferScheduler.class);

    private final AppointmentService appointmentService;

    public WaitlistOfferScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // Every 5 minutes: the acceptance window is 2 hours, so this is comfortably fine-grained.
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void expireStaleWaitlistOffers() {
        int expired = appointmentService.expireStaleWaitlistOffers();
        if (expired > 0) {
            log.info("Waitlist sweep: expired {} unconfirmed offer(s)", expired);
        }
    }
}
