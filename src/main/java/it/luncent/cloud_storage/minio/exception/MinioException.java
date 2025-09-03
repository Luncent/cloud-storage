package it.luncent.cloud_storage.minio.exception;

public class MinioException extends RuntimeException {
    public MinioException(String message, Throwable cause) {
        super(message, cause);
    }
}
