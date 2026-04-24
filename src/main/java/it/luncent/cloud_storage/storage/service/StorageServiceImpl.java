package it.luncent.cloud_storage.storage.service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import it.luncent.cloud_storage.storage.exception.converter.MinioExceptionConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

//TODO exception handling
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private static final Integer FILE_SIZE_NOT_KNOWN = -1;
    private static final Long MB = 1024L * 1024L;
    private static final Long PART_SIZE = 10 * MB;

    private final MinioClient minioClient;
    private final MinioExceptionConverter exceptionConverter;

    @Override
    public ObjectWriteResponse copyObject(String from, String to, String bucket) {
        try {
            return minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(
                                    CopySource.builder()
                                            .bucket(bucket)
                                            .object(from)
                                            .build()
                            )
                            .bucket(bucket)
                            .object(to)
                            .build()
            );
        } catch (ErrorResponseException ex) {
            throw exceptionConverter.convert(ex);
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String path, String bucket) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String path, String bucket) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
        } catch (ErrorResponseException ex) {
            throw exceptionConverter.convert(ex);
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public Iterable<Result<Item>> listObjects(String path, String bucket) {
        if (!path.endsWith("/")) {
            throw new IllegalArgumentException("list objects only allowed on path ending with /");
        }
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(path)
                            .build()
            );
            objects.iterator().next().get();
            return objects;
        } catch (ErrorResponseException ex) {
            throw exceptionConverter.convert(ex);
        } catch (NoSuchElementException ex) {
            throw new ObjectNotFoundException(path);
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<StatObjectResponse> getObjectMetadata(String path, String bucket) {
        try {
            return Optional.of(minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            ));
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                return Optional.empty();
            }
            throw new StorageException(e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public ObjectWriteResponse upload(String path, String bucket, InputStream inputStream, String contentType) {
        try {
            return minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .contentType(contentType)
                            .stream(inputStream, FILE_SIZE_NOT_KNOWN, PART_SIZE)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteFilesBatch(String bucketName, Set<String> objectNames) {
        List<DeleteObject> deleteObjects = objectNames.stream()
                .map(DeleteObject::new)
                .toList();
        try {
            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build()
            );
            for (Result<DeleteError> error : errors) {
            }
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

}
