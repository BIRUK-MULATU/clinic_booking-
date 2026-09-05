package et.aau.clinic.service;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stands in for a real SMS gateway. Logging is enough here: the point
 * of this project is the test suite, not a paid SMS integration, and
 * this still gives production code a real (if minimal) implementation
 * to depend on outside of tests.
 */
@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void sendConfirmation(Patient patient, Appointment appointment) {
        log.info("SMS to {}: your appointment on {} is confirmed, fee {} ETB",
                patient.getPhone(), appointment.getSlot().getStartTime(), appointment.getFeeAmount());
    }
}
