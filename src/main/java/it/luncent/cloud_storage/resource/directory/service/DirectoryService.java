package it.luncent.cloud_storage.resource.directory.service;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;

public interface DirectoryService {

    ResourceMetadataResponse getMetadata(String path);

    List<ResourceMetadataResponse> getContents(String path);

    Set<String> getAllContents(String path);

    ResourceMetadataResponse createEmptyDirectory(String path);

    void delete(String path);

    void download(OutputStream outputStream, String path);

    ResourceMetadataResponse move(MoveRequest request);

    boolean exists(String path);
}
