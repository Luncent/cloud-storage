package it.luncent.cloud_storage.security.exception;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
