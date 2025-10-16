package it.luncent.cloud_storage.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@ControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(BindingResult ex, HttpServletRequest request, HttpServletResponse response) {
        String error = ex.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(joining(", "));
        log.error(error, ex);
        ErrorResponse errorResponse = new ErrorResponse(error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleException(HandlerMethodValidationException ex, HttpServletRequest request, HttpServletResponse response) {
        log.error(ex.getMessage(), ex);
        String errorMessage = ex.getParameterValidationResults().stream()
                .flatMap(result-> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(joining(", "));
        ErrorResponse errorResponse = new ErrorResponse(errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
