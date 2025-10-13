package it.luncent.cloud_storage.storage.mapper;

import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.storage.constants.ResourceType;
import it.luncent.cloud_storage.storage.model.common.ResourcePath;
import it.luncent.cloud_storage.storage.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    default ResourceMetadataResponse mapToFolderResponse(ResourcePath path){
        String fullPath = path.real();
        int length = fullPath.length();
        int prelastSlashIndex = fullPath.lastIndexOf('/', length-2);
        //+1 to cut first / in /dir/
        String folderName = fullPath.substring(prelastSlashIndex+1);
        return new ResourceMetadataResponse(path.relative(), folderName, null, ResourceType.DIRECTORY);
    }


    default ResourceMetadataResponse mapToFileResponse(ResourcePath resourcePath, StatObjectResponse objectMetadata){
        String relativePath =resourcePath.relative();
        int lastSlashIndex = relativePath.lastIndexOf('/');
        String fileName = relativePath.substring(lastSlashIndex+1);
        return new ResourceMetadataResponse(
                resourcePath.relative(),
                fileName,
                objectMetadata.size(),
                ResourceType.FILE
        );
    }
}
