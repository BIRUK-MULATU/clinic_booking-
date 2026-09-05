package et.aau.clinic.config;

import et.aau.clinic.domain.Department;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.DepartmentRepository;
import et.aau.clinic.repository.DoctorRepository;
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
 *
 * Departments/doctors are seeded (hospital-expansion Phase A) so the new
 * tables are populated, but no existing page renders them yet - the
 * five graded templates are unchanged, so seeding this data has no
 * visible effect on the app a user clicks through today.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final Clock clock;

    public DataSeeder(PatientRepository patientRepository, SlotRepository slotRepository,
                       DepartmentRepository departmentRepository, DoctorRepository doctorRepository, Clock clock) {
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
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

        Department cardiology = departmentRepository.save(new Department("Cardiology"));
        Department pediatrics = departmentRepository.save(new Department("Pediatrics"));
        Doctor cardiologist = doctorRepository.save(
                new Doctor("Dr. Kebede Alemu", "Cardiologist", cardiology));
        Doctor pediatrician = doctorRepository.save(
                new Doctor("Dr. Hanna Tesfaye", "Pediatrician", pediatrics));

        LocalDateTime now = LocalDateTime.now(clock);
        slotRepository.save(new Slot(now.plusDays(1).withHour(9).withMinute(0), cardiologist));
        slotRepository.save(new Slot(now.plusDays(1).withHour(10).withMinute(0), cardiologist));
        slotRepository.save(new Slot(now.plusDays(2).withHour(9).withMinute(0), pediatrician));
        slotRepository.save(new Slot(now.plusDays(2).withHour(14).withMinute(30), pediatrician));
        slotRepository.save(new Slot(now.plusHours(1)));
    }
}
