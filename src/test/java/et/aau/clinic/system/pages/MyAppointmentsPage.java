package et.aau.clinic.system.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MyAppointmentsPage {

    private final WebDriver driver;
    private final String baseUrl;

    public MyAppointmentsPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public String getStatus(Long appointmentId) {
        return driver.findElement(By.id("appointment-status-" + appointmentId)).getText();
    }

    public MyAppointmentsPage cancel(Long appointmentId) {
        driver.findElement(By.id("cancel-button-" + appointmentId)).click();
        return new MyAppointmentsPage(driver, baseUrl);
    }

    public MyAppointmentsPage confirm(Long appointmentId) {
        driver.findElement(By.id("confirm-button-" + appointmentId)).click();
        return new MyAppointmentsPage(driver, baseUrl);
    }
}
