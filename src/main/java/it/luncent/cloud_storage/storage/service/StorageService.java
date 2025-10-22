package it.luncent.cloud_storage.storage.service;

import io.minio.ObjectWriteResponse;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationFilter;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;

import java.io.InputStream;
import java.util.List;

//TODO change parameters and return types
//TODO remove bucket creation in code do it in docker
public interface StorageService {

    String createEmptyDirectory(ResourcePath directoryPath);

    ObjectWriteResponse copyObject(ResourcePath from, ResourcePath to);

    void deleteDirectory(ResourcePath directoryPath);

    void deleteFile(ResourcePath filePath);

    InputStream downloadFile(ResourcePath filePath);

    Iterable<Result<Item>> getDirectoryContent(ResourcePath directoryPath);

    StatObjectResponse getObjectMetadata(ResourcePath objectPath);

    void populateWithDirectoryObjects(ResourcePath directoryPath, List<Item> objects, PopulationFilter populationFilter);

    void populateWithDirectoryObjectsAsync(ResourcePath directoryPath, List<Item> objects, PopulationFilter populationFilter);

    void uploadFile(ResourcePath filePath, InputStream inputStream, String contentType);
}
