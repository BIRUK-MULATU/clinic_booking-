package et.aau.clinic.system.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class BookingPage {

    private final WebDriver driver;
    private final String baseUrl;

    public BookingPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public ConfirmationPage confirmBooking() {
        driver.findElement(By.id("book-submit")).click();
        return new ConfirmationPage(driver, baseUrl);
    }

    public String getErrorMessage() {
        return driver.findElement(By.id("booking-error")).getText();
    }
}
