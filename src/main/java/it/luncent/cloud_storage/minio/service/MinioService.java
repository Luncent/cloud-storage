package it.luncent.cloud_storage.minio.service;

import io.minio.StatObjectResponse;

import java.io.InputStream;

//TODO change parameters and return types
public interface MinioService {

    void createBucket(String bucketName);

    boolean bucketExists(String bucketName);

    StatObjectResponse getObject(String bucketName, String objectName);

    void uploadFile(String bucketName, String fileName, InputStream inputStream, String contentType);

    void deleteFile(String bucketName, String fileName);

    InputStream downloadFile(String bucketName, String fileName);

    String createEmptyDirectory(String bucketName, String folderName);
}
