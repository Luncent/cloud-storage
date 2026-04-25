package it.luncent.cloud_storage.resource.exception;

import it.luncent.cloud_storage.common.exception.ErrorResponse;
import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ResourceExceptionHandler {
    private static final String RESERVED_NAME_TEMPLATE = "name %s is reserved";

    @ExceptionHandler(ReservedNameException.class)
    public ResponseEntity<ErrorResponse> reservedNameException(ReservedNameException ex) {
        String message = String.format(RESERVED_NAME_TEMPLATE, ex.getReservedName());
        ErrorResponse errorResponse = new ErrorResponse(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(FileCountLimitExceededException.class)
    public ResponseEntity<ErrorResponse> fileCountLimitException(FileCountLimitExceededException ex) {
        String message = "files count limit exceeded";
        ErrorResponse errorResponse = new ErrorResponse(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> maxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        String message = "max upload size exceeded";
        ErrorResponse errorResponse = new ErrorResponse(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
