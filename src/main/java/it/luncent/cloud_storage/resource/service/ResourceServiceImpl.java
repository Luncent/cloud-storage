package it.luncent.cloud_storage.resource.service;

import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.resource.exception.DownloadException;
import it.luncent.cloud_storage.resource.mapper.ResourceMapper;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.common.UploadingFile;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.service.ArchiveService;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static it.luncent.cloud_storage.storage.util.StorageUtil.isDirectory;
import static java.util.stream.Collectors.toList;

//TODO rethink exception handling
//TODO get bucket name for users from properties files
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {
    private final ResourcePathUtil resourcePathUtil;
    private final ResourceMapper resourceMapper;
    private final Tika tika;
    private final StorageService storageService;
    private final ArchiveService archiveService;

    @Override
    public ResourceMetadataResponse createDirectory(String path) {
        return getResourceMetadata(createEmptyDirectory(resourcePathUtil.getResourcePathFromRelative(path)));
    }

    @Override
    public void deleteResource(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        if (isDirectory(path)) {
            storageService.deleteDirectory(resourcePath);
            return;
        }
        storageService.deleteFile(resourcePath);
    }

    @Override
    public void downloadResource(OutputStream outputStream, String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        if (isDirectory(path)) {
            archiveService.downloadArchiveAsync(resourcePath, outputStream);
            return;
        }
        //TODO add check for existence
        InputStream fileInputStream = storageService.downloadFile(resourcePath);
        writeFileInputStreamToOutputStream(fileInputStream, outputStream);
    }

    private void writeFileInputStreamToOutputStream(InputStream fileInputStream, OutputStream outputStream) {
        try (BufferedInputStream bis = new BufferedInputStream(fileInputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
            bis.transferTo(bufferedOutputStream);
        } catch (Exception e) {
            throw new DownloadException(e.getMessage(), e);
        }
    }

    @Override
    public List<ResourceMetadataResponse> getDirectoryContents(String path) {
        return List.of();
    }

    //TODO think about exception handling (convert minio exceptions)
    @Override
    public ResourceMetadataResponse getResourceMetadata(String relativePath) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(relativePath);
        if (isDirectory(relativePath)) {
            storageService.getDirectoryContent(resourcePath);
            return resourceMapper.mapToFolderResponse(resourcePath.relative());
        }
        StatObjectResponse objectMetadata = storageService.getObjectMetadata(resourcePath);
        return resourceMapper.mapToFileResponse(resourcePath, objectMetadata);
    }

    //TODO add validation on controller level
    @Override
    public ResourceMetadataResponse moveResource(MoveRequest request) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(request.from());

        if(isDirectory(request.from())) {
            Iterable<Result<Item>> objects = storageService.getDirectoryContent(resourcePath);
            moveDirectory(objects, resourcePath, request);
            return null;
        }

        return null;
    }

    private void moveDirectory(Iterable<Result<Item>> objects, ResourcePath resourcePath, MoveRequest request) {
        ResourcePath newFolderPath = resourcePathUtil.getResourcePathFromRelative(request.to());
        createEmptyDirectory(newFolderPath);
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

    private String uploadFileResource(UploadRequest request) {
        try {
            UploadingFile uploadingFile = UploadingFile.withKnownFileSize(request);
            return uploadFile(uploadingFile);
        } catch (Exception e) {
            //TODO make method to handle minioException and throw corresponding one
            throw new RuntimeException(e);
        }
    }

    private String uploadFile(UploadingFile uploadingFile) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(uploadingFile.relativePath());
        storageService.uploadFile(
                resourcePath,
                uploadingFile.inputStream(),
                uploadingFile.contentType()
        );
        return resourcePath.relative();
    }

    /*private PutObjectArgs buildPutObjectArgs(UploadingFile uploadingFile, ResourcePath resourcePath) {
        if (uploadingFile.fileSize().isEmpty()) {
            return PutObjectArgs.builder()
                    .bucket(usersBucket)
                    .object(resourcePath.absolute())
                    .contentType(uploadingFile.contentType())
                    .stream(uploadingFile.inputStream(), FILE_SIZE_NOT_AVAILABLE, 10 * MB)
                    .build();
        }
        return PutObjectArgs.builder()
                .bucket(usersBucket)
                .object(resourcePath.absolute())
                .contentType(uploadingFile.contentType())
                .stream(uploadingFile.inputStream(), uploadingFile.fileSize().get(), FILE_SIZE_AVAILABLE)
                .build();
    }*/

    private List<String> uploadArchive(UploadRequest request) {
        List<String> uploadedResourcesNames = new ArrayList<>();

        MultipartFile archive = request.file();
        try (ZipInputStream zis = new ZipInputStream(archive.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    uploadedResourcesNames.add(createEmptyDirectory(resourcePathUtil.getResourcePathFromRelative(request.targetDirectory() + entry.getName())));
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
                request.targetDirectory() + entry.getName(),
                contentType,
                bufferedInputStream
        );
        return uploadFile(uploadingFile);
    }

    private String createEmptyDirectory(ResourcePath path) {
        storageService.createEmptyDirectory(path);
        return path.relative();
    }

    private boolean isArchive(UploadRequest request) {
        return !request.file().getResource().isFile();
    }

}

