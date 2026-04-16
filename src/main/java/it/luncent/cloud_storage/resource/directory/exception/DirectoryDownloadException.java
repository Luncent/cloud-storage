package it.luncent.cloud_storage.resource.directory.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DirectoryDownloadException extends RuntimeException {
    @Getter
    private final String directory;
}
