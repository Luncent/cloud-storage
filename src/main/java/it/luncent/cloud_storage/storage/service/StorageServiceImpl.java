package it.luncent.cloud_storage.storage.service;

import io.minio.*;
import io.minio.errors.*;
import it.luncent.cloud_storage.config.properties.MinioProperties;
import it.luncent.cloud_storage.minio.exception.MinioException;
import it.luncent.cloud_storage.minio.service.MinioService;
import it.luncent.cloud_storage.storage.mapper.ResourceMapper;
import it.luncent.cloud_storage.storage.model.common.ResourcePath;
import it.luncent.cloud_storage.storage.model.common.UploadingFile;
import it.luncent.cloud_storage.storage.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.storage.model.request.UploadRequest;
import it.luncent.cloud_storage.storage.model.response.ResourceMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

//TODO rethink exception handling
//TODO get bucket name for users from properties files
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {
    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";
    private static final Long FILE_SIZE_NOT_AVAILABLE = -1L;
    private static final Long FILE_SIZE_AVAILABLE = -1L;
    private static final Long MB = 1024L * 1024L;

    private final MinioClient minioClient;
    private final ResourceMapper resourceMapper;
    private final Tika tika;
    private final MinioService minioService;
    private final MinioProperties minioProperties;

    //TODO make method for exception handling + strategy for converting minio exceptions to custom ones
    @Override
    public ResourceMetadataResponse getResourceMetadata(String relativePath) {
        ResourcePath resourcePath = getResourcePath(relativePath);
        if (isDirectory(relativePath)) {
            //bad - should check existence
            return resourceMapper.mapToFolderResponse(resourcePath);
        }
        try {
            StatObjectResponse objectMetadata = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.usersBucket())
                            .object(resourcePath.real())
                            .build()
            );
            return resourceMapper.mapToFileResponse(resourcePath, objectMetadata);
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

    @Override
    public void deleteResource(String path) {
        if (isDirectory(path)) {
            deleteDirectory(path);
            return;
        }
        deleteFile(path);
    }

    private void deleteDirectory(String path) {
    }

    private void deleteFile(String path) {
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
        List<String> uploadedResourcesNames = new ArrayList<>();
        if (isArchive(request)) {
            //TODO check should get relative path
            uploadedResourcesNames.addAll(uploadArchive(request));
        } else {
            //TODO check should get relative path
            uploadedResourcesNames.add(uploadFileResource(request));
        }
        return uploadedResourcesNames.stream()
                .map(this::getResourceMetadata)
                .collect(toList());
    }

    @Override
    public List<ResourceMetadataResponse> getDirectoryContents(String path) {
        return List.of();
    }

    @Override
    public InputStream downloadResource(String path) {
        return null;
    }

    //TODO get userId from SecurityContext
    private ResourcePath getResourcePath(String relativePath) {
        String realPath = String.format(USER_RESOURCE_PATH_TEMPLATE, 1, relativePath);
        return new ResourcePath(relativePath, realPath);
    }

    private boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    private String uploadFileResource(UploadRequest request) {
        try {
            UploadingFile uploadingFile = UploadingFile.withKnownFileSize(request);
            return uploadFile(uploadingFile);
        } catch (Exception e) {
            //TODO make method to handle minioException and throw corresponding one
            throw new RuntimeException(e);
        }
    }

    private String uploadFile(UploadingFile uploadingFile) throws Exception {
        String relativePath = uploadingFile.targetDirectory() + uploadingFile.fileName();
        ResourcePath resourcePath = getResourcePath(relativePath);
        minioClient.putObject(
                buildPutObjectArgs(uploadingFile, resourcePath)
        );
        return resourcePath.relative();
    }

    private PutObjectArgs buildPutObjectArgs(UploadingFile uploadingFile, ResourcePath resourcePath) {
        if (uploadingFile.fileSize().isEmpty()) {
            return PutObjectArgs.builder()
                    .bucket(minioProperties.usersBucket())
                    .object(resourcePath.real())
                    .contentType(uploadingFile.contentType())
                    .stream(uploadingFile.inputStream(), FILE_SIZE_NOT_AVAILABLE, 10 * MB)
                    .build();
        }
        return PutObjectArgs.builder()
                .bucket(minioProperties.usersBucket())
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
        BufferedInputStream bufferedInputStream = new BufferedInputStream(archiveInputStream);
        String contentType = tika.detect(bufferedInputStream);
        UploadingFile uploadingFile = UploadingFile.withUnKnownFileSize(
                entry.getName(),
                contentType,
                request.targetDirectory(),
                bufferedInputStream
        );
        return uploadFile(uploadingFile);
    }

    private String createEmptyDirectory(UploadRequest request, String directoryName) {
        ResourcePath resourcePath = getResourcePath(request.targetDirectory() + directoryName);
        minioService.createEmptyDirectory(minioProperties.usersBucket(), resourcePath.real());
        return resourcePath.relative();
    }

    private boolean isArchive(UploadRequest request) {
        return !request.file().getResource().isFile();
    }

}

