package it.luncent.cloud_storage.resource.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.luncent.cloud_storage.resource.constants.ResourceType;
import it.luncent.cloud_storage.resource.utils.PathUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceMetadataResponse(String path,
                                       String name,
                                       Long size,
                                       ResourceType type) {

    public ResourceMetadataResponse(String path, String name, Long size, ResourceType type) {
        this.path = PathUtils.getRelativePath(path);
        this.name = name;
        this.size = size;
        this.type = type;
    }

}
