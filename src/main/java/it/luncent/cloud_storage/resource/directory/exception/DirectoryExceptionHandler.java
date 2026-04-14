package it.luncent.cloud_storage.resource.directory.exception;

import it.luncent.cloud_storage.common.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class DirectoryExceptionHandler {

    private static final String EXISTS_TEMPLATE = "Directory %s already exists";

    @ExceptionHandler(DirectoryExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse directoryExistsException(DirectoryExistsException e) {
        String message = String.format(EXISTS_TEMPLATE, e.getName());
        log.error(message);
        return new ErrorResponse(message);
    }


}
