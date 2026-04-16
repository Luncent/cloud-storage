package it.luncent.cloud_storage.storage.service;

import io.minio.ObjectWriteResponse;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

public interface StorageService {

    ObjectWriteResponse copyObject(String from, String to, String bucket);

    void delete(String path, String bucket);

    InputStream download(String path, String bucket);

    Iterable<Result<Item>> listObjects(String path, String bucket);

    Optional<StatObjectResponse> getObjectMetadata(String path, String bucket);

    ObjectWriteResponse upload(String path, String bucket, InputStream inputStream, String contentType);

    void deleteFilesBatch(String bucketName, Set<String> objectNames);
}
