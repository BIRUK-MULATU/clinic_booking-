package et.aau.clinic.web.api;

import et.aau.clinic.web.api.dto.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Scoped to et.aau.clinic.web.api only, and ordered ahead of the
 * Thymeleaf app's GlobalExceptionHandler, so for these controllers an
 * IllegalStateException/IllegalArgumentException comes back as a JSON
 * body the fetch client can read rather than the "error" HTML view.
 *
 * The @Order is load-bearing: without it the two @ControllerAdvice
 * beans tie at LOWEST_PRECEDENCE and which one handles a thrown
 * exception is undefined - see DEF-004.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "et.aau.clinic.web.api")
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound() {
        return ResponseEntity.status(404).body(new ErrorResponse("Not found."));
    }
}
