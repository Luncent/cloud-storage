package it.luncent.cloud_storage.resource.directory.service;

import io.minio.Result;
import io.minio.messages.Item;
import it.luncent.cloud_storage.resource.archive.exception.ArchiveDownloadException;
import it.luncent.cloud_storage.resource.archive.service.ArchiveService;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryDownloadException;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryExistsException;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryMoveException;
import it.luncent.cloud_storage.resource.directory.exception.DirectoryNotFoundException;
import it.luncent.cloud_storage.resource.directory.mapper.DirectoryMapper;
import it.luncent.cloud_storage.resource.file.service.FileService;
import it.luncent.cloud_storage.resource.mapper.FileMapper;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final FileMapper fileMapper;
    private final DirectoryMapper directoryMapper;
    private final ResourcePathUtil resourcePathUtil;
    @Value("${minio.users-bucket}")
    private String bucketName;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        if (!exists(path)) {
            throw new DirectoryNotFoundException(path);
        }
        return directoryMapper.mapToResponse(path);
    }

    @Override
    public List<ResourceMetadataResponse> getContents(String path) {
        Iterable<Result<Item>> directoryObjects = getDirectoryContent(resourcePathUtil.getAbsolutePath(path));
        List<ResourceMetadataResponse> directoryContents = new ArrayList<>();
        for (Result<Item> directoryObject : directoryObjects) {
            Item item = getItem(directoryObject);
            directoryContents.add(getMetadata(item));
        }
        return directoryContents;
    }

    @Override
    public List<Item> getAllContents(String absolutePath) {
        List<Item> objects = new ArrayList<>();
        Iterable<Result<Item>> directoryObjects = null;
        try {
            directoryObjects = storageService.listObjects(absolutePath, bucketName);
        } catch (ObjectNotFoundException e) {
            throw new DirectoryNotFoundException(absolutePath);
        }
        for (Result<Item> result : directoryObjects) {
            Item object = getItem(result);
            String objectName = object.objectName();
            if (isDirectory(objectName)) {
                objects.addAll(getAllContents(objectName));
            }
            objects.add(object);
        }
        return objects;
    }

    @Override
    public ResourceMetadataResponse createEmptyDirectory(String relativePath) {
        if (exists(relativePath)) {
            throw new DirectoryExistsException(relativePath);
        }
        String directoryPath = relativePath + EMPTY_DIRECTORY_MARKER;
        fileService.upload(new ByteArrayInputStream(new byte[FOLDER_MARKER_SIZE]), directoryPath, MimeTypeUtils.ALL_VALUE);
        return directoryMapper.mapToResponse(relativePath);
    }

    @Override
    public void delete(String path) {
        ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(path);
        List<Item> objects = getAllContents(directoryPath.absolute());
        Set<String> objectNames = objects.stream()
                .map(Item::objectName)
                .collect(Collectors.toSet());
        fileService.deleteFilesBatch(directoryPath.bucketName(), objectNames);
    }

    //TODO фильтрануть надо будет
    @Override
    public void download(OutputStream outputStream, String path) {
        Map<String, InputStream> resourceNamesStreams = new HashMap<>();
        String resourcePath = resourcePathUtil.getAbsolutePath(path);
        for (Item item : getAllContents(resourcePath)) {
            String absolutePath = item.objectName();
            String itemRelativePath = resourcePathUtil.getRelativePath(absolutePath);
            String objectName = itemRelativePath.substring(path.length());
            if (isDirectory(objectName)) {
                resourceNamesStreams.put(objectName, null);
                continue;
            }
            resourceNamesStreams.put(objectName, fileService.download(itemRelativePath));
        }
        try {
            archiveService.outputArchive(resourceNamesStreams, outputStream);
        } catch (ArchiveDownloadException e) {
            throw new DirectoryDownloadException(path);
        }
    }

    @Override
    public ResourceMetadataResponse move(MoveRequest request) {
        ResourcePath sourceFolder = resourcePathUtil.getResourcePathFromRelative(request.from());
        moveDirectory(request);
        String newDirectoryAbsolutePath = resourcePathUtil
                .getFullTargetPath(sourceFolder.absolute(), request.from(), request.to());
        String newDirectoryRelativePath = resourcePathUtil.getRelativePath(newDirectoryAbsolutePath);
        return getMetadata(newDirectoryRelativePath);
    }

    @Override
    public boolean exists(String path) {
        return fileService.exists(path + EMPTY_DIRECTORY_MARKER);
    }

    private Iterable<Result<Item>> getDirectoryContent(String path) {
        try {
            return storageService.listObjects(path, bucketName);
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

    private ResourceMetadataResponse getMetadata(Item item) {
        String path = item.objectName();
        String relativePath = resourcePathUtil.getRelativePath(path);
        if (isDirectory(path)) {
            return directoryMapper.mapToResponse(relativePath);
        }
        return fileMapper.mapToFileResponse(relativePath, item.size());
    }

    private void moveDirectory(MoveRequest request) {
        ResourcePath sourcePath = resourcePathUtil.getResourcePathFromRelative(request.from());
        //TODO убрать
        Set<String> sourceObjectsFullPaths = getAllContents(sourcePath.absolute()).stream()
                .map(Item::objectName)
                .collect(Collectors.toSet());
        sourceObjectsFullPaths.add(sourcePath.absolute());
        checkCollisions(request, sourceObjectsFullPaths);
        sourceObjectsFullPaths.forEach(objectFullPath -> moveObject(objectFullPath, request));
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
            String fullObjectTargetPath = resourcePathUtil.getFullTargetPath(sourceObjectFullPath, from, to);
            if (fileService.exists(resourcePathUtil.getRelativePath(fullObjectTargetPath))) {
                existingFiles.add(resourcePathUtil.getRelativePath(fullObjectTargetPath));
            }
        }
        if (!existingFiles.isEmpty()) {
            throw new DirectoryMoveException(existingFiles);
        }
    }

    private String moveObject(String sourceObjectFullPath, MoveRequest request) {
        String targetFullPath = resourcePathUtil.getFullTargetPath(sourceObjectFullPath,
                request.from(), request.to());
        String targetRelative = resourcePathUtil.getRelativePath(targetFullPath);
        if (isDirectory(targetFullPath)) {
            try {
                createEmptyDirectory(targetRelative);
            } catch (DirectoryExistsException e) {
                //ignore conflict
            }
            return targetRelative;
        }
        String fromRelative = resourcePathUtil.getRelativePath(sourceObjectFullPath);
        MoveRequest fileMoveRequest = new MoveRequest(fromRelative, targetRelative);
        fileService.move(fileMoveRequest);
        return targetRelative;
    }

}
