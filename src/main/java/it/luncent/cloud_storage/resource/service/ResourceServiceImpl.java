package it.luncent.cloud_storage.resource.service;

import it.luncent.cloud_storage.resource.directory.service.DirectoryService;
import it.luncent.cloud_storage.resource.exception.ReservedNameException;
import it.luncent.cloud_storage.resource.file.service.FileService;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.DIRECTORY_SUFFIX;
import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final DirectoryService directoryService;
    private final FileService fileService;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        if (PathUtils.isDirectory(path)) {
            return directoryService.getMetadata(path);
        }
        if (PathUtils.isMarker(path)) {
            throw new ReservedNameException(EMPTY_DIRECTORY_MARKER);
        }
        return fileService.getMetadata(path);
    }

    @Override
    public void deleteResource(String path) {
        if (PathUtils.isDirectory(path)) {
            directoryService.delete(path);
            return;
        }
        if (PathUtils.isMarker(path)) {
            throw new ReservedNameException(EMPTY_DIRECTORY_MARKER);
        }
        fileService.delete(path);
    }

    @Override
    public void downloadResource(OutputStream outputStream, String path) {
        if (PathUtils.isDirectory(path)) {
            directoryService.download(outputStream, path);
            return;
        }
        if (PathUtils.isMarker(path)) {
            throw new ReservedNameException(EMPTY_DIRECTORY_MARKER);
        }
        fileService.download(outputStream, path);
    }

    @Override
    public ResourceMetadataResponse moveResource(MoveRequest request) {
        if (PathUtils.isDirectory(request.from())) {
            return directoryService.move(request);
        }
        if (PathUtils.isMarker(request.to()) || PathUtils.isMarker(request.from())) {
            throw new ReservedNameException(EMPTY_DIRECTORY_MARKER);
        }
        return fileService.move(request);
    }

    @Override
    public List<ResourceMetadataResponse> search(String query) {
        Set<String> paths = directoryService
                .getAllContents(PathUtils.getAbsolutePath(DIRECTORY_SUFFIX));
        return StringUtils.isBlank(query) ?
                paths.stream()
                        .map(this::getMetadata)
                        .toList() :
                paths.stream()
                        .filter(path -> resourceNameMatchesQuery(path, query))
                        .map(this::getMetadata)
                        .toList();
    }

    @Override
    public List<ResourceMetadataResponse> upload(UploadRequest request) {
        if (PathUtils.isMarker(request.file().getResource().getFilename())) {
            throw new ReservedNameException(EMPTY_DIRECTORY_MARKER);
        }
        ResourceMetadataResponse fileUploadResponse = uploadFile(request);
        List<String> uploadedResourcesRelativePaths = createNestedDirectories(request);
        List<ResourceMetadataResponse> resourcesMetadata = uploadedResourcesRelativePaths.stream()
                .map(this::getMetadata)
                .collect(toList());
        resourcesMetadata.add(fileUploadResponse);
        return resourcesMetadata;
    }

    @Override
    public List<ResourceMetadataResponse> upload(List<UploadRequest> requests) {
        return requests.stream()
                .map(this::upload)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private List<String> createNestedDirectories(UploadRequest request) {
        List<String> directoriesToCreate = getDirectoriesRelativePaths(request).stream()
                .filter(directoryRelativePath -> !directoryService.exists(directoryRelativePath))
                .collect(toList());
        directoriesToCreate.forEach(directoryService::createEmptyDirectory);
        return directoriesToCreate;
    }

    private List<String> getDirectoriesRelativePaths(UploadRequest request) {
        @SuppressWarnings("ConstantConditions")
        List<String> directoriesNames = new LinkedList<>();
        String fileName = request.file().getResource().getFilename();
        int lastSlashIndex;
        while ((lastSlashIndex = fileName.lastIndexOf('/')) != -1) {
            fileName = fileName.substring(0, lastSlashIndex);
            directoriesNames.add(fileName.substring(0, lastSlashIndex));
        }
        return directoriesNames.stream()
                .map(name -> request.targetDirectory() + name + "/")
                .collect(Collectors.toList());
    }

    private boolean resourceNameMatchesQuery(String resourceName, String query) {
        if (PathUtils.isDirectory(resourceName)) {
            int length = resourceName.length();
            int penultimateSlashIndex = resourceName.lastIndexOf('/', length - 2);
            String directoryName = resourceName.substring(penultimateSlashIndex + 1);
            return Strings.CI.contains(directoryName, query);
        }
        int lastSlashIndex = resourceName.lastIndexOf('/');
        String fileName = resourceName.substring(lastSlashIndex + 1);
        return Strings.CI.contains(fileName, query);
    }

    private ResourceMetadataResponse uploadFile(UploadRequest request) {
        try {
            MultipartFile fileToUpload = request.file();
            String fileName = fileToUpload.getResource().getFilename();
            String contentType = fileToUpload.getContentType();
            String path = request.targetDirectory() + fileName;
            return fileService.upload(fileToUpload.getInputStream(), path, contentType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

