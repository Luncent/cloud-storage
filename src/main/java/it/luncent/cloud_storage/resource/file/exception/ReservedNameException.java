package it.luncent.cloud_storage.resource.file.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ReservedNameException extends RuntimeException {
    @Getter
    private final String reservedName;
}
