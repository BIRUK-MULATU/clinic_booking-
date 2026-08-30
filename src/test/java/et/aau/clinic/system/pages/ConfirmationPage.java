package et.aau.clinic.system.pages;

import org.openqa.selenium.WebDriver;

public class ConfirmationPage extends AbstractPage {

    public ConfirmationPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public String getStatus() {
        return find("confirmation-status").getText();
    }

    public String getFeeCategory() {
        return find("confirmation-fee-category").getText();
    }

    public String getFeeAmount() {
        return find("confirmation-fee-amount").getText();
    }

    /** The appointment id, read from the current /confirmation/{id} URL. */
    public Long getAppointmentId() {
        String url = driver.getCurrentUrl();
        return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
    }

    public MyAppointmentsPage goToMyAppointments() {
        find("my-appointments-link").click();
        return new MyAppointmentsPage(driver, baseUrl);
    }
}
