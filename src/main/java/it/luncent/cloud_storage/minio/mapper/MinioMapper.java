package it.luncent.cloud_storage.minio.mapper;

import it.luncent.cloud_storage.minio.constants.ResourceType;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MinioMapper {

    default ResourceMetadataResponse mapToFolderResponse(String path){
        int length = path.length();
        int prelastSlashIndex = path.lastIndexOf('/', length-2);
        //+1 to cut first / in /dir/
        String folderName = path.substring(prelastSlashIndex+1);
        return new ResourceMetadataResponse(path, folderName, null, ResourceType.DIRECTORY);
    }


}
