package it.luncent.cloud_storage.storage.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import it.luncent.cloud_storage.storage.exception.StorageException;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static java.lang.String.format;

//TODO rethink exception handling
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final String FOLDER_NOT_FOUND_TEMPLATE = "folder %s not found";
    private static final String EMPTY_FOLDER_TAG = "empty-folder-tag";
    private static final Integer FILE_SIZE_NOT_KNOWN = -1;
    private static final Integer FILE_SIZE_IS_KNOWN = -1;
    private static final Integer EMPTY_FOLDER_SIZE = 0;
    private static final Long MB = 1024L * 1024L;
    private static final Long PART_SIZE = 10 * MB;

    private final MinioClient minioClient;

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName).build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public StatObjectResponse getObject(String bucketName, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                throw new ResourceNotFoundException(e.response().message(), e);
            }
            throw new StorageException(e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public String createEmptyDirectory(String bucketName, String targetDirectory) {
        try {
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(targetDirectory + EMPTY_FOLDER_TAG)
                            .stream(new ByteArrayInputStream(new byte[EMPTY_FOLDER_SIZE]), EMPTY_FOLDER_SIZE, FILE_SIZE_IS_KNOWN)
                            .build()
            );
            return response.object();
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public void checkDirectoryExistence(String bucketName, String folderName) {
        Iterable<Result<Item>> objects = null;
        try {
            objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(folderName)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
        if (!objects.iterator().hasNext()) {
            throw new ResourceNotFoundException(format(FOLDER_NOT_FOUND_TEMPLATE, folderName));
        }
    }

    @Override
    public void uploadFile(String bucketName, String fileName, InputStream inputStream, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .contentType(contentType)
                            .stream(inputStream, FILE_SIZE_NOT_KNOWN, PART_SIZE)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteFile(String bucketName, String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String bucketName, String fileName) {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return response;
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }
}
