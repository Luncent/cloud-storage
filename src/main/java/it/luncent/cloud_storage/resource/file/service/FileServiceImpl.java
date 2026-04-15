package it.luncent.cloud_storage.resource.file.service;

import io.minio.ObjectWriteResponse;
import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.resource.exception.DownloadException;
import it.luncent.cloud_storage.resource.file.exception.FileExistsException;
import it.luncent.cloud_storage.resource.file.exception.FileNotFoundException;
import it.luncent.cloud_storage.resource.mapper.FileMapper;
import it.luncent.cloud_storage.resource.model.common.Path;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final ResourcePathUtil resourcePathUtil;
    private final StorageService storageService;
    private final FileMapper fileMapper;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        StatObjectResponse objectMetadata = findMetadataByPath(resourcePath);
        return fileMapper.mapToFileResponse(resourcePath, objectMetadata);
    }

    @Override
    public void delete(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        if (!exists(path)) {
            throw new FileNotFoundException(path);
        }
        storageService.delete(resourcePath);
    }

    @Override
    public void deleteFilesBatch(String bucket, Set<String> paths) {
        storageService.deleteFilesBatch(bucket, paths);
    }

    @Override
    public void download(OutputStream outputStream, String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        try {
            InputStream fileInputStream = storageService.downloadFile(resourcePath);
            writeFileInputStreamToOutputStream(fileInputStream, outputStream);
        } catch (ObjectNotFoundException ex) {
            throw new FileNotFoundException(path);
        }
    }

    @Override
    public ResourceMetadataResponse move(MoveRequest request) {
        String sourceFileName = request.from();
        if (!exists(sourceFileName)) {
            throw new FileNotFoundException(sourceFileName);
        }
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(sourceFileName);
        String targetObjectFullPath = getFullTargetPath(resourcePath.absolute(), request);
        checkCollision(targetObjectFullPath);
        ResourcePath newObjectPath = copyObject(resourcePath.absolute(), request);
        delete(resourcePath.relative());
        return getMetadata(newObjectPath.relative());
    }

    @Override
    public boolean exists(String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        try {
            findMetadataByPath(resourcePath);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public ResourceMetadataResponse upload(InputStream inputStream, Path path, String contentType) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path.getFullPath());
        checkCollision(resourcePath.relative());
        //TODO check if response contains size
        ObjectWriteResponse response = storageService.uploadFile(resourcePath, inputStream, contentType);
        return getMetadata(resourcePath.relative());
    }

    private StatObjectResponse findMetadataByPath(ResourcePath path) {
        return storageService.getObjectMetadata(path)
                .orElseThrow(() -> new FileNotFoundException(path.relative()));
    }

    private void writeFileInputStreamToOutputStream(InputStream fileInputStream, OutputStream outputStream) {
        try (BufferedInputStream bis = new BufferedInputStream(fileInputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
            bis.transferTo(bufferedOutputStream);
        } catch (Exception e) {
            throw new DownloadException(e.getMessage(), e);
        }
    }

    private void checkCollision(String fullPath) {
        if (exists(resourcePathUtil.getRelativePath(fullPath))) {
            throw new FileExistsException(resourcePathUtil.getRelativePath(fullPath));
        }
    }

    private String getFullTargetPath(String sourceFullPath, MoveRequest request) {
        String sourcePathPrefix = resourcePathUtil.getResourcePathFromRelative(request.from()).absolute();
        String targetPathPrefix = resourcePathUtil.getResourcePathFromRelative(request.to()).absolute();
        return targetPathPrefix + (sourceFullPath.substring(sourcePathPrefix.length()));
    }

    private ResourcePath copyObject(String sourceObjectFullPath, MoveRequest request) {
        String targetFullPath = getFullTargetPath(sourceObjectFullPath, request);
        ResourcePath toPath = resourcePathUtil.getResourcePathFromAbsolute(targetFullPath);
        ResourcePath fromPath = resourcePathUtil.getResourcePathFromAbsolute(sourceObjectFullPath);
        storageService.copyObject(fromPath, toPath);
        return toPath;
    }
}
