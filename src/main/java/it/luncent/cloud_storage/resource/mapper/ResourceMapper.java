package it.luncent.cloud_storage.resource.mapper;

import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    default ResourceMetadataResponse mapToFolderResponse(ResourcePath path) {
        String pathWithResourceName = path.relative();
        int length = pathWithResourceName.length();
        int penultimateSlashIndex = pathWithResourceName.lastIndexOf('/', length - 2);
        String folderName = pathWithResourceName.substring(penultimateSlashIndex + 1);
        String folderPath = pathWithResourceName.substring(0, penultimateSlashIndex);
        return new ResourceMetadataResponse(folderPath, folderName, null, ResourceType.DIRECTORY);
    }

    default ResourceMetadataResponse mapToFileResponse(ResourcePath resourcePath, StatObjectResponse objectMetadata) {
        String relativePath = resourcePath.relative();
        int lastSlashIndex = relativePath.lastIndexOf('/');
        String fileName = relativePath.substring(lastSlashIndex + 1);
        String filePath = relativePath.substring(0, lastSlashIndex);
        return new ResourceMetadataResponse(
                filePath,
                fileName,
                objectMetadata.size(),
                ResourceType.FILE
        );
    }
}
