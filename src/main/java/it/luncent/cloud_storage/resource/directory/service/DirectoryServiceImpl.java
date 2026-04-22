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
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        List<Item> directoryObjects = getDirectoryContent(path);
        return directoryObjects.stream()
                .map(this::getMetadata)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getAllContents(String path) {
        Set<String> objectsNames = new HashSet<>();
        try {
            for (Item item : getDirectoryContent(path)) {
                String objectName = item.objectName();
                if (item.isDir()) {
                    objectsNames.addAll(getAllContents(objectName));
                }
                objectsNames.add(objectName);
            }
        } catch (ObjectNotFoundException e) {
            throw new DirectoryNotFoundException(path);
        }
        return objectsNames;
    }

    @Override
    public ResourceMetadataResponse createEmptyDirectory(String path) {
        if (exists(path)) {
            throw new DirectoryExistsException(path);
        }
        String directoryPath = path + EMPTY_DIRECTORY_MARKER;
        fileService.upload(new ByteArrayInputStream(new byte[FOLDER_MARKER_SIZE]), directoryPath, MimeTypeUtils.ALL_VALUE);
        return directoryMapper.mapToResponse(path);
    }

    @Override
    public void delete(String path) {
        Set<String> objectNames = getAllContents(path);
        fileService.deleteFilesBatch(bucketName, getPathsWithDirectoryMarkers(objectNames));
    }

    @Override
    public void download(OutputStream outputStream, String path) {
        Map<String, InputStream> resourceNamesStreams = new HashMap<>();
        for (String nestedObjectPath : getAllContents(path)) {
            String objectName = nestedObjectPath.substring(path.length());
            if (isDirectory(objectName)) {
                resourceNamesStreams.put(objectName, null);
                continue;
            }
            resourceNamesStreams.put(objectName, fileService.download(nestedObjectPath));
        }
        try {
            archiveService.outputArchive(resourceNamesStreams, outputStream);
        } catch (ArchiveDownloadException e) {
            throw new DirectoryDownloadException(path);
        }
    }

    @Override
    public ResourceMetadataResponse move(MoveRequest request) {
        String from = request.from();
        String to = request.to();
        //TODO убрать
        Set<String> objectsToMove = getAllContents(from);
        objectsToMove.add(from);
        checkCollisions(from, to, objectsToMove);
        objectsToMove.forEach(objectToMove -> moveObject(objectToMove, from, to));
        fileService.deleteFilesBatch(bucketName, getPathsWithDirectoryMarkers(objectsToMove));
        return getMetadata(to);
    }

    @Override
    public boolean exists(String path) {
        return fileService.exists(path + EMPTY_DIRECTORY_MARKER);
    }

    private List<Item> getDirectoryContent(String path) {
        try {
            List<Item> content = new ArrayList<>();
            for (Result<Item> directoryObject : storageService.listObjects(path, bucketName)) {
                Item item = getItem(directoryObject);
                if (item.objectName().endsWith(EMPTY_DIRECTORY_MARKER)) {
                    continue;
                }
                content.add(item);
            }
            return content;
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
        if (isDirectory(path)) {
            return directoryMapper.mapToResponse(path);
        }
        return fileMapper.mapToFileResponse(path, item.size());
    }

    private Set<String> getPathsWithDirectoryMarkers(Set<String> paths) {
        Set<String> newPaths = new HashSet<>();
        for (String path : paths) {
            if (isDirectory(path)) {
                newPaths.add(path + EMPTY_DIRECTORY_MARKER);
            }
            newPaths.add(path);
        }
        return newPaths;
    }

    private void checkCollisions(String fromDir, String toDir, Set<String> objects) {
        Set<String> existingFiles = new HashSet<>();
        for (String object : objects) {
            if (isDirectory(object)) {
                continue;
            }
            String objectNewPath = PathUtils.getDirectoryObjectNewPath(object, fromDir, toDir);
            if (fileService.exists(objectNewPath)) {
                existingFiles.add(objectNewPath);
            }
        }
        if (!existingFiles.isEmpty()) {
            throw new DirectoryMoveException(existingFiles);
        }
    }

    private String moveObject(String path, String fromDir, String toDir) {
        String newPath = PathUtils.getDirectoryObjectNewPath(path, fromDir, toDir);
        if (isDirectory(newPath)) {
            try {
                createEmptyDirectory(newPath);
            } catch (DirectoryExistsException e) {
                //ignore conflict
            }
            return newPath;
        }
        MoveRequest fileMoveRequest = new MoveRequest(path, newPath);
        fileService.move(fileMoveRequest);
        return newPath;
    }

}
