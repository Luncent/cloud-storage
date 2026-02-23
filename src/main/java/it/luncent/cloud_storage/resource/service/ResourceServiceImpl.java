package it.luncent.cloud_storage.resource.service;

import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationSettings;
import it.luncent.cloud_storage.resource.directory.service.DirectoryService;
import it.luncent.cloud_storage.resource.exception.ConflictException;
import it.luncent.cloud_storage.resource.file.service.FileService;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.common.UploadingFile;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
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
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(request.from());

        if (isDirectory(request.from())) {
            moveDirectory(request, resourcePath);
            String newDirectoryAbsolutePath = getFullTargetPath(resourcePath.absolute(), request);
            String newDirectoryRelativePath = resourcePathUtil.getRelativePath(newDirectoryAbsolutePath);
            return getMetadata(newDirectoryRelativePath);
        }

        ResourcePath newResourcePath = moveFile(request, resourcePath);
        return getMetadata(newResourcePath.relative());
    }

    @Override
    public List<ResourceMetadataResponse> searchResource(Optional<String> query) {
        ResourcePath rootDirectory = resourcePathUtil.getResourcePathFromRelative(ROOT_DIRECTORY);
        List<Item> objects = new ArrayList<>();
        PopulationSettings populationSettings = PopulationSettings.builder()
                .includeDirectories(true)
                .includeMarkers(false)
                .includeFiles(true)
                .build();
        storageService.populateWithDirectoryObjectsAsync(rootDirectory, objects, populationSettings);

        return query.map(searchQuery ->
                        objects.stream()
                                .filter(object -> resourceNameMatchesQuery(object.objectName(), searchQuery))
                                .map(object -> resourcePathUtil.getRelativePath(object.objectName()))
                                .map(this::getMetadata)
                                .toList())
                .orElseGet(() ->
                        objects.stream()
                                .map(object -> resourcePathUtil.getRelativePath(object.objectName()))
                                .map(this::getMetadata)
                                .toList());
    }

    @Override
    @SneakyThrows
    public List<ResourceMetadataResponse> upload(UploadRequest request) {
        List<String> uploadedResourcesRelativePaths = new ArrayList<>(createNestedDirectories(request));
        String fileName = request.file().getResource().getFilename();
        checkCollisions(resourcePathUtil.getAbsolutePathFromRelative(request.targetDirectory() + fileName));
        uploadedResourcesRelativePaths.add(uploadFileResource(request));
        return uploadedResourcesRelativePaths.stream()
                .map(this::getMetadata)
                .collect(toList());
    }

    @Override
    public List<ResourceMetadataResponse> upload(List<UploadRequest> requests) {
        return requests.stream()
                .map(this::upload)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private List<String> createNestedDirectories(UploadRequest request) {
        List<String> directoriesToCreate = getDirectoriesRelativePaths(request).stream()
                .filter(directoryRelativePath -> !directoryExists(directoryRelativePath))
                .collect(toList());
        for (String directoryToCreate : directoriesToCreate) {
            ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(directoryToCreate);
            directoryService.createEmptyDirectory(directoryPath.absolute());
        }
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

    /**
     * @param sourceFullPath полный путь перемещаемого объекта
     *                       <pre>{@code
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             // Пример получения нового пути объекта, при перемещении папки
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             moveRequest = new MoveRequest(from="dir1/dir2/", to="dir3/")
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             fullTargetPath = moveObject("user-1-files/dir1/dir2/dir4/file.txt", moveRequest);
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             // fullTargetPath = user-1-files/dir3/dir4/file.txt
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             }</pre>
     */
    private String getFullTargetPath(String sourceFullPath, MoveRequest request) {
        String sourcePathPrefix = resourcePathUtil.getResourcePathFromRelative(request.from()).absolute();
        String targetPathPrefix = resourcePathUtil.getResourcePathFromRelative(request.to()).absolute();
        return targetPathPrefix + (sourceFullPath.substring(sourcePathPrefix.length()));
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

    //TODO move to storage service
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

    private ResourcePath moveFile(MoveRequest request, ResourcePath sourcePath) {
        StatObjectResponse response = storageService.getObjectMetadata(sourcePath);
        String objectSourceFullPath = response.object();
        checkCollisions(getFullTargetPath(objectSourceFullPath, request));
        ResourcePath newObjectPath = copyObject(objectSourceFullPath, request);
        storageService.delete(sourcePath);
        return newObjectPath;
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
                .map(item -> getFullTargetPath(item.objectName(), request))
                .collect(Collectors.toSet());
        sourceObjectsFullPaths.add(getFullTargetPath(sourcePath.absolute(), request));
        checkCollisions(sourceObjectsFullPaths.toArray(new String[0]));
        sourceObjectsFullPaths.forEach(objectFullPath -> copyObject(objectFullPath, request));
        directoryService.delete(sourcePath.relative());
    }

    //TODO удалить после рефакторинга
    public ResourceMetadataResponse createEmptyDirectory(String relativePath) {
        if (directoryExists(relativePath)) {
            throw new ConflictException(String.format(OBJECT_EXISTS_TEMPLATE, relativePath));
        }
        ResourcePath directoryPath = resourcePathUtil.getResourcePathFromRelative(relativePath);
        ResourceMetadataResponse response = directoryService.createEmptyDirectory(directoryPath.absolute());
        return getMetadata(response.path());
    }

    private ResourcePath copyObject(String sourceObjectFullPath, MoveRequest request) {
        String targetFullPath = getFullTargetPath(sourceObjectFullPath, request);
        ResourcePath toPath = resourcePathUtil.getResourcePathFromAbsolute(targetFullPath);
        if (isDirectory(targetFullPath)) {
            if (!directoryExists(toPath.relative())) {
                createEmptyDirectory(toPath.relative());
            }
            return toPath;
        }
        ResourcePath fromPath = resourcePathUtil.getResourcePathFromAbsolute(sourceObjectFullPath);
        storageService.copyObject(fromPath, toPath);
        return toPath;
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

    private String uploadFileResource(UploadRequest request) {
        try {
            UploadingFile uploadingFile = UploadingFile.withKnownFileSize(request);
            return uploadFile(uploadingFile);
        } catch (Exception e) {
            //TODO make method to handle minioException and throw corresponding one
            throw new RuntimeException(e);
        }
    }

    private String uploadFile(UploadingFile uploadingFile) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(uploadingFile.relativePath());
        storageService.uploadFile(
                resourcePath,
                uploadingFile.inputStream(),
                uploadingFile.contentType()
        );
        return resourcePath.relative();
    }

    private String createEmptyDirectory(ResourcePath path) {
        directoryService.createEmptyDirectory(path.absolute());
        return path.relative();
    }

}

