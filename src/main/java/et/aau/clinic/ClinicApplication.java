package et.aau.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class ClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicApplication.class, args);
    }

    /**
     * The real clock, used when the application actually runs.
     * Tests replace this bean with Clock.fixed(...) so that time-dependent
     * rules (2-hour booking lead time, 24-hour cancellation window)
     * can be tested at their exact boundaries.
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
