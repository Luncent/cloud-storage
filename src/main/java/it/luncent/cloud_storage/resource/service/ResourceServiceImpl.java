package it.luncent.cloud_storage.resource.service;

import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.constants.PopulationFilter;
import it.luncent.cloud_storage.common.util.ObjectStorageUtil;
import it.luncent.cloud_storage.resource.exception.DownloadException;
import it.luncent.cloud_storage.resource.exception.ConflictException;
import it.luncent.cloud_storage.resource.mapper.ResourceMapper;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.common.UploadingFile;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.storage.service.ArchiveService;
import it.luncent.cloud_storage.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    private final ResourceMapper resourceMapper;
    private final Tika tika;
    private final StorageService storageService;
    private final ArchiveService archiveService;

    @Override
    public ResourceMetadataResponse createEmptyDirectory(String relativePath) {
        if (directoryExists(relativePath)){
            throw new ConflictException(String.format(OBJECT_EXISTS_TEMPLATE, relativePath));
        }
        return getResourceMetadata(createEmptyDirectory(resourcePathUtil.getResourcePathFromRelative(relativePath)));
    }

    @Override
    public void deleteResource(String relativePath) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(relativePath);
        if (isDirectory(relativePath)) {
            storageService.deleteDirectory(resourcePath);
            return;
        }
        storageService.deleteFile(resourcePath);
    }

    @Override
    public void downloadResource(OutputStream outputStream, String path) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(path);
        if (isDirectory(path)) {
            archiveService.downloadArchiveAsync(resourcePath, outputStream);
            return;
        }
        //TODO add check for existence
        InputStream fileInputStream = storageService.downloadFile(resourcePath);
        writeFileInputStreamToOutputStream(fileInputStream, outputStream);
    }

    @Override
    @SneakyThrows
    public List<ResourceMetadataResponse> getDirectoryContents(String path) {
        if(StringUtils.isBlank(path)){
            return new ArrayList<>();
        }
        //TODO not found
        List<ResourceMetadataResponse> directoryContents = new ArrayList<>();
        Iterable<Result<Item>> objects = storageService.getDirectoryContent(resourcePathUtil.getResourcePathFromRelative(path));
        //TODO распараллелить
        for(Result<Item> result : objects) {
            String objectFullPath = result.get().objectName();
            if(!ObjectStorageUtil.isMarker(objectFullPath)) {
                directoryContents.add(getResourceMetadata(resourcePathUtil.getRelativePath(objectFullPath)));
            }
        }
        return directoryContents;
    }

    //TODO think about exception handling (convert minio exceptions)
    @Override
    public ResourceMetadataResponse getResourceMetadata(String relativePath) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(relativePath);
        if (isDirectory(relativePath)) {
            storageService.getDirectoryContent(resourcePath);
            return resourceMapper.mapToFolderResponse(resourcePath.relative());
        }
        StatObjectResponse objectMetadata = storageService.getObjectMetadata(resourcePath);
        return resourceMapper.mapToFileResponse(resourcePath, objectMetadata);
    }

    //TODO add validation on controller level
    @Override
    public ResourceMetadataResponse moveResource(MoveRequest request) {
        ResourcePath resourcePath = resourcePathUtil.getResourcePathFromRelative(request.from());

        if (isDirectory(request.from())) {
            moveDirectory(request, resourcePath);
            String newDirectoryAbsolutePath = getFullTargetPath(resourcePath.absolute(), request);
            String newDirectoryRelativePath = resourcePathUtil.getRelativePath(newDirectoryAbsolutePath);
            return getResourceMetadata(newDirectoryRelativePath);
        }

        ResourcePath newResourcePath = moveFile(request, resourcePath);
        return getResourceMetadata(newResourcePath.relative());
    }

    @Override
    public List<ResourceMetadataResponse> searchResource(Optional<String> query) {
        ResourcePath rootDirectory = resourcePathUtil.getResourcePathFromRelative(ROOT_DIRECTORY);
        List<Item> objects = new ArrayList<>();
        PopulationFilter populationFilter = PopulationFilter.builder()
                .includeDirectories(true)
                .includeMarkers(false)
                .includeFiles(true)
                .build();
        storageService.populateWithDirectoryObjectsAsync(rootDirectory, objects, populationFilter);

        return query.map(searchQuery ->
                        objects.stream()
                                .filter(object -> resourceNameMatchesQuery(object.objectName(), searchQuery))
                                .map(object -> resourcePathUtil.getRelativePath(object.objectName()))
                                .map(this::getResourceMetadata)
                                .toList())
                .orElseGet(() ->
                        objects.stream()
                                .map(object -> resourcePathUtil.getRelativePath(object.objectName()))
                                .map(this::getResourceMetadata)
                                .toList());
    }

    @Override
    @SneakyThrows
    public List<ResourceMetadataResponse> upload(UploadRequest request) {
        List<String> uploadedResourcesRelativePaths = new ArrayList<>(createNestedDirectories(request));

        if (isArchive(request.file().getInputStream())) {
            uploadedResourcesRelativePaths.addAll(uploadArchive(request));
        } else {
            uploadedResourcesRelativePaths.add(uploadFileResource(request));
        }
        return uploadedResourcesRelativePaths.stream()
                .map(this::getResourceMetadata)
                .collect(toList());
    }

    private List<String> createNestedDirectories(UploadRequest request) {
        List<String> directoriesToCreate = getDirectoriesRelativePaths(request).stream()
                .filter(directoryRelativePath -> !directoryExists(directoryRelativePath))
                .collect(toList());
        for (String directoryToCreate : directoriesToCreate) {
            storageService.createEmptyDirectory(resourcePathUtil.getResourcePathFromRelative(directoryToCreate));
        }
        return directoriesToCreate;
    }

    private List<String> getDirectoriesRelativePaths(UploadRequest request) {
        List<String> directoriesNames = new LinkedList<>(List.of(request.file().getName().split("/")));
        //removing filename
        directoriesNames.removeLast();

        return directoriesNames.stream()
                .map(name -> request.targetDirectory() + name + "/")
                .collect(Collectors.toList());
    }

    private void writeFileInputStreamToOutputStream(InputStream fileInputStream, OutputStream outputStream) {
        try (BufferedInputStream bis = new BufferedInputStream(fileInputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
            bis.transferTo(bufferedOutputStream);
        } catch (Exception e) {
            throw new DownloadException(e.getMessage(), e);
        }
    }

    /**
     * @param sourceFullPath полный путь перемещаемого объекта
     *                       <pre>{@code
     *                                                                                                                                                                                                                                                                                               // Пример получения нового пути объекта, при перемещении папки
     *                                                                                                                                                                                                                                                                                               moveRequest = new MoveRequest(from="dir1/dir2/", to="dir3/")
     *                                                                                                                                                                                                                                                                                               fullTargetPath = moveObject("user-1-files/dir1/dir2/dir4/file.txt", moveRequest);
     *                                                                                                                                                                                                                                                                                               // fullTargetPath = user-1-files/dir3/dir4/file.txt
     *                                                                                                                                                                                                                                                                                               }</pre>
     */
    private String getFullTargetPath(String sourceFullPath, MoveRequest request) {
        String sourcePathPrefix = resourcePathUtil.getResourcePathFromRelative(request.from()).absolute();
        String targetPathPrefix = resourcePathUtil.getResourcePathFromRelative(request.to()).absolute();
        return targetPathPrefix + (sourceFullPath.substring(sourcePathPrefix.length()));
    }

    private void checkCollisions(MoveRequest request, String... sourceObjectsFullPaths) {
        List<String> errors = new ArrayList<>();
        for (String sourceObjectFullPath : sourceObjectsFullPaths) {
            String fullObjectTargetPath = getFullTargetPath(sourceObjectFullPath, request);
            if (isDirectory(fullObjectTargetPath)) {
                continue;
            }
            if (fileExists(fullObjectTargetPath)) {
                errors.add(String.format(OBJECT_EXISTS_TEMPLATE, resourcePathUtil.getRelativePath(fullObjectTargetPath)));
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
        checkCollisions(request, objectSourceFullPath);
        ResourcePath newObjectPath = copyObject(objectSourceFullPath, request);
        storageService.deleteFile(sourcePath);
        return newObjectPath;
    }

    private void moveDirectory(MoveRequest request, ResourcePath sourcePath) {
        List<Item> objects = new ArrayList<>();
        PopulationFilter populationFilter = PopulationFilter.builder()
                .includeDirectories(true)
                .includeMarkers(false)
                .includeFiles(true)
                .build();
        storageService.populateWithDirectoryObjectsAsync(sourcePath, objects, populationFilter);
        Set<String> sourceObjectsFullPaths = objects.stream()
                .map(Item::objectName)
                .collect(Collectors.toSet());
        sourceObjectsFullPaths.add(sourcePath.absolute());
        checkCollisions(request, sourceObjectsFullPaths.toArray(new String[0]));
        sourceObjectsFullPaths.forEach(objectFullPath -> copyObject(objectFullPath, request));
        storageService.deleteDirectory(sourcePath);
    }

    private ResourcePath copyObject(String sourceObjectFullPath, MoveRequest request) {
        String targetFullPath = getFullTargetPath(sourceObjectFullPath, request);
        ResourcePath toPath = resourcePathUtil.getResourcePathFromAbsolute(targetFullPath);
        if (isDirectory(targetFullPath)) {
            if (!directoryExists(toPath.relative())) {
                createEmptyDirectory(toPath.relative());
            }
            //storageService.deleteDirectory(resourcePathUtil.getResourcePathFromAbsolute(sourceObjectFullPath));
            return toPath;
        }
        ResourcePath fromPath = resourcePathUtil.getResourcePathFromAbsolute(sourceObjectFullPath);
        storageService.copyObject(fromPath, toPath);
        //storageService.deleteFile(fromPath);

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
        } else {
            int lastSlashIndex = resourceName.lastIndexOf('/');
            if (resourceIsInRootDirectory(lastSlashIndex)) {
                return Strings.CI.contains(resourceName, query);
            }
            String fileName = resourceName.substring(lastSlashIndex + 1);
            return Strings.CI.contains(fileName, query);
        }
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

    /*private PutObjectArgs buildPutObjectArgs(UploadingFile uploadingFile, ResourcePath resourcePath) {
        if (uploadingFile.fileSize().isEmpty()) {
            return PutObjectArgs.builder()
                    .bucket(usersBucket)
                    .object(resourcePath.absolute())
                    .contentType(uploadingFile.contentType())
                    .stream(uploadingFile.inputStream(), FILE_SIZE_NOT_AVAILABLE, 10 * MB)
                    .build();
        }
        return PutObjectArgs.builder()
                .bucket(usersBucket)
                .object(resourcePath.absolute())
                .contentType(uploadingFile.contentType())
                .stream(uploadingFile.inputStream(), uploadingFile.fileSize().get(), FILE_SIZE_AVAILABLE)
                .build();
    }*/

    private List<String> uploadArchive(UploadRequest request) {
        List<String> uploadedResourcesRelativePaths = new ArrayList<>();

        MultipartFile archive = request.file();
        try (ZipInputStream zis = new ZipInputStream(archive.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    uploadedResourcesRelativePaths.add(createEmptyDirectory(resourcePathUtil.getResourcePathFromRelative(request.targetDirectory() + entry.getName())));
                    zis.closeEntry();
                    continue;
                }
                uploadedResourcesRelativePaths.add(uploadFileFromArchive(zis, entry, request));
                zis.closeEntry();
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return uploadedResourcesRelativePaths;
    }

    private String uploadFileFromArchive(ZipInputStream archiveInputStream, ZipEntry entry, UploadRequest request) throws Exception {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(archiveInputStream);
        String contentType = tika.detect(bufferedInputStream);
        UploadingFile uploadingFile = UploadingFile.withUnKnownFileSize(
                request.targetDirectory() + entry.getName(),
                contentType,
                bufferedInputStream
        );
        return uploadFile(uploadingFile);
    }

    private String createEmptyDirectory(ResourcePath path) {
        storageService.createEmptyDirectory(path);
        return path.relative();
    }

    private boolean isArchive(InputStream fileInputStream) {
        try {
            return tika.detect(fileInputStream).equals("application/zip");
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

}

