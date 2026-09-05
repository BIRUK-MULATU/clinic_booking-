package et.aau.clinic.system.pages;

import org.openqa.selenium.WebDriver;

public class BookingPage extends AbstractPage {

    public BookingPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public ConfirmationPage confirmBooking() {
        find("book-submit").click();
        return new ConfirmationPage(driver, baseUrl);
    }

    public String getErrorMessage() {
        return find("booking-error").getText();
    }
}
