package it.luncent.cloud_storage.resource.mapper;

import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.resourceIsInRootDirectory;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    default ResourceMetadataResponse mapToFolderResponse(String pathWithResourceName) {
        if(isRootDirectory(pathWithResourceName)){
            return new ResourceMetadataResponse("", "/", null, ResourceType.DIRECTORY);
        }
        int length = pathWithResourceName.length();
        int penultimateSlashIndex = pathWithResourceName.lastIndexOf('/', length - 2);
        if(resourceIsInRootDirectory(penultimateSlashIndex)) {
            return new ResourceMetadataResponse("/", pathWithResourceName, null, ResourceType.DIRECTORY);
        }
        String folderName = pathWithResourceName.substring(penultimateSlashIndex + 1);
        String folderPath = pathWithResourceName.substring(0, penultimateSlashIndex+1);
        return new ResourceMetadataResponse(folderPath, folderName, null, ResourceType.DIRECTORY);
    }

    default ResourceMetadataResponse mapToFileResponse(ResourcePath resourcePath, StatObjectResponse objectMetadata) {
        String relativePath = resourcePath.relative();
        int lastSlashIndex = relativePath.lastIndexOf('/');
        if(resourceIsInRootDirectory(lastSlashIndex)) {
            return new ResourceMetadataResponse("/", relativePath, objectMetadata.size(), ResourceType.FILE);
        }
        String fileName = relativePath.substring(lastSlashIndex + 1);
        String filePath = relativePath.substring(0, lastSlashIndex+1);
        return new ResourceMetadataResponse(filePath, fileName, objectMetadata.size(), ResourceType.FILE);
    }

    private boolean isRootDirectory(String path) {
        return path.equals("/");
    }
}
