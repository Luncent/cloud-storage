package it.luncent.cloud_storage.resource.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.luncent.cloud_storage.common.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class ResourceExceptionHandler {
    private static final String RESERVED_NAME_TEMPLATE = "name %s is reserved";
    private final ObjectMapper objectMapper;

    @ExceptionHandler(ReservedNameException.class)
    public ResponseEntity<ErrorResponse> reservedNameException(ReservedNameException ex) {
        String message = String.format(RESERVED_NAME_TEMPLATE, ex.getReservedName());
        ErrorResponse errorResponse = new ErrorResponse(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
