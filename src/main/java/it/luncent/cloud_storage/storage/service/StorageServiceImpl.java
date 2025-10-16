package it.luncent.cloud_storage.storage.service;

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
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.storage.exception.ReservedNameException;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static it.luncent.cloud_storage.storage.util.StorageUtil.isDirectory;
import static java.lang.String.format;

//TODO rethink exception handling
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final String FOLDER_NOT_FOUND_TEMPLATE = "folder %s not found";
    private static final String EMPTY_DIRECTORY_TAG = "empty-folder-tag";
    private static final Integer FILE_SIZE_NOT_KNOWN = -1;
    private static final Integer FILE_SIZE_IS_KNOWN = -1;
    private static final Integer EMPTY_FOLDER_SIZE = 0;
    private static final Long MB = 1024L * 1024L;
    private static final Long PART_SIZE = 10 * MB;

    private final MinioClient minioClient;


    @Override
    public void checkDirectoryExistence(ResourcePath directoryPath) {
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(directoryPath.bucketName())
                            .prefix(directoryPath.full())
                            .build()
            );
            objects.iterator().next().get();
        } catch (ErrorResponseException ex) {
            if (ex.response().code() == 404) {
                throw new ResourceNotFoundException(format(FOLDER_NOT_FOUND_TEMPLATE, directoryPath.relative()), ex);
            }
            throw new StorageException(ex.getMessage(), ex);
        } catch (NoSuchElementException ex) {
            throw new ResourceNotFoundException(format(FOLDER_NOT_FOUND_TEMPLATE, directoryPath.relative()), ex);
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public String createEmptyDirectory(ResourcePath directoryPath) {
        try {
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(directoryPath.bucketName())
                            .object(directoryPath.full() + EMPTY_DIRECTORY_TAG)
                            .stream(new ByteArrayInputStream(new byte[EMPTY_FOLDER_SIZE]), EMPTY_FOLDER_SIZE, FILE_SIZE_IS_KNOWN)
                            .build()
            );
            return response.object();
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteDirectory(ResourcePath directoryPath) {
        checkDirectoryExistence(directoryPath);
        List<Item> objects = new ArrayList<>();
        populateWithDirectoryObjects(directoryPath, objects, false);
        List<String> objectNames = objects.stream()
                .map(Item::objectName)
                .toList();
        deleteFilesBatch(directoryPath.bucketName(), objectNames);
    }

    @Override
    public void deleteFile(ResourcePath filePath) {
        checkRequestedPathForEmptyDirectoryTag(filePath);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(filePath.bucketName())
                            .object(filePath.full())
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(ResourcePath filePath) {
        checkRequestedPathForEmptyDirectoryTag(filePath);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(filePath.bucketName())
                            .object(filePath.full())
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public StatObjectResponse getObject(ResourcePath objectPath) {
        checkRequestedPathForEmptyDirectoryTag(objectPath);
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(objectPath.bucketName())
                            .object(objectPath.full())
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                throw new ResourceNotFoundException(objectPath.relative() + " not found", e);
            }
            throw new StorageException(e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public void populateWithDirectoryObjects(ResourcePath directoryPath, List<Item> objects) {
        populateWithDirectoryObjects(directoryPath, objects, true);
    }

    @Override
    public void uploadFile(ResourcePath filePath, InputStream inputStream, String contentType) {
        checkRequestedPathForEmptyDirectoryTag(filePath);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(filePath.bucketName())
                            .object(filePath.full())
                            .contentType(contentType)
                            .stream(inputStream, FILE_SIZE_NOT_KNOWN, PART_SIZE)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    private void checkRequestedPathForEmptyDirectoryTag(ResourcePath objectPath) {
        if (objectPath.relative().endsWith(EMPTY_DIRECTORY_TAG)) {
            throw new ReservedNameException(EMPTY_DIRECTORY_TAG + " is reserved");
        }
    }

    public void deleteFilesBatch(String bucketName, List<String> objectNames) {
        List<DeleteObject> deleteObjects = objectNames.stream()
                .map(DeleteObject::new)
                .toList();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build()
        );
        results.forEach(result -> {
        });
    }

    private void populateWithDirectoryObjects(ResourcePath directoryPath, List<Item> objects, boolean skipDirectoryMarkingFile) {
        try {
            //TODO проверить сколько запросов по сети идет, распаралелить
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(directoryPath.bucketName())
                            .prefix(directoryPath.full())
                            .build()
            );
            for (Result<Item> result : results) {
                Item object = result.get();
                String objectName = object.objectName();
                if (isDirectory(objectName)) {
                    populateWithDirectoryObjects(directoryPath, objects, skipDirectoryMarkingFile);
                    continue;
                }
                if (!skipDirectoryMarkingFile || !objectName.endsWith(EMPTY_DIRECTORY_TAG)) {
                    objects.add(object);
                }
            }
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }
}
