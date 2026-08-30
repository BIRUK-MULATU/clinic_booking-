package et.aau.clinic.system.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ConfirmationPage {

    private final WebDriver driver;
    private final String baseUrl;

    public ConfirmationPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public String getStatus() {
        return driver.findElement(By.id("confirmation-status")).getText();
    }

    public String getFeeCategory() {
        return driver.findElement(By.id("confirmation-fee-category")).getText();
    }

    public String getFeeAmount() {
        return driver.findElement(By.id("confirmation-fee-amount")).getText();
    }

    /** The appointment id, read from the current /confirmation/{id} URL. */
    public Long getAppointmentId() {
        String url = driver.getCurrentUrl();
        return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
    }

    public MyAppointmentsPage goToMyAppointments() {
        driver.findElement(By.id("my-appointments-link")).click();
        return new MyAppointmentsPage(driver, baseUrl);
    }
}
