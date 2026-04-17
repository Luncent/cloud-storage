package it.luncent.cloud_storage.resource.file.exception;

import it.luncent.cloud_storage.common.exception.ErrorResponse;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class FileExceptionHandler {

    private static final String FILE_NOT_FOUND_TEMPLATE = "file %s not found";
    private static final String FILE_EXISTS_TEMPLATE = "file %s exists";
    private static final String RESERVED_NAME_TEMPLATE = "file name %s is reserved";
    private static final String DOWNLOAD_ERROR_TEMPLATE = "could not download %s";

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex) {
        String message = String.format(FILE_NOT_FOUND_TEMPLATE, PathUtils.getRelativePath(ex.getName()));
        log.error(message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(ReservedNameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleReservedNameException(ReservedNameException e) {
        String message = String.format(RESERVED_NAME_TEMPLATE, PathUtils.getRelativePath(e.getReservedName()));
        log.error(message);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(FileExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFilesExistException(FileExistsException e) {
        String message = String.format(FILE_EXISTS_TEMPLATE, PathUtils.getRelativePath(e.getFileName()));
        log.error(message);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(FileDownloadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleFileDownloadException(FileDownloadException e) {
        String message = String.format(DOWNLOAD_ERROR_TEMPLATE, PathUtils.getRelativePath(e.getFileName()));
        log.error(message);
        return new ErrorResponse(message);
    }

}
