package it.luncent.cloud_storage.minio.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
