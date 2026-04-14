package it.luncent.cloud_storage.storage.exception.converter;

import io.minio.errors.ErrorResponseException;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import org.springframework.stereotype.Component;

@Component
public class MinioExceptionConverter {

    public StorageException convert(ErrorResponseException e) {
        switch (e.response().code()){
            case 404: throw new ObjectNotFoundException(e.errorResponse().objectName());
            default: return new StorageException(e);
        }
    }

}
