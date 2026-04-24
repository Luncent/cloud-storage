package it.luncent.cloud_storage.resource.directory.mapper;

import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import org.mapstruct.Mapper;

import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.DIRECTORY_SUFFIX;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface DirectoryMapper {

    default ResourceMetadataResponse mapToResponse(String path) {
        String directoryName = PathUtils.getDirectoryName(path);
        if(directoryName.equals(DIRECTORY_SUFFIX)){
            return new ResourceMetadataResponse(path, directoryName, null, ResourceType.DIRECTORY);
        }
        int length = path.length();
        int penultimateSlashIndex = path.lastIndexOf('/', length - 2);
        String directoryPath = path.substring(0, penultimateSlashIndex + 1);
        return new ResourceMetadataResponse(directoryPath, directoryName, null, ResourceType.DIRECTORY);
    }
}
