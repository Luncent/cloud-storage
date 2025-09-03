package it.luncent.cloud_storage.minio.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import it.luncent.cloud_storage.minio.exception.MinioException;
import it.luncent.cloud_storage.minio.mapper.MinioMapper;
import it.luncent.cloud_storage.minio.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

//TODO add event listener to create bucket if it not exists when app starts

@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {
    private static final String USERS_DATA_BUCKET = "user-files";

    private final MinioClient minioClient;
    private final MinioMapper minioMapper;

    @Override
    public void createBucketForUsersData() {
        if (!bucketExists(USERS_DATA_BUCKET)) {
            createBucket(USERS_DATA_BUCKET);
        }
    }

    private void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException(e.getMessage(), e);
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName).build()
            );
        } catch (Exception ex) {
            throw new MinioException(ex.getMessage(), ex);
        }
    }

    @Override
    public ResourceMetadataResponse getResourceMetadata(String path) {
        ResourceMetadataResponse response = null;
        if(isFolder(path)){
            return minioMapper.mapToFolderResponse(path);
        }

        return null;
    }

    private boolean isFolder(String path) {
        return path.endsWith("/");
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
