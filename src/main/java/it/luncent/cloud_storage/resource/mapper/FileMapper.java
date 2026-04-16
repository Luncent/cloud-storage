package it.luncent.cloud_storage.resource.mapper;

import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.resourceIsInRootDirectory;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface FileMapper {

    //TODO убрать, не нужен этот метод, размер и имя можно и без него получить
    default ResourceMetadataResponse mapToFileResponse(String relative, long size) {
        int lastSlashIndex = relative.lastIndexOf('/');
        if (resourceIsInRootDirectory(lastSlashIndex)) {
            return new ResourceMetadataResponse("/", relative, size, ResourceType.FILE);
        }
        String fileName = relative.substring(lastSlashIndex + 1);
        String filePath = relative.substring(0, lastSlashIndex + 1);
        return new ResourceMetadataResponse(filePath, fileName, size, ResourceType.FILE);
    }
}
