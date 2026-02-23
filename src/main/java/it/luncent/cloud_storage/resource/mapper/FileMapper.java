package it.luncent.cloud_storage.resource.mapper;

import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.resourceIsInRootDirectory;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface FileMapper {

    //TODO убрать, не нужен этот метод, размер и имя можно и без него получить
    default ResourceMetadataResponse mapToFileResponse(ResourcePath resourcePath, StatObjectResponse objectMetadata) {
        String relativePath = resourcePath.relative();
        int lastSlashIndex = relativePath.lastIndexOf('/');
        if (resourceIsInRootDirectory(lastSlashIndex)) {
            return new ResourceMetadataResponse("/", relativePath, objectMetadata.size(), ResourceType.FILE);
        }
        String fileName = relativePath.substring(lastSlashIndex + 1);
        String filePath = relativePath.substring(0, lastSlashIndex + 1);
        return new ResourceMetadataResponse(filePath, fileName, objectMetadata.size(), ResourceType.FILE);
    }
}
