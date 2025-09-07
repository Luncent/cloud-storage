package it.luncent.cloud_storage.minio.service;

import io.minio.*;
import io.minio.errors.*;
import it.luncent.cloud_storage.minio.exception.MinioException;
import it.luncent.cloud_storage.minio.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.minio.mapper.MinioMapper;
import it.luncent.cloud_storage.minio.model.ResourcePath;
import it.luncent.cloud_storage.minio.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//TODO add event listener to create bucket if it not exists when app starts
//TODO rethink exception handling
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {
    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";
    private static final String USERS_DATA_BUCKET = "user-files";
    private static final String EMPTY_FOLDER_TAG = "empty-folder-tag";
    private static final Long MB = 1024L * 1024L;
    private static final Integer FILE_SIZE_NOT_AVAILABLE = -1;

    private final MinioClient minioClient;
    private final MinioMapper minioMapper;
    private final Tika tika;

    @Override
    public void createBucketForUsersData() {
        if (!bucketExists()) {
            createBucket();
        }
    }

    private void createBucket() {
        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(USERS_DATA_BUCKET)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException(e.getMessage(), e);
        }
    }

    private boolean bucketExists() {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(MinioServiceImpl.USERS_DATA_BUCKET).build()
            );
        } catch (Exception ex) {
            throw new MinioException(ex.getMessage(), ex);
        }
    }

    //TODO make method for exception handling + strategy for converting minio exceptions to custom ones
    @Override
    public ResourceMetadataResponse getResourceMetadata(String relativePath) {
        ResourcePath resourcePath = getRealResourcePath(relativePath);
        if (isFolder(relativePath)) {
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
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse().code().equals("NoSuchKey")) {
                throw new ResourceNotFoundException(ex.getMessage(), ex);
            }
            throw new MinioException(ex.getMessage(), ex);
        } catch (Exception ex) {
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
        try {
            List<String> uploadedResourcesNames = new ArrayList<>();
            if (isArchive(request)) {
                uploadedResourcesNames.addAll(uploadArchive(request));
            } else {
                String fileName = request.file().getName();
                String contentType = request.file().getContentType();
                uploadedResourcesNames.add(uploadFile(request.file().getInputStream(), fileName, contentType, request.targetDirectory()));
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            //TODO think about exception
            throw new MinioException(e.getMessage(), e);
        }
        //TODO correct response
        return List.of();
    }

    private String uploadFile(InputStream inputStream, String fileName, String contentType, String targetDirectory) throws Exception {
        ResourcePath resourcePath = getRealResourcePath(targetDirectory + fileName);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(USERS_DATA_BUCKET)
                        .object(resourcePath.real())
                        .contentType(contentType)
                        .stream(inputStream, FILE_SIZE_NOT_AVAILABLE, 10 * MB)
                        .build()
        );
        return resourcePath.relative();
    }

    private List<String> uploadArchive(UploadRequest request) {
        List<String> uploadedResourcesNames = new ArrayList<>();

        MultipartFile archive = request.file();
        try (ZipInputStream zis = new ZipInputStream(archive.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    //TODO check entryname ends on "/"
                    ResourcePath resourcePath = getRealResourcePath(request.targetDirectory() + entry.getName());
                    uploadedResourcesNames.add(mkDir(resourcePath.real()));
                    zis.closeEntry();
                    continue;
                }
                //TODO check mark feature, or does it detect type without bad consequences? (it reads from is so later i get 0. Need to read it all in byte array or find out workaround)
                //String contentType = tika.detect(zis);
                String contentType = "text/plain";

                //TODO get content type, size and refactoring
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                byte[] fileData = baos.toByteArray();

                zis.closeEntry();
                uploadedResourcesNames.add(uploadFile(new ByteArrayInputStream(fileData), entry.getName(), contentType, request.targetDirectory()));
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return uploadedResourcesNames;
    }

    //TODO check if u can put empty object like that
    private String mkDir(String directoryPath) throws Exception{
        ObjectWriteResponse response = minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(USERS_DATA_BUCKET)
                        .object(directoryPath + EMPTY_FOLDER_TAG)
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build()
        );
        return directoryPath;
    }

    private boolean isArchive(UploadRequest request) {
        return !request.file().getResource().isFile();
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
