package et.aau.clinic.config;

import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Demo data for manual use (mvn spring-boot:run) and for a human to
 * click through the app. Excluded from the "test" profile so it never
 * pollutes the fresh H2 instance each integration/system test gets -
 * see src/test/resources/application.properties.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final Clock clock;

    public DataSeeder(PatientRepository patientRepository, SlotRepository slotRepository, Clock clock) {
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        Patient adult = new Patient("Abebe Kebede", LocalDate.of(1990, 5, 1), "abebe", "secret", "0911000001");
        Patient child = new Patient("Selam Tesfaye", LocalDate.of(2015, 6, 15), "selam", "secret", "0911000002");
        Patient senior = new Patient("Almaz Girma", LocalDate.of(1955, 1, 1), "almaz", "secret", "0911000003");
        senior.setOutstandingBalance(new BigDecimal("100"));
        patientRepository.save(adult);
        patientRepository.save(child);
        patientRepository.save(senior);

        LocalDateTime now = LocalDateTime.now(clock);
        slotRepository.save(new Slot(now.plusDays(1).withHour(9).withMinute(0)));
        slotRepository.save(new Slot(now.plusDays(1).withHour(10).withMinute(0)));
        slotRepository.save(new Slot(now.plusDays(2).withHour(9).withMinute(0)));
        slotRepository.save(new Slot(now.plusDays(2).withHour(14).withMinute(30)));
        slotRepository.save(new Slot(now.plusHours(1)));
    }
}
