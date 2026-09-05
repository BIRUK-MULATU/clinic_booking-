package et.aau.clinic.web.api;

import et.aau.clinic.web.api.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Scoped to et.aau.clinic.web.api only, so it takes precedence over the
 * Thymeleaf app's GlobalExceptionHandler for these controllers - without
 * this, the same exceptions would render the "error" HTML view instead
 * of a JSON body, which the fetch client in frontend/ cannot use.
 */
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
