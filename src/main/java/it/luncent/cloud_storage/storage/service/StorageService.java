package it.luncent.cloud_storage.storage.service;

import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;

import java.io.InputStream;
import java.util.List;

//TODO change parameters and return types
//TODO remove bucket creation in code do it in docker
public interface StorageService {

    void checkDirectoryExistence(ResourcePath directoryPath);

    String createEmptyDirectory(ResourcePath directoryPath);

    void deleteDirectory(ResourcePath directoryPath);

    void deleteFile(ResourcePath filePath);

    InputStream downloadFile(ResourcePath filePath);

    StatObjectResponse getObject(ResourcePath objectPath);

    void populateWithDirectoryObjects(ResourcePath directoryPath, List<Item> objects);

    void uploadFile(ResourcePath filePath, InputStream inputStream, String contentType);
}
