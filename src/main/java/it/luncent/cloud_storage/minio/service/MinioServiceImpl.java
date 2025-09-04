package it.luncent.cloud_storage.minio.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import it.luncent.cloud_storage.minio.exception.MinioException;
import it.luncent.cloud_storage.minio.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.minio.mapper.MinioMapper;
import it.luncent.cloud_storage.minio.model.ResourcePath;
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
    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";
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

    //TODO make method for exception handling + strategy for converting minio exceptions to custom ones
    @Override
    public ResourceMetadataResponse getResourceMetadata(String relativePath) {
        ResourcePath resourcePath = getRealResourcePath(relativePath);
        if(isFolder(relativePath)){
            return minioMapper.mapToFolderResponse(resourcePath);
        }
        try {
            StatObjectResponse objectMetadata = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(USERS_DATA_BUCKET)
                            .object(resourcePath.real())
                            .build()
            );
            return minioMapper.mapToFileResponse(resourcePath, objectMetadata);
        }catch (ErrorResponseException ex){
            if(ex.errorResponse().code().equals("NoSuchKey")){
                throw new ResourceNotFoundException(ex.getMessage(), ex);
            }
            throw new MinioException(ex.getMessage(), ex);
        }
        catch (Exception ex){
            throw new MinioException(ex.getMessage(), ex);
        }
    }
    //TODO get userId from SecurityContext
    private ResourcePath getRealResourcePath(String relativePath) {
        String realPath = String.format(USER_RESOURCE_PATH_TEMPLATE, 1, relativePath);
        return new ResourcePath(relativePath, realPath);
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
