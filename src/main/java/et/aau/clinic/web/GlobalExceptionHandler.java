package et.aau.clinic.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

/**
 * Turns the exceptions core/ and the repositories throw (invalid state
 * transition, invalid age, record not found) into a plain error page
 * instead of Spring's Whitelabel page - keeps Selenium tests able to
 * assert on something.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class, NoSuchElementException.class})
    public String handleBusinessError(Exception ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }
}
