package it.luncent.cloud_storage.resource.directory.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class DirectoryMoveException extends RuntimeException {
    @Getter
    private final Set<String> conflictFiles;
}
