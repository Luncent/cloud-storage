package it.luncent.cloud_storage.storage.service;

import io.minio.ObjectWriteResponse;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationSettings;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StorageService {

    ObjectWriteResponse copyObject(ResourcePath from, ResourcePath to);

    void delete(ResourcePath filePath);

    InputStream downloadFile(ResourcePath filePath);

    Iterable<Result<Item>> getDirectoryContent(ResourcePath directoryPath);

    Optional<StatObjectResponse> getObjectMetadata(ResourcePath objectPath);

    void populateWithDirectoryObjects(ResourcePath directoryPath, List<Item> objects, PopulationSettings populationSettings);

    void populateWithDirectoryObjectsAsync(ResourcePath directoryPath, List<Item> objects, PopulationSettings populationSettings);

    void uploadFile(ResourcePath filePath, InputStream inputStream, String contentType);

    ObjectWriteResponse uploadFile(ResourcePath filePath, InputStream inputStream);

    void deleteFilesBatch(String bucketName, Set<String> objectNames);
}
