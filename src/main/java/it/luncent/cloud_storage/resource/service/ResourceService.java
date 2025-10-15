package it.luncent.cloud_storage.resource.service;

import it.luncent.cloud_storage.resource.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;

import java.io.InputStream;
import java.util.List;

public interface ResourceService {

    ResourceMetadataResponse getResourceMetadata(String path);

    List<ResourceMetadataResponse> upload(UploadRequest request);

    ResourceMetadataResponse createDirectory(String path);

    void deleteResource(String path);

    ResourceMetadataResponse moveOrRenameResource(MoveRenameRequest request);

    List<ResourceMetadataResponse> searchResource(String query);

    List<ResourceMetadataResponse> getDirectoryContents(String path);

    InputStream downloadResource(String path);
}
