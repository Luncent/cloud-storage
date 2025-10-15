package it.luncent.cloud_storage.resource.service;

import io.minio.*;
import it.luncent.cloud_storage.storage.service.StorageService;
import it.luncent.cloud_storage.security.service.AuthService;
import it.luncent.cloud_storage.resource.mapper.ResourceMapper;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.common.UploadingFile;
import it.luncent.cloud_storage.resource.model.request.MoveRenameRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
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
public class ResourceServiceImpl implements ResourceService {
    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";
    private static final Long FILE_SIZE_NOT_AVAILABLE = -1L;
    private static final Long FILE_SIZE_AVAILABLE = -1L;
    private static final Long MB = 1024L * 1024L;

    private final AuthService authService;
    private final ResourceMapper resourceMapper;
    private final Tika tika;
    private final StorageService storageService;
    @Value("${minio.users-bucket}")
    private String usersBucket;

    //TODO think about exception handling (convert minio exceptions)
    @Override
    public ResourceMetadataResponse getResourceMetadata(String relativePath) {
        ResourcePath resourcePath = getFullResourcePath(relativePath);
        if (isDirectory(relativePath)) {
            storageService.checkDirectoryExistence(usersBucket, resourcePath.full());
            return resourceMapper.mapToFolderResponse(resourcePath);
        }
        StatObjectResponse objectMetadata = storageService.getObject(usersBucket, resourcePath.full());
        return resourceMapper.mapToFileResponse(resourcePath, objectMetadata);
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
    public ResourceMetadataResponse createDirectory(String path) {
        return getResourceMetadata(createEmptyDirectory(path));
    }

    @Override
    public List<ResourceMetadataResponse> getDirectoryContents(String path) {
        return List.of();
    }

    @Override
    public InputStream downloadResource(String path) {
        return null;
    }

    private ResourcePath getFullResourcePath(String relativePath) {
        Long currentUserId = authService.getCurrentUser().id();
        String realPath = String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, relativePath);
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

    private String uploadFile(UploadingFile uploadingFile){
        ResourcePath resourcePath = getFullResourcePath(uploadingFile.relativePath());
        storageService.uploadFile(
                usersBucket,
                resourcePath.full(),
                uploadingFile.inputStream(),
                uploadingFile.contentType()
        );
        return resourcePath.relative();
    }

    private PutObjectArgs buildPutObjectArgs(UploadingFile uploadingFile, ResourcePath resourcePath) {
        if (uploadingFile.fileSize().isEmpty()) {
            return PutObjectArgs.builder()
                    .bucket(usersBucket)
                    .object(resourcePath.full())
                    .contentType(uploadingFile.contentType())
                    .stream(uploadingFile.inputStream(), FILE_SIZE_NOT_AVAILABLE, 10 * MB)
                    .build();
        }
        return PutObjectArgs.builder()
                .bucket(usersBucket)
                .object(resourcePath.full())
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
                    uploadedResourcesNames.add(createEmptyDirectory(request.targetDirectory() + entry.getName()));
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
                request.targetDirectory()+entry.getName(),
                contentType,
                bufferedInputStream
        );
        return uploadFile(uploadingFile);
    }

    private String createEmptyDirectory(String path) {
        ResourcePath resourcePath = getFullResourcePath(path);
        storageService.createEmptyDirectory(usersBucket, resourcePath.full());
        return resourcePath.relative();
    }

    private boolean isArchive(UploadRequest request) {
        return !request.file().getResource().isFile();
    }

}

