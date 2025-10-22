package it.luncent.cloud_storage.resource.exception;

public class MoveConflictException extends RuntimeException {
    public MoveConflictException(String message) {
        super(message);
    }
}
