package it.luncent.cloud_storage.resource.file.service;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;

import java.io.OutputStream;
import java.util.Set;

public interface FileService {

    ResourceMetadataResponse getMetadata(String path);

    void delete(String path);

    void deleteFilesAndMarkersBatch(String bucket, Set<String> paths);

    void download(OutputStream outputStream, String path);

    ResourceMetadataResponse move(MoveRequest request);

    boolean exists(String path);


}
