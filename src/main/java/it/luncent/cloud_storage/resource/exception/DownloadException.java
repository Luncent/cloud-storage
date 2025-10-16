package it.luncent.cloud_storage.resource.exception;

public class DownloadException extends RuntimeException {
    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
