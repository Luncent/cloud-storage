package it.luncent.cloud_storage.resource.file.service;

import io.minio.ObjectWriteResponse;
import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.resource.file.exception.FileDownloadException;
import it.luncent.cloud_storage.resource.file.exception.FileExistsException;
import it.luncent.cloud_storage.resource.file.exception.FileNotFoundException;
import it.luncent.cloud_storage.resource.mapper.FileMapper;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ObjectNotFoundException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final ResourcePathUtil resourcePathUtil;
    private final StorageService storageService;
    private final FileMapper fileMapper;
    @Value("${minio.users-bucket}")
    private String bucketName;

    @Override
    public ResourceMetadataResponse getMetadata(String path) {
        StatObjectResponse objectMetadata = findMetadataByPath(path);
        return fileMapper.mapToFileResponse(path, objectMetadata.size());
    }

    @Override
    public void delete(String path) {
        String absolutePath = resourcePathUtil.getAbsolutePath(path);
        if (!exists(path)) {
            throw new FileNotFoundException(path);
        }
        storageService.delete(absolutePath, bucketName);
    }

    @Override
    public void deleteFilesBatch(String bucket, Set<String> paths) {
        storageService.deleteFilesBatch(bucket, paths);
    }

    @Override
    public void download(OutputStream outputStream, String path) {
        InputStream fileInputStream = download(path);
        try (BufferedInputStream bis = new BufferedInputStream(fileInputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
            bis.transferTo(bufferedOutputStream);
        } catch (IOException ex) {
            throw new FileDownloadException(path);
        }
    }

    @Override
    public InputStream download(String path) {
        String absolutePath = resourcePathUtil.getAbsolutePath(path);
        try {
            return storageService.download(absolutePath, bucketName);
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
        String targetObjectFullPath = resourcePathUtil
                .getFullTargetPath(resourcePath.absolute(), request.from(), request.to());
        checkCollision(targetObjectFullPath);
        String copiedObjectPath = copyObject(resourcePath.absolute(), request);
        delete(resourcePath.relative());
        return getMetadata(resourcePathUtil.getRelativePath(copiedObjectPath));
    }

    @Override
    public boolean exists(String path) {
        try {
            findMetadataByPath(path);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public ResourceMetadataResponse upload(InputStream inputStream, String path, String contentType) {
        checkCollision(path);
        String absolutePath = resourcePathUtil.getAbsolutePath(path);
        //TODO check if response contains size
        ObjectWriteResponse response = storageService.upload(absolutePath, bucketName, inputStream, contentType);
        return getMetadata(path);
    }

    private StatObjectResponse findMetadataByPath(String path) {
        return storageService.getObjectMetadata(resourcePathUtil.getAbsolutePath(path), bucketName)
                .orElseThrow(() -> new FileNotFoundException(path));
    }

    private void checkCollision(String fullPath) {
        if (exists(resourcePathUtil.getRelativePath(fullPath))) {
            throw new FileExistsException(resourcePathUtil.getRelativePath(fullPath));
        }
    }

    private String copyObject(String fromPath, MoveRequest request) {
        String toPath = resourcePathUtil.getFullTargetPath(fromPath, request.from(), request.to());
        try {
            ObjectWriteResponse response = storageService.copyObject(fromPath, toPath, bucketName);
            return response.object();
        } catch (ObjectNotFoundException e) {
            throw new FileNotFoundException(fromPath);
        }
    }
}
