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
    private static final String NOT_FOUND_TEMPLATE = "Directory %s not found";
    private static final String MOVE_CONFLICT = "files %s already exist";
    private static final String DOWNLOAD_ERROR_TEMPLATE = "could not download directory %s";

    @ExceptionHandler(DirectoryExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse directoryExistsException(DirectoryExistsException e) {
        String message = String.format(EXISTS_TEMPLATE, e.getName());
        log.error(message);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(DirectoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse directoryNotFoundException(DirectoryNotFoundException e) {
        String message = String.format(NOT_FOUND_TEMPLATE, e.getName());
        log.error(message);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(DirectoryMoveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse directoryMoveException(DirectoryMoveException e) {
        String message = String.format(
                MOVE_CONFLICT,
                String.join(",\n", e.getConflictFiles())
        );
        log.error(message);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(DirectoryDownloadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse directoryDownloadException(DirectoryDownloadException e) {
        String message = String.format(DOWNLOAD_ERROR_TEMPLATE, e.getDirectory());
        log.error(message);
        return new ErrorResponse(message);
    }

}
