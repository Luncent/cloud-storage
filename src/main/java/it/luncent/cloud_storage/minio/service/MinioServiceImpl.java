package it.luncent.cloud_storage.minio.service;

import it.luncent.cloud_storage.minio.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class MinioServiceImpl implements MinioService {


    @Override
    public void createBucket(String bucketName) {

    }

    @Override
    public ResourceMetadataResponse getResourceMetadata(String path) {
        return null;
    }

    @Override
    public void deleteResource(String path) {

    }

    @Override
    public ResourceMetadataResponse moveOrRenameResource(MoveRenameRequest request) {
        return null;
    }

    @Override
    public List<ResourceMetadataResponse> searchResource(String query) {
        return List.of();
    }

    @Override
    public List<ResourceMetadataResponse> upload(UploadRequest request) {
        return List.of();
    }

    @Override
    public List<ResourceMetadataResponse> getDirectoryContents(String path) {
        return List.of();
    }

    @Override
    public InputStream downloadResource(String path) {
        return null;
    }
}
