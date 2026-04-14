package it.luncent.cloud_storage.storage.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ObjectNotFoundException extends StorageException {
    private final String name;
}
