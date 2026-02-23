package it.luncent.cloud_storage.resource.directory.service;

import io.minio.Result;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationSettings;
import it.luncent.cloud_storage.common.util.ObjectStorageUtil;
import it.luncent.cloud_storage.resource.directory.mapper.DirectoryMapper;
import it.luncent.cloud_storage.resource.exception.ConflictException;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import it.luncent.cloud_storage.storage.service.ArchiveService;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    private static final String DIRECTORY_EXISTS_TEMPLATE = "directory %s already exists";
    private static final String FILE_EXISTS_TEMPLATE = "file %s already exists";
    private static final Integer FILE_SIZE_IS_KNOWN = -1;
    private static final Integer EMPTY_FOLDER_SIZE = 0;

    private final ArchiveService archiveService;
    private final StorageService storageService;
    private final ResourcePathUtil resourcePathUtil;
    private final DirectoryMapper directoryMapper;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        storageService.getDirectoryContent(resourcePath);
        return directoryMapper.mapToResponse(resourcePath.relative());
    }

    @Override
    public List<ResourceMetadataResponse> getContents(String path) {
        Iterable<Result<Item>> directoryObjects = getDirectoryContent(path);
        List<ResourceMetadataResponse> directoryContents = new ArrayList<>();
        for (Result<Item> directoryObject : directoryObjects) {
            Item item = getItem(directoryObject);
            if (!ObjectStorageUtil.isMarker(item.objectName())) {
                directoryContents.add(getResourceMetadata(item));
            }
        }
        return directoryContents;
    }

    @Override
    public ResourceMetadataResponse createEmptyDirectory(String relativePath) {
        if (directoryExists(relativePath)) {
            throw new ConflictException(String.format(DIRECTORY_EXISTS_TEMPLATE, relativePath));
        }
        ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(relativePath);
        storageService.uploadFile(directoryPath, new ByteArrayInputStream(new byte[EMPTY_FOLDER_SIZE]));
        return getMetadata(directoryPath.relative());
    }

    @Override
    public void delete(String path) {
        List<Item> objects = new ArrayList<>();
        PopulationSettings includeFilesAndMarkers = new PopulationSettings(false, true, true);
        ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(path);
        storageService.populateWithDirectoryObjectsAsync(directoryPath, objects, includeFilesAndMarkers);
        List<String> objectNames = objects.stream()
                .map(Item::objectName)
                .toList();
        storageService.deleteFilesBatch(directoryPath.bucketName(), objectNames);
    }

    @Override
    public void download(OutputStream outputStream, String path) {
        //TODO add check for existence
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        archiveService.downloadArchiveAsync(resourcePath, outputStream);
    }

    @Override
    public ResourceMetadataResponse move(MoveRequest request) {
        ResourcePath sourceFolder = resourcePathUtil.getResourcePathFromRelative(request.from());
        moveDirectory(request, sourceFolder);
        String newDirectoryAbsolutePath = getFullTargetPath(sourceFolder.absolute(), request);
        String newDirectoryRelativePath = resourcePathUtil.getRelativePath(newDirectoryAbsolutePath);
        return getMetadata(newDirectoryRelativePath);
    }

    private Iterable<Result<Item>> getDirectoryContent(String path) {
        try {
            return storageService.getDirectoryContent(resourcePathUtil.getResourcePathFromRelative(path));
        } catch (ResourceNotFoundException e) {
            if (path.isEmpty()) {
                return List.of();
            }
            throw e;
        }
    }

    private Item getItem(Result<Item> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private ResourceMetadataResponse getResourceMetadata(Item item) {
        String path = item.objectName();
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromAbsolute(path);
        if (isDirectory(path)) {
            return directoryMapper.mapToResponse(resourcePath.relative());
        }
        return directoryMapper.mapToFileResponse(resourcePath, item);
    }

    private boolean directoryExists(String relativeObjectPath) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(relativeObjectPath);
        try {
            Iterable<Result<Item>> results = storageService.getDirectoryContent(resourcePath);
            for (Result<Item> result : results) {
                if (result.get().objectName().endsWith(EMPTY_DIRECTORY_MARKER)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void moveDirectory(MoveRequest request, ResourcePath sourcePath) {
        List<Item> objects = new ArrayList<>();
        PopulationSettings populationSettings = PopulationSettings.builder()
                .includeDirectories(true)
                .includeMarkers(false)
                .includeFiles(true)
                .build();
        storageService.populateWithDirectoryObjectsAsync(sourcePath, objects, populationSettings);
        Set<String> sourceObjectsFullPaths = objects.stream()
                .map(Item::objectName)
                .collect(Collectors.toSet());
        sourceObjectsFullPaths.add(sourcePath.absolute());
        checkCollisions((path) -> getFullTargetPath(path, request), sourceObjectsFullPaths.toArray(new String[0]));
        sourceObjectsFullPaths.forEach(objectFullPath -> copyObject(objectFullPath, request));
        delete(sourcePath.relative());
    }

    private void checkCollisions(String from, String to, String... objects) {
        List<String> errors = new ArrayList<>();
        for (String sourceObjectFullPath : objects) {
            if (isDirectory(sourceObjectFullPath)) {
                continue;
            }
            String fullObjectTargetPath = getFullTargetPath(sourceObjectFullPath, from, to);
            if (fileExists(fullObjectTargetPath)) {
                errors.add(String.format(FILE_EXISTS_TEMPLATE, resourcePathUtil.getRelativePath(fullObjectTargetPath)));
            }
        }

        if (!errors.isEmpty()) {
            throw new ConflictException(String.join(", ", errors));
        }
    }

    private String getFullTargetPath(String sourceFullPath, String from, String to) {
        String sourcePathPrefix = resourcePathUtil.getResourcePathFromRelative(from).absolute();
        String targetPathPrefix = resourcePathUtil.getResourcePathFromRelative(to).absolute();
        return targetPathPrefix + (sourceFullPath.substring(sourcePathPrefix.length()));
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
}
