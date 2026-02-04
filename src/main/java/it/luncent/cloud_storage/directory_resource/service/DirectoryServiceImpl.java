package it.luncent.cloud_storage.directory_resource.service;

import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.util.ObjectStorageUtil;
import it.luncent.cloud_storage.resource.mapper.ResourceMapper;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    private final StorageService storageService;
    private final ResourcePathUtil resourcePathUtil;
    private final ResourceMapper resourceMapper;

    @Override
    public List<ResourceMetadataResponse> getDirectoryContents(String path) {
        //TODO not found вынести чтоли в отдельный метод try-catch
        try {
            Iterable<Result<Item>> directoryObjects = getDirectoryContent(path);
            List<ResourceMetadataResponse> directoryContents = new ArrayList<>();
            for (Result<Item> directoryObject : directoryObjects) {
                String objectFullPath = directoryObject.get().objectName();
                if (!ObjectStorageUtil.isMarker(objectFullPath)) {
                    directoryContents.add(getResourceMetadata(resourcePathUtil.getRelativePath(objectFullPath)));
                }
            }
            return directoryContents;
        } catch (ResourceNotFoundException e) {
            if (path.isEmpty()) {
                return List.of();
            }
            throw e;
        }
    }

    private List<ResourceMetadataResponse> mapToResponses(Iterable<Result<Item>> directoryObjects) {
        List<ResourceMetadataResponse> directoryContents = new ArrayList<>();
        for (Result<Item> directoryObject : directoryObjects) {
            try {
                Item metadata = directoryObject.get();
                String fullPath = directoryObject.get().objectName();
                if (!ObjectStorageUtil.isMarker(fullPath)) {
                    directoryContents.add(resourceMapper.map);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return directoryContents;
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

    @Override
    public ResourceMetadataResponse createEmptyDirectory(String path) {
        return null;
    }

    //TODO Подумать что делать с ним и его дубликатом
    private ResourceMetadataResponse getResourceMetadata(Item metadata) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromAbsolute(relativePath);
        if (isDirectory(relativePath)) {
            storageService.getDirectoryContent(resourcePath);
            return resourceMapper.mapToFolderResponse(resourcePath.relative());
        }
        StatObjectResponse objectMetadata = storageService.getObjectMetadata(resourcePath);
        return resourceMapper.mapToFileResponse(resourcePath, objectMetadata);
    }
}
