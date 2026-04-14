package it.luncent.cloud_storage.resource.file.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class FileExistsException extends RuntimeException {
    private final String fileName;
}
