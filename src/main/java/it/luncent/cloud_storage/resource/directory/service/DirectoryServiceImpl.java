package it.luncent.cloud_storage.resource.directory.service;

import io.minio.Result;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationSettings;
import it.luncent.cloud_storage.common.util.ObjectStorageUtil;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryNotFoundException;
import it.luncent.cloud_storage.resource.directory.mapper.DirectoryMapper;
import it.luncent.cloud_storage.resource.exception.ConflictException;
import it.luncent.cloud_storage.resource.file.service.FileService;
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

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    public static final String EMPTY_DIRECTORY_MARKER = "empty-folder-tag";
    private static final String DIRECTORY_EXISTS_TEMPLATE = "directory %s already exists";
    private static final String DIRECTORY_NOT_FOUND_TEMPLATE = "directory %s already exists";
    private static final String FILE_EXISTS_TEMPLATE = "file %s already exists";
    private static final Integer EMPTY_FOLDER_SIZE = 0;

    private final ArchiveService archiveService;
    private final StorageService storageService;
    private final FileService fileService;
    private final ResourcePathUtil resourcePathUtil;
    private final DirectoryMapper directoryMapper;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        if (exists(path)) {
            throw new DirectoryNotFoundException(String.format(DIRECTORY_NOT_FOUND_TEMPLATE, path));
        }
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
        if (exists(relativePath)) {
            throw new ConflictException(String.format(DIRECTORY_NOT_FOUND_TEMPLATE, relativePath));
        }
        return directoryMapper.mapToResponse(createEmptyDirectoryIgnoringConflicts(relativePath));
    }

    private String createEmptyDirectoryIgnoringConflicts(String relativePath) {
        ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(relativePath + EMPTY_DIRECTORY_MARKER);
        storageService.uploadFile(directoryPath, new ByteArrayInputStream(new byte[EMPTY_FOLDER_SIZE]));
        return relativePath;
    }

    @Override
    public void delete(String path) {
        List<Item> objects = new ArrayList<>();
        PopulationSettings includeFilesAndMarkers = new PopulationSettings(false, true, true);
        ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(path);
        storageService.populateWithDirectoryObjectsAsync(directoryPath, objects, includeFilesAndMarkers);
        Set<String> objectNames = objects.stream()
                .map(Item::objectName)
                .collect(Collectors.toSet());
        fileService.deleteFilesAndMarkersBatch(directoryPath.bucketName(), objectNames);
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
        String newDirectoryAbsolutePath = getFullTargetPath(sourceFolder.absolute(), request.from(), request.to());
        String newDirectoryRelativePath = resourcePathUtil.getRelativePath(newDirectoryAbsolutePath);
        return getMetadata(newDirectoryRelativePath);
    }

    @Override
    public boolean exists(String relativeObjectPath) {
        ResourcePath resourcePath = resourcePathUtil
                .getResourcePathFromRelative(relativeObjectPath + EMPTY_DIRECTORY_MARKER);
        return fileService.exists(resourcePath.relative());
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
        checkCollisions(request, sourceObjectsFullPaths);
        sourceObjectsFullPaths.forEach(objectFullPath -> copyObject(objectFullPath, request));
        delete(sourcePath.relative());
    }

    private void checkCollisions(MoveRequest request, Set<String> objects) {
        List<String> errors = getCollisions(request.from(), request.to(), objects.toArray(new String[0]));
        if (!errors.isEmpty()) {
            throw new ConflictException(String.join(", ", errors));
        }
    }

    private List<String> getCollisions(String from, String to, String... objects) {
        List<String> errors = new ArrayList<>();
        for (String sourceObjectFullPath : objects) {
            if (isDirectory(sourceObjectFullPath)) {
                continue;
            }
            String fullObjectTargetPath = getFullTargetPath(sourceObjectFullPath, from, to);
            if (fileService.exists(resourcePathUtil.getRelativePath(fullObjectTargetPath))) {
                errors.add(String.format(FILE_EXISTS_TEMPLATE, resourcePathUtil.getRelativePath(fullObjectTargetPath)));
            }
        }
        return errors;
    }

    private ResourcePath copyObject(String sourceObjectFullPath, MoveRequest request) {
        String targetFullPath = getFullTargetPath(sourceObjectFullPath, request.from(), request.to());
        ResourcePath toPath = resourcePathUtil.getResourcePathFromAbsolute(targetFullPath);
        if (isDirectory(targetFullPath)) {
            createEmptyDirectoryIgnoringConflicts(toPath.relative());
            return toPath;
        }
        ResourcePath fromPath = resourcePathUtil.getResourcePathFromAbsolute(sourceObjectFullPath);
        storageService.copyObject(fromPath, toPath);
        return toPath;
    }

    private String getFullTargetPath(String sourceFullPath, String from, String to) {
        String sourcePathPrefix = resourcePathUtil.getResourcePathFromRelative(from).absolute();
        String targetPathPrefix = resourcePathUtil.getResourcePathFromRelative(to).absolute();
        return targetPathPrefix + (sourceFullPath.substring(sourcePathPrefix.length()));
    }

}
