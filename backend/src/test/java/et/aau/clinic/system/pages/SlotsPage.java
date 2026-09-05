package et.aau.clinic.system.pages;

import org.openqa.selenium.WebDriver;

public class SlotsPage extends AbstractPage {

    public SlotsPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public BookingPage bookSlot(Long slotId) {
        find("book-link-" + slotId).click();
        return new BookingPage(driver, baseUrl);
    }

    public MyAppointmentsPage goToMyAppointments() {
        find("my-appointments-link").click();
        return new MyAppointmentsPage(driver, baseUrl);
    }
}
