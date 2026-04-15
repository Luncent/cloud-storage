package it.luncent.cloud_storage.resource.service;

import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationSettings;
import it.luncent.cloud_storage.resource.directory.service.DirectoryService;
import it.luncent.cloud_storage.resource.exception.ConflictException;
import it.luncent.cloud_storage.resource.file.service.FileService;
import it.luncent.cloud_storage.resource.model.common.Path;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.ROOT_DIRECTORY;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.resourceIsInRootDirectory;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private static final String OBJECT_EXISTS_TEMPLATE = "conflict %s already exists";

    private final ResourcePathUtil resourcePathUtil;
    private final StorageService storageService;
    private final DirectoryService directoryService;
    private final FileService fileService;

    @Override
    public ResourceMetadataResponse getMetadata(String relativePath) {
        if (isDirectory(relativePath)) {
            return directoryService.getMetadata(relativePath);
        }
        return fileService.getMetadata(relativePath);
    }

    @Override
    public void deleteResource(String relativePath) {
        if (isDirectory(relativePath)) {
            directoryService.delete(relativePath);
            return;
        }
        fileService.delete(relativePath);
    }

    @Override
    public void downloadResource(OutputStream outputStream, String path) {
        if (isDirectory(path)) {
            directoryService.download(outputStream, path);
            return;
        }
        fileService.download(outputStream, path);
    }

    @Override
    public ResourceMetadataResponse moveResource(MoveRequest request) {
        if (isDirectory(request.from())) {
            return directoryService.move(request);
        }
        return fileService.move(request);
    }

    @Override
    public List<ResourceMetadataResponse> search(String query) {
        ResourcePath rootDirectory = resourcePathUtil.getResourcePathFromRelative(ROOT_DIRECTORY);
        List<Item> objects = new ArrayList<>();
        PopulationSettings populationSettings = PopulationSettings.builder()
                .includeDirectories(true)
                .includeMarkers(false)
                .includeFiles(true)
                .build();
        storageService.populateWithDirectoryObjectsAsync(rootDirectory, objects, populationSettings);

        return StringUtils.isBlank(query) ?
                objects.stream()
                        .map(object -> resourcePathUtil.getRelativePath(object.objectName()))
                        .map(this::getMetadata)
                        .toList() :
                objects.stream()
                        .filter(object -> resourceNameMatchesQuery(object.objectName(), query))
                        .map(object -> resourcePathUtil.getRelativePath(object.objectName()))
                        .map(this::getMetadata)
                        .toList();
    }

    @Override
    @SneakyThrows
    public List<ResourceMetadataResponse> upload(UploadRequest request) {
        ResourceMetadataResponse response = uploadFile(request);
        List<String> uploadedResourcesRelativePaths = createNestedDirectories(request);
        List<ResourceMetadataResponse> resourceMetadataResponses = uploadedResourcesRelativePaths.stream()
                .map(this::getMetadata)
                .collect(toList());
        resourceMetadataResponses.add(response);
        return resourceMetadataResponses;
    }

    @Override
    public List<ResourceMetadataResponse> upload(List<UploadRequest> requests) {
        return requests.stream()
                .map(this::upload)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    //TODO не создаются
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

    private void checkCollisions(String... filePaths) {
        List<String> errors = new ArrayList<>();
        for (String targetObjectFullPath : filePaths) {
            if (isDirectory(targetObjectFullPath)) {
                continue;
            }
            if (fileExists(targetObjectFullPath)) {
                errors.add(String.format(OBJECT_EXISTS_TEMPLATE, resourcePathUtil.getRelativePath(targetObjectFullPath)));
            }
        }
        if (!errors.isEmpty()) {
            throw new ConflictException(String.join(", ", errors));
        }
    }

    private boolean fileExists(String objectFullPath) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromAbsolute(objectFullPath);
        try {
            storageService.getObjectMetadata(resourcePath);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private boolean resourceNameMatchesQuery(String resourceName, String query) {
        if (isDirectory(resourceName)) {
            int length = resourceName.length();
            int penultimateSlashIndex = resourceName.lastIndexOf('/', length - 2);
            if (resourceIsInRootDirectory(penultimateSlashIndex)) {
                return Strings.CI.contains(resourceName, query);
            }
            String directoryName = resourceName.substring(penultimateSlashIndex + 1);
            return Strings.CI.contains(directoryName, query);
        }
        int lastSlashIndex = resourceName.lastIndexOf('/');
        if (resourceIsInRootDirectory(lastSlashIndex)) {
            return Strings.CI.contains(resourceName, query);
        }
        String fileName = resourceName.substring(lastSlashIndex + 1);
        return Strings.CI.contains(fileName, query);
    }

    private ResourceMetadataResponse uploadFile(UploadRequest request) {
        try {
            MultipartFile fileToUpload = request.file();
            String fileName = request.file().getResource().getFilename();
            String contentType = fileToUpload.getContentType();
            Path path = new Path(request.targetDirectory(), fileName);
            return fileService.upload(fileToUpload.getInputStream(), path, contentType);
        } catch (Exception e) {
            //TODO make method to handle minioException and throw corresponding one
            throw new RuntimeException(e);
        }
    }

}

