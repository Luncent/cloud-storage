package it.luncent.cloud_storage.resource.file.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FileNotFoundException extends RuntimeException {
    private final String message;
}
