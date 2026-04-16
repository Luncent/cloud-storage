package it.luncent.cloud_storage.resource.directory.mapper;

import it.luncent.cloud_storage.resource.constants.ResourceType;
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

    private boolean isRootDirectory(String path) {
        return path.equals("/");
    }
}
