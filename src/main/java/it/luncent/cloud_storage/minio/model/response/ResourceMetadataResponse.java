package it.luncent.cloud_storage.minio.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.luncent.cloud_storage.minio.constants.ResourceType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceMetadataResponse(String path,
                                       String name,
                                       Long size,
                                       ResourceType type) {
}
