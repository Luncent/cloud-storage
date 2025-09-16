package it.luncent.cloud_storage.minio.service;

import io.minio.*;
import io.minio.errors.*;
import it.luncent.cloud_storage.minio.exception.MinioException;
import it.luncent.cloud_storage.minio.mapper.MinioMapper;
import it.luncent.cloud_storage.minio.model.common.ResourcePath;
import it.luncent.cloud_storage.minio.model.common.UploadingFile;
import it.luncent.cloud_storage.minio.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//TODO add event listener to create bucket if it not exists when app starts
//TODO rethink exception handling
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {
    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";
    private static final String USERS_DATA_BUCKET = "user-files";
    private static final Long FILE_SIZE_NOT_AVAILABLE = -1L;
    private static final Long FILE_SIZE_AVAILABLE = -1L;

    private static final Long MB = 1024L * 1024L;


    private final MinioClient minioClient;
    private final MinioMapper minioMapper;
    private final Tika tika;

    private final MinioService minioService;

    @Override
    public void createBucketForUsersData() {
        if (!minioService.bucketExists(USERS_DATA_BUCKET)) {
            minioService.createBucket(USERS_DATA_BUCKET);
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
                //TODO not found should be
                throw new MinioException(ex.getMessage(), ex);
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
                UploadingFile uploadingFile = UploadingFile.withKnownFileSize(request);
                uploadedResourcesNames.add(uploadFile(uploadingFile));
            }
            uploadedResourcesNames.forEach(System.out::println);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            //TODO think about exception
            throw new MinioException(e.getMessage(), e);
        }
        //TODO correct response
        return List.of();
    }

    private String uploadFile(UploadingFile uploadingFile) throws Exception {
        String relativePath = uploadingFile.targetDirectory() + uploadingFile.fileName();
        ResourcePath resourcePath = getRealResourcePath(relativePath);
        minioClient.putObject(
                buildPutObjectArgs(uploadingFile, resourcePath)
        );
        return resourcePath.relative();
    }

    private PutObjectArgs buildPutObjectArgs(UploadingFile uploadingFile, ResourcePath resourcePath) {
        if(uploadingFile.fileSize().isEmpty()){
            return PutObjectArgs.builder()
                    .bucket(USERS_DATA_BUCKET)
                    .object(resourcePath.real())
                    .contentType(uploadingFile.contentType())
                    .stream(uploadingFile.inputStream(), FILE_SIZE_NOT_AVAILABLE, 10 * MB)
                    .build();
        }
        return PutObjectArgs.builder()
                .bucket(USERS_DATA_BUCKET)
                .object(resourcePath.real())
                .contentType(uploadingFile.contentType())
                .stream(uploadingFile.inputStream(), uploadingFile.fileSize().get(), FILE_SIZE_AVAILABLE)
                .build();
    }

    private List<String> uploadArchive(UploadRequest request) {
        List<String> uploadedResourcesNames = new ArrayList<>();

        MultipartFile archive = request.file();
        try (ZipInputStream zis = new ZipInputStream(archive.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    uploadedResourcesNames.add(createEmptyDirectory(request, entry.getName()));
                    zis.closeEntry();
                    continue;
                }
                uploadedResourcesNames.add(uploadFileFromArchive(zis, entry, request));
                zis.closeEntry();
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return uploadedResourcesNames;
    }

    private String uploadFileFromArchive(ZipInputStream archiveInputStream, ZipEntry entry, UploadRequest request) throws Exception {
        String contentType = getFileContentTypeFromStream(archiveInputStream);
        UploadingFile uploadingFile = UploadingFile.withUnKnownFileSize(
                entry.getName(),
                contentType,
                request.targetDirectory(),
                archiveInputStream
        );
        return uploadFile(uploadingFile);
    }

    private String getFileContentTypeFromStream(ZipInputStream inputStream) throws IOException {
        TikaInputStream tikaInputStream = TikaInputStream.get(inputStream);
        return tika.detect(tikaInputStream);
    }

    private String createEmptyDirectory(UploadRequest request, String directoryName) {
        ResourcePath resourcePath = getRealResourcePath(request.targetDirectory() + directoryName);
        minioService.createEmptyDirectory(USERS_DATA_BUCKET, resourcePath.real());
        return resourcePath.relative();
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

