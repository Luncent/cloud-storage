package it.luncent.cloud_storage.storage.service;

import it.luncent.cloud_storage.storage.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.storage.model.request.UploadRequest;
import it.luncent.cloud_storage.storage.model.response.ResourceMetadataResponse;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    ResourceMetadataResponse getResourceMetadata(String path);

    List<ResourceMetadataResponse> upload(UploadRequest request);

    void deleteResource(String path);

    ResourceMetadataResponse moveOrRenameResource(MoveRenameRequest request);

    List<ResourceMetadataResponse> searchResource(String query);

    List<ResourceMetadataResponse> getDirectoryContents(String path);

    InputStream downloadResource(String path);
}
