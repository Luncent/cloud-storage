package it.luncent.cloud_storage.minio.service;

import io.minio.*;
import it.luncent.cloud_storage.minio.exception.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

//TODO rethink exception handling
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private static final String EMPTY_FOLDER_TAG = "empty-folder-tag";
    private static final Integer FILE_SIZE_NOT_KNOWN = -1;
    private static final Integer FILE_SIZE_IS_KNOWN = -1;
    private static final Integer EMPTY_FOLDER_SIZE = 0;
    private static final Long MB = 1024L * 1024L;
    private static final Long PART_SIZE = 10 * MB;

    private final MinioClient minioClient;

    @Override
    public void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException(e.getMessage(), e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName).build()
            );
        } catch (Exception ex) {
            throw new MinioException(ex.getMessage(), ex);
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
        } catch (Exception e) {
            throw new MinioException(e.getMessage(), e);
        }
    }

    @Override
    public void createEmptyDirectory(String bucketName, String targetDirectory) {
        try {
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(targetDirectory + EMPTY_FOLDER_TAG)
                            .stream(new ByteArrayInputStream(new byte[EMPTY_FOLDER_SIZE]), EMPTY_FOLDER_SIZE, FILE_SIZE_IS_KNOWN)
                            .build()
            );
        } catch (Exception ex) {
            throw new MinioException(ex.getMessage(), ex);
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
            throw new MinioException(ex.getMessage(), ex);
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
            throw new MinioException(e.getMessage(), e);
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
        }catch (Exception ex){
            throw new MinioException(ex.getMessage(), ex);
        }
    }
}
