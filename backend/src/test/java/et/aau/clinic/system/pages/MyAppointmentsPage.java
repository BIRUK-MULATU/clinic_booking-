package et.aau.clinic.system.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

public class MyAppointmentsPage extends AbstractPage {

    public MyAppointmentsPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public String getStatus(Long appointmentId) {
        return find("appointment-status-" + appointmentId).getText();
    }

    /** The appointment's slot time cell - changes when the appointment is rescheduled (Rule K). */
    public String getSlotTime(Long appointmentId) {
        return find("appointment-slot-" + appointmentId).getText();
    }

    /** Rule K: pick a new slot from the row's dropdown and submit the reschedule. */
    public MyAppointmentsPage reschedule(Long appointmentId, Long newSlotId) {
        new Select(find("reschedule-slot-" + appointmentId)).selectByValue(String.valueOf(newSlotId));
        find("reschedule-button-" + appointmentId).click();
        return new MyAppointmentsPage(driver, baseUrl);
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
