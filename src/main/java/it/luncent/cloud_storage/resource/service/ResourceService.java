package it.luncent.cloud_storage.resource.service;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;

import java.io.OutputStream;
import java.util.List;

public interface ResourceService {

    ResourceMetadataResponse createDirectory(String path);

    void deleteResource(String path);

    void downloadResource(OutputStream outputStream, String path);

    List<ResourceMetadataResponse> getDirectoryContents(String path);

    ResourceMetadataResponse getResourceMetadata(String path);

    ResourceMetadataResponse moveResource(MoveRequest request);

    List<ResourceMetadataResponse> searchResource(String query);

    //TODO temporary file max size. Why we need to set it. Check file name before upload, maybe add more symbols for correct path regexp
    List<ResourceMetadataResponse> upload(UploadRequest request);
}
