package it.luncent.cloud_storage.resource.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReservedNameException extends RuntimeException {
    @Getter
    private final String reservedName;
}
