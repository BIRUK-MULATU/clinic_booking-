package et.aau.clinic.system.pages;

import org.openqa.selenium.WebDriver;

public class MyAppointmentsPage extends AbstractPage {

    public MyAppointmentsPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public String getStatus(Long appointmentId) {
        return find("appointment-status-" + appointmentId).getText();
    }

    public MyAppointmentsPage cancel(Long appointmentId) {
        find("cancel-button-" + appointmentId).click();
        return new MyAppointmentsPage(driver, baseUrl);
    }

    public MyAppointmentsPage confirm(Long appointmentId) {
        find("confirm-button-" + appointmentId).click();
        return new MyAppointmentsPage(driver, baseUrl);
    }
}
