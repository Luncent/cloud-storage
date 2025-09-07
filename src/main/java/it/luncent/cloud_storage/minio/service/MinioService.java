package it.luncent.cloud_storage.minio.service;

import it.luncent.cloud_storage.minio.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;

import java.io.InputStream;
import java.util.List;

public interface MinioService {

    //implemented
    void createBucketForUsersData();

    //implemented but need extra work
    ResourceMetadataResponse getResourceMetadata(String path);

    List<ResourceMetadataResponse> upload(UploadRequest request);

    void deleteResource(String path);

    ResourceMetadataResponse moveOrRenameResource(MoveRenameRequest request);

    List<ResourceMetadataResponse> searchResource(String query);

    List<ResourceMetadataResponse> getDirectoryContents(String path);

    InputStream downloadResource(String path);
}
