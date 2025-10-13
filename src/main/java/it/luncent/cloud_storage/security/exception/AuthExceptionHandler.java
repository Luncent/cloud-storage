package it.luncent.cloud_storage.security.exception;

import it.luncent.cloud_storage.common.exception.ErrorMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

//TODO add AOP for logging
@ControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleUsernameNotFoundException(BadCredentialsException ex, HttpServletRequest request, HttpServletResponse response) {
        log.error(ex.getMessage(), ex);
        ErrorMessage errorMessage = new ErrorMessage("Bad credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
    }

    @ExceptionHandler(UsernameExistsException.class)
    public ResponseEntity<ErrorMessage> handleUsernameExistsException(UsernameExistsException ex, HttpServletRequest request, HttpServletResponse response) {
        log.error(ex.getMessage(), ex);
        ErrorMessage errorMessage = new ErrorMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

}
