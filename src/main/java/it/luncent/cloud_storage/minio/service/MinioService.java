package it.luncent.cloud_storage.minio.service;

import it.luncent.cloud_storage.minio.model.request.ReplaceRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;

import java.util.List;

//TODO add upload resource method
public interface MinioService {

    ResourceMetadataResponse getResourceMetadata(String path);

    void deleteResource(String path);

    ResourceMetadataResponse replaceOrRenameResource(ReplaceRenameRequest request);

    List<ResourceMetadataResponse> searchResource(String query);

    List<ResourceMetadataResponse> upload(UploadRequest request);

    List<ResourceMetadataResponse> getDirectoryContents(String path);


}
