package it.luncent.cloud_storage.resource.directory.service;

import io.minio.Result;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationSettings;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryExistsException;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryMoveException;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryNotFoundException;
import it.luncent.cloud_storage.resource.directory.mapper.DirectoryMapper;
import it.luncent.cloud_storage.resource.file.service.FileService;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import it.luncent.cloud_storage.storage.service.ArchiveService;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    public static final String EMPTY_DIRECTORY_MARKER = "empty-folder-tag";
    private static final Integer FOLDER_MARKER_SIZE = 0;

    private final ArchiveService archiveService;
    private final StorageService storageService;
    private final FileService fileService;
    private final ResourcePathUtil resourcePathUtil;
    private final DirectoryMapper directoryMapper;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        if (fileService.exists(path + EMPTY_DIRECTORY_MARKER)) {
            throw new DirectoryNotFoundException(path);
        }
        return directoryMapper.mapToResponse(resourcePath.relative());
    }

    @Override
    public List<ResourceMetadataResponse> getContents(String path) {
        Iterable<Result<Item>> directoryObjects = getDirectoryContent(path);
        List<ResourceMetadataResponse> directoryContents = new ArrayList<>();
        for (Result<Item> directoryObject : directoryObjects) {
            Item item = getItem(directoryObject);
            directoryContents.add(getResourceMetadata(item));
        }
        return directoryContents;
    }

    @Override
    public ResourceMetadataResponse createEmptyDirectory(String relativePath) {
        if (fileService.exists(relativePath + EMPTY_DIRECTORY_MARKER)) {
            throw new DirectoryExistsException(relativePath);
        }
        ResourcePath directoryPath = resourcePathUtil
                .getResourcePathFromRelative(relativePath + EMPTY_DIRECTORY_MARKER);
        storageService.uploadFile(directoryPath,
                new ByteArrayInputStream(new byte[FOLDER_MARKER_SIZE]), MimeTypeUtils.ALL_VALUE);
        return directoryMapper.mapToResponse(relativePath);
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
        fileService.deleteFilesBatch(directoryPath.bucketName(), objectNames);
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
        moveDirectory(request);
        String newDirectoryAbsolutePath = getFullTargetPath(sourceFolder.absolute(), request.from(), request.to());
        String newDirectoryRelativePath = resourcePathUtil.getRelativePath(newDirectoryAbsolutePath);
        return getMetadata(newDirectoryRelativePath);
    }

    @Override
    public boolean exists(String path) {
        return fileService.exists(path + EMPTY_DIRECTORY_MARKER);
    }

    private Iterable<Result<Item>> getDirectoryContent(String path) {
        try {
            return storageService.listObjects(resourcePathUtil.getResourcePathFromRelative(path));
        } catch (ObjectNotFoundException e) {
            boolean isRootDirectory = path.isEmpty();
            if (isRootDirectory) {
                return List.of();
            }
            throw new DirectoryNotFoundException(path);
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

    private void moveDirectory(MoveRequest request) {
        ResourcePath sourcePath = resourcePathUtil.getResourcePathFromRelative(request.from());
        List<Item> objects = new ArrayList<>();
        //TODO убрать
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
        String from = request.from();
        String to = request.to();
        Set<String> existingFiles = new HashSet<>();
        for (String sourceObjectFullPath : objects) {
            if (isDirectory(sourceObjectFullPath)) {
                continue;
            }
            String fullObjectTargetPath = getFullTargetPath(sourceObjectFullPath, from, to);
            if (fileService.exists(resourcePathUtil.getRelativePath(fullObjectTargetPath))) {
                existingFiles.add(resourcePathUtil.getRelativePath(fullObjectTargetPath));
            }
        }
        if (!existingFiles.isEmpty()) {
            throw new DirectoryMoveException(existingFiles);
        }
    }

    private ResourcePath copyObject(String sourceObjectFullPath, MoveRequest request) {
        String targetFullPath = getFullTargetPath(sourceObjectFullPath, request.from(), request.to());
        ResourcePath toPath = resourcePathUtil.getResourcePathFromAbsolute(targetFullPath);
        if (isDirectory(targetFullPath)) {
            try {
                createEmptyDirectory(toPath.relative());
                return toPath;
            } catch (DirectoryExistsException e) {

            }
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
