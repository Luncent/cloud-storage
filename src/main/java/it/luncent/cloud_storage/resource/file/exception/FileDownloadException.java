package it.luncent.cloud_storage.resource.file.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileDownloadException extends RuntimeException {
    @Getter
    private final String fileName;
}
