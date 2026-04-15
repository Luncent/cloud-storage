package it.luncent.cloud_storage.resource.file.service;

import it.luncent.cloud_storage.resource.model.common.Path;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public interface FileService {

    ResourceMetadataResponse getMetadata(String path);

    void delete(String path);

    void deleteFilesBatch(String bucket, Set<String> paths);

    void download(OutputStream outputStream, String path);

    ResourceMetadataResponse move(MoveRequest request);

    boolean exists(String path);

    ResourceMetadataResponse upload(InputStream inputStream, Path path, String contentType);
}
