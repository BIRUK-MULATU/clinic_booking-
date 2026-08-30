package et.aau.clinic.system.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SlotsPage {

    private final WebDriver driver;
    private final String baseUrl;

    public SlotsPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public BookingPage bookSlot(Long slotId) {
        driver.findElement(By.id("book-link-" + slotId)).click();
        return new BookingPage(driver, baseUrl);
    }

    public MyAppointmentsPage goToMyAppointments() {
        driver.findElement(By.id("my-appointments-link")).click();
        return new MyAppointmentsPage(driver, baseUrl);
    }
}
