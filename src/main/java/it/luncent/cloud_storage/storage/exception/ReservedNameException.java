package it.luncent.cloud_storage.storage.exception;

public class ReservedNameException extends RuntimeException {
    public ReservedNameException(String message) {
        super(message);
    }
}
