package it.luncent.cloud_storage.minio.service;

import it.luncent.cloud_storage.minio.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;

import java.io.InputStream;
import java.util.List;

public interface MinioService {

    void createBucketForUsersData();

    ResourceMetadataResponse getResourceMetadata(String path);

    void deleteResource(String path);

    ResourceMetadataResponse moveOrRenameResource(MoveRenameRequest request);

    List<ResourceMetadataResponse> searchResource(String query);

    List<ResourceMetadataResponse> upload(UploadRequest request);

    List<ResourceMetadataResponse> getDirectoryContents(String path);

    InputStream downloadResource(String path);
}
