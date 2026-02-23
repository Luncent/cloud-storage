package it.luncent.cloud_storage.resource.directory.mapper;

import io.minio.messages.Item;
import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.resourceIsInRootDirectory;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface DirectoryMapper {

    default ResourceMetadataResponse mapToResponse(String pathWithResourceName) {
        if (isRootDirectory(pathWithResourceName)) {
            return new ResourceMetadataResponse("", "/", null, ResourceType.DIRECTORY);
        }
        int length = pathWithResourceName.length();
        int penultimateSlashIndex = pathWithResourceName.lastIndexOf('/', length - 2);
        if (resourceIsInRootDirectory(penultimateSlashIndex)) {
            return new ResourceMetadataResponse("/", pathWithResourceName, null, ResourceType.DIRECTORY);
        }
        String folderName = pathWithResourceName.substring(penultimateSlashIndex + 1);
        String folderPath = pathWithResourceName.substring(0, penultimateSlashIndex + 1);
        return new ResourceMetadataResponse(folderPath, folderName, null, ResourceType.DIRECTORY);
    }

    default ResourceMetadataResponse mapToFileResponse(ResourcePath resourcePath, Item objectMetadata) {
        String relativePath = resourcePath.relative();
        int lastSlashIndex = relativePath.lastIndexOf('/');
        if (resourceIsInRootDirectory(lastSlashIndex)) {
            return new ResourceMetadataResponse("/", relativePath, objectMetadata.size(), ResourceType.FILE);
        }
        String fileName = relativePath.substring(lastSlashIndex + 1);
        String filePath = relativePath.substring(0, lastSlashIndex + 1);
        return new ResourceMetadataResponse(filePath, fileName, objectMetadata.size(), ResourceType.FILE);
    }

    private boolean isRootDirectory(String path) {
        return path.equals("/");
    }
}
