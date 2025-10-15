package it.luncent.cloud_storage.storage.service;

import io.minio.StatObjectResponse;

import java.io.InputStream;

//TODO change parameters and return types
//TODO remove bucket creation in code do it in docker
public interface StorageService {

    boolean bucketExists(String bucketName);

    StatObjectResponse getObject(String bucketName, String objectName);

    void uploadFile(String bucketName, String fileName, InputStream inputStream, String contentType);

    void deleteFile(String bucketName, String fileName);

    InputStream downloadFile(String bucketName, String fileName);

    String createEmptyDirectory(String bucketName, String folderName);

    void checkDirectoryExistence(String bucketName, String folderName);
}
