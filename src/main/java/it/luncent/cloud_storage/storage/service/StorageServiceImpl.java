package it.luncent.cloud_storage.storage.service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationFilter;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.security.service.AuthService;
import it.luncent.cloud_storage.storage.exception.ReservedNameException;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isMarker;
import static java.lang.String.format;

//TODO rethink exception handling
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private static final String FOLDER_NOT_FOUND_TEMPLATE = "folder %s not found";
    private static final String OBJECT_NOT_FOUND_TEMPLATE = "object %s not found";
    private static final Integer FILE_SIZE_NOT_KNOWN = -1;
    private static final Integer FILE_SIZE_IS_KNOWN = -1;
    private static final Integer EMPTY_FOLDER_SIZE = 0;
    private static final Long MB = 1024L * 1024L;
    private static final Long PART_SIZE = 10 * MB;

    private final MinioClient minioClient;
    private final ResourcePathUtil resourcePathUtil;
    private final ForkJoinPool forkJoinPool;
    private final AuthService authService;

    @Override
    public String createEmptyDirectory(ResourcePath directoryPath) {
        try {
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(directoryPath.bucketName())
                            .object(directoryPath.absolute() + EMPTY_DIRECTORY_MARKER)
                            .stream(new ByteArrayInputStream(new byte[EMPTY_FOLDER_SIZE]), EMPTY_FOLDER_SIZE, FILE_SIZE_IS_KNOWN)
                            .build()
            );
            return response.object();
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public ObjectWriteResponse copyObject(ResourcePath from, ResourcePath to) {
        try {
            return minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(
                                    CopySource.builder()
                                            .bucket(from.bucketName())
                                            .object(from.absolute())
                                            .build()
                            )
                            .bucket(to.bucketName())
                            .object(to.absolute())
                            .build()
            );
        } catch (ErrorResponseException ex) {
            if (ex.response().code() == 404) {
                throw new ResourceNotFoundException(format(OBJECT_NOT_FOUND_TEMPLATE, from.relative()), ex);
            }
            throw new StorageException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    //TODO нужно ли удалять файлы директории для ее удаления? просто директорию нельзя удалить?
    @Override
    public void deleteDirectory(ResourcePath directoryPath) {
        List<Item> objects = new ArrayList<>();
        PopulationFilter includeFilesAndMarkers = new PopulationFilter(false, true, true);
        populateWithDirectoryObjectsAsync(directoryPath, objects, includeFilesAndMarkers);
        List<String> objectNames = objects.stream()
                .map(Item::objectName)
                .toList();
        deleteFilesBatch(directoryPath.bucketName(), objectNames);
    }

    @Override
    public void deleteFile(ResourcePath filePath) {
        checkRequestedPathForEmptyDirectoryTag(filePath);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(filePath.bucketName())
                            .object(filePath.absolute())
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(ResourcePath filePath) {
        checkRequestedPathForEmptyDirectoryTag(filePath);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(filePath.bucketName())
                            .object(filePath.absolute())
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public Iterable<Result<Item>> getDirectoryContent(ResourcePath directoryPath) {
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(directoryPath.bucketName())
                            .prefix(directoryPath.absolute())
                            .build()
            );
            objects.iterator().next().get();
            return objects;
        } catch (ErrorResponseException ex) {
            if (ex.response().code() == 404) {
                throw new ResourceNotFoundException(format(FOLDER_NOT_FOUND_TEMPLATE, directoryPath.relative()), ex);
            }
            throw new StorageException(ex.getMessage(), ex);
        } catch (NoSuchElementException ex) {
            throw new ResourceNotFoundException(format(FOLDER_NOT_FOUND_TEMPLATE, directoryPath.relative()), ex);
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public StatObjectResponse getObjectMetadata(ResourcePath objectPath) {
        checkRequestedPathForEmptyDirectoryTag(objectPath);
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(objectPath.bucketName())
                            .object(objectPath.absolute())
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                throw new ResourceNotFoundException(objectPath.relative() + " not found", e);
            }
            throw new StorageException(e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public void populateWithDirectoryObjects(ResourcePath directoryPath, List<Item> objects, PopulationFilter populationFilter) {
        try {
            Iterable<Result<Item>> results = getDirectoryContent(directoryPath);
            for (Result<Item> result : results) {
                Item object = result.get();
                String objectName = object.objectName();
                log.debug("Found object {} for dir {}", objectName, directoryPath.absolute());
                if (isDirectory(objectName)) {
                    if(populationFilter.includeDirectories()){
                        objects.add(object);
                    }
                    ResourcePath nestedDirectoryPath = resourcePathUtil.getResourcePathFromAbsolute(objectName);
                    populateWithDirectoryObjects(nestedDirectoryPath, objects, populationFilter);
                    continue;
                }
                if (!isSkipMarker(populationFilter.includeMarkers(), objectName)) {
                    if(populationFilter.includeFiles()){
                        objects.add(object);
                    }
                }
            }
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    @Override
    public void populateWithDirectoryObjectsAsync(ResourcePath directoryPath, List<Item> objects, PopulationFilter populationFilter) {
        Long userId = authService.getCurrentUser().id();
        ConcurrentObjectsExtractionAction extractionAction = new ConcurrentObjectsExtractionAction(directoryPath, objects, populationFilter, userId);
        forkJoinPool.invoke(extractionAction);
    }

    @Override
    public void uploadFile(ResourcePath filePath, InputStream inputStream, String contentType) {
        checkRequestedPathForEmptyDirectoryTag(filePath);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(filePath.bucketName())
                            .object(filePath.absolute())
                            .contentType(contentType)
                            .stream(inputStream, FILE_SIZE_NOT_KNOWN, PART_SIZE)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException(ex.getMessage(), ex);
        }
    }

    private void checkRequestedPathForEmptyDirectoryTag(ResourcePath objectPath) {
        if (isMarker(objectPath.absolute())) {
            throw new ReservedNameException(EMPTY_DIRECTORY_MARKER + " is reserved");
        }
    }

    public void deleteFilesBatch(String bucketName, List<String> objectNames) {
        List<DeleteObject> deleteObjects = objectNames.stream()
                .map(DeleteObject::new)
                .toList();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build()
        );
        results.forEach(result -> {
        });
    }

    private boolean isSkipMarker(boolean includeMarkers, String objectName) {
        return !includeMarkers && isMarker(objectName);
    }

    private class ConcurrentObjectsExtractionAction extends RecursiveAction {

        private final ResourcePath directoryPath;
        private final List<Item> objects;
        private final PopulationFilter populationFilter;
        private final Long userId;

        public ConcurrentObjectsExtractionAction(ResourcePath directoryPath, List<Item> objects, PopulationFilter populationFilter, Long userId) {
            this.directoryPath = directoryPath;
            this.objects = objects;
            this.populationFilter = populationFilter;
            this.userId = userId;
        }

        @Override
        protected void compute() {
            try {
                Iterable<Result<Item>> directoryObjects = getDirectoryContent(directoryPath);
                List<ResourcePath> nestedDirectoriesPaths = new ArrayList<>();
                for (Result<Item> directoryObject : directoryObjects) {
                    Item object = directoryObject.get();
                    String objectName = object.objectName();
                    if (isDirectory(objectName)) {
                        if(populationFilter.includeDirectories()){
                            objects.add(object);
                        }
                        nestedDirectoriesPaths.add(resourcePathUtil.concurrentGetPathFromAbsolute(objectName, userId));
                        continue;
                    }
                    if (isSkipMarker(populationFilter.includeMarkers(), objectName)) {
                        continue;
                    }
                    if(populationFilter.includeFiles()){
                        log.debug("adding file thread {}", Thread.currentThread().getName());
                        objects.add(object);
                    }
                }
                startRecursiveActions(nestedDirectoriesPaths);
            } catch (ResourceNotFoundException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new StorageException(ex.getMessage(), ex);
            }
        }

        private void startRecursiveActions(List<ResourcePath> nestedDirectories) {
            List<ConcurrentObjectsExtractionAction> action = nestedDirectories.stream()
                    .map(nestedDirectoryPath -> new ConcurrentObjectsExtractionAction(nestedDirectoryPath, objects, populationFilter, userId))
                    .toList();
            RecursiveAction.invokeAll(action);
        }
    }
}
