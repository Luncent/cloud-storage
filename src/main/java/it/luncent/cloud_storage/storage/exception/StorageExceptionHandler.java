package it.luncent.cloud_storage.storage.exception;

import it.luncent.cloud_storage.common.exception.ErrorResponse;
import it.luncent.cloud_storage.resource.exception.DownloadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class StorageExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ReservedNameException.class)
    public ResponseEntity<ErrorResponse> handleReservedNameException(ReservedNameException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleMinioException(StorageException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("storage error"));
    }

    @ExceptionHandler(DownloadException.class)
    public ResponseEntity<ErrorResponse> handleMinioException(DownloadException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("download error"));
    }
}
