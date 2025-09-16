package it.luncent.cloud_storage.minio.mapper;

import io.minio.StatObjectResponse;
import it.luncent.cloud_storage.minio.constants.ResourceType;
import it.luncent.cloud_storage.minio.model.common.ResourcePath;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MinioMapper {

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
