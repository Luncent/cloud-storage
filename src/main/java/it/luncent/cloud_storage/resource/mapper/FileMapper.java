package it.luncent.cloud_storage.resource.mapper;

import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface FileMapper {

    default ResourceMetadataResponse mapToFileResponse(String path, long size) {
        String fileName = PathUtils.getFileName(path);
        int lastSlashIndex = path.lastIndexOf('/');
        String filePath = path.substring(0, lastSlashIndex + 1);
        return new ResourceMetadataResponse(filePath, fileName, size, ResourceType.FILE);
    }
}
